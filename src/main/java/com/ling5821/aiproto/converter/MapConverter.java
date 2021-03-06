package com.ling5821.aiproto.converter;

import com.ling5821.aiproto.util.ByteBufUtils;
import io.netty.buffer.ByteBuf;

import java.util.HashMap;
import java.util.Map;

/**
 * @author lsj
 * @date 2021/1/25 16:07
 */
public abstract class MapConverter<K, V> implements Converter<Map<K, V>> {
    protected abstract K readKey(ByteBuf input);

    protected abstract void writeKey(ByteBuf output, K key);

    protected abstract int valueSize();

    protected abstract V convert(K key, ByteBuf input);

    protected abstract void convert(K key, ByteBuf output, V value);

    @Override
    public Map<K, V> convert(ByteBuf input) {
        if (!input.isReadable()) {
            return null;
        }
        Map<K, V> map = new HashMap<>();
        do {
            K id = readKey(input);
            int len = ByteBufUtils.readInt(input, valueSize());
            Object value = convert(id, input.readSlice(len));
            map.put(id, (V)value);
        } while (input.isReadable());
        return map;
    }

    @Override
    public void convert(ByteBuf output, Map<K, V> map) {
        if (map == null || map.isEmpty()) {
            return;
        }
        for (Map.Entry<K, V> entry : map.entrySet()) {
            K key = entry.getKey();
            V value = entry.getValue();
            writeKey(output, key);
            int valueSize = valueSize();
            int begin = output.writerIndex();
            output.writeBytes(ByteBufUtils.BLOCKS[valueSize]);
            convert(key, output, value);
            int len = output.writerIndex() - begin - valueSize;
            ByteBufUtils.setInt(output, valueSize, begin, len);
        }
    }
}
