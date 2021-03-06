package com.ling5821.aiproto.util;

import com.ling5821.aiproto.DefaultLoadStrategy;

import java.io.File;
import java.lang.annotation.Annotation;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * @author lsj
 * @date 2021/3/24 16:22
 */
public class ClassUtils {

    public static List<Class<?>> getClassList(String packageName, Class<? extends Annotation> annotationClass) {
        List<Class<?>> classList = getClassList(packageName);
        Iterator<Class<?>> iterator = classList.iterator();
        while (iterator.hasNext()) {
            Class<?> next = iterator.next();
            if (!next.isAnnotationPresent(annotationClass)) {
                iterator.remove();
            }
        }
        return classList;
    }

    public static List<Class<?>> getClassList(String packageName) {
        List<Class<?>> classList = new LinkedList<>();
        String path = packageName.replace(".", "/");
        ClassLoader[] classLoaders = getClassLoaders();
        for (ClassLoader classLoader : classLoaders) {
            try {
                Enumeration<URL> urls = classLoader.getResources(path);
                while (urls.hasMoreElements()) {
                    URL url = urls.nextElement();

                    if (url != null) {
                        String protocol = url.getProtocol();

                        if ("file".equals(protocol)) {
                            addClass(classList, url.toURI().getPath(), packageName, classLoader);

                        } else if ("jar".equals(protocol)) {
                            JarURLConnection jarURLConnection = (JarURLConnection)url.openConnection();
                            JarFile jarFile = jarURLConnection.getJarFile();

                            Enumeration<JarEntry> jarEntries = jarFile.entries();
                            while (jarEntries.hasMoreElements()) {

                                JarEntry jarEntry = jarEntries.nextElement();
                                String entryName = jarEntry.getName();

                                if (entryName.startsWith(path) && entryName.endsWith(".class")) {
                                    String className =
                                        entryName.substring(0, entryName.lastIndexOf(".")).replaceAll("/", ".");
                                    addClass(classList, className, classLoader);
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException("Initial class error!");
            }
        }
        return classList;
    }

    private static void addClass(List<Class<?>> classList, String packagePath, String packageName,
        ClassLoader classLoader) {
        try {
            File[] files =
                new File(packagePath).listFiles(file -> (file.isDirectory() || file.getName().endsWith(".class")));
            if (files != null) {
                for (File file : files) {
                    String fileName = file.getName();
                    if (file.isFile()) {
                        String className = fileName.substring(0, fileName.lastIndexOf("."));
                        if (packageName != null) {
                            className = packageName + "." + className;
                        }
                        addClass(classList, className, classLoader);
                    } else {
                        String subPackagePath = fileName;
                        if (packageName != null) {
                            subPackagePath = packagePath + "/" + subPackagePath;
                        }
                        String subPackageName = fileName;
                        if (packageName != null) {
                            subPackageName = packageName + "." + subPackageName;
                        }
                        addClass(classList, subPackagePath, subPackageName, classLoader);
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void addClass(List<Class<?>> classList, String className, ClassLoader classLoader) {
        classList.add(loadClass(className, false, classLoader));
    }

    public static Class<?> loadClass(String className, boolean isInitialized, ClassLoader classLoader) {
        try {
            return Class.forName(className, isInitialized, classLoader);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public static ClassLoader contextClassLoader() {
        return Thread.currentThread().getContextClassLoader();
    }

    public static ClassLoader staticClassLoader() {
        return ClassUtils.class.getClassLoader();
    }

    public static ClassLoader[] getClassLoaders() {
        ClassLoader contextClassLoader = contextClassLoader(), staticClassLoader = staticClassLoader();
        return contextClassLoader != null ? staticClassLoader != null && contextClassLoader != staticClassLoader ?
            new ClassLoader[] {contextClassLoader, staticClassLoader} : new ClassLoader[] {contextClassLoader} :
            new ClassLoader[] {};
    }
}