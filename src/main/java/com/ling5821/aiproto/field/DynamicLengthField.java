package com.ling5821.aiproto.field;

import com.ling5821.aiproto.Schema;
import com.ling5821.aiproto.annotation.Field;
import com.ling5821.aiproto.util.ByteBufUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;

import java.beans.PropertyDescriptor;

/**
 * @author lsj
 * @date 2021/1/25 19:49
 */
public class DynamicLengthField<T> extends BasicField<T> {
    protected final Schema<T> schema;
    protected final int lengthSize;

    public DynamicLengthField(Field field, PropertyDescriptor property, Schema<T> schema) {
        super(field, property);
        this.schema = schema;
        this.lengthSize = field.lengthSize();
    }

    @Override
    public boolean readFrom(ByteBuf input, Object message) throws Exception {
        int length = ByteBufUtils.readInt(input, lengthSize);
        if (!input.isReadable(length)) {
            return false;
        }
        Object value = schema.readFrom(input, length);
        writeMethod.invoke(message, value);
        return true;
    }

    @Override
    public void writeTo(ByteBuf output, Object message) throws Exception {
        Object value = readMethod.invoke(message);
        if (value != null) {
            int begin = output.writerIndex();
            output.writeBytes(ByteBufUtils.BLOCKS[lengthSize]);
            schema.writeTo(output, (T)value);
            int length = output.writerIndex() - begin - lengthSize;
            ByteBufUtils.setInt(output, lengthSize, begin, length);
        }
    }

    @Override
    public int compareTo(BasicField<T> that) {
        int r = Integer.compare(this.index, that.index);
        if (r == 0) {
            r = (that instanceof DynamicLengthField) ? 1 : -1;
        }
        return r;
    }

    public static class Logger<T> extends DynamicLengthField<T> {

        public Logger(Field field, PropertyDescriptor property, Schema<T> schema) {
            super(field, property, schema);
        }

        @Override
        public boolean readFrom(ByteBuf input, Object message) throws Exception {
            int before = input.readerIndex();

            int length = ByteBufUtils.readInt(input, lengthSize);
            if (!input.isReadable(length)) {
                return false;
            }
            Object value = schema.readFrom(input, length);
            writeMethod.invoke(message, value);

            int after = input.readerIndex();
            String hex = ByteBufUtil.hexDump(input.slice(before, after - before));
            println(this.index, this.desc, hex, value);
            return true;
        }

        @Override
        public void writeTo(ByteBuf output, Object message) throws Exception {
            int before = output.writerIndex();

            Object value = readMethod.invoke(message);
            if (value != null) {
                int begin = output.writerIndex();
                output.writeBytes(ByteBufUtils.BLOCKS[lengthSize]);
                schema.writeTo(output, (T)value);
                int length = output.writerIndex() - begin - lengthSize;
                ByteBufUtils.setInt(output, lengthSize, begin, length);
            }

            int after = output.writerIndex();
            String hex = ByteBufUtil.hexDump(output.slice(before, after - before));
            println(this.index, this.desc, hex, value);
        }
    }
}
