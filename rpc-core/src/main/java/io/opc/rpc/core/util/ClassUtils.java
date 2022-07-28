package io.opc.rpc.core.util;

import io.opc.rpc.api.exception.ExceptionCode;
import io.opc.rpc.api.exception.OpcRpcRuntimeException;
import java.io.File;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

/**
 * Copy from [CSDN](<a href="https://blog.csdn.net/yuhentian/article/details/110007378">Java获取指定package下所有类</a>)
 *
 * @author caihongwen
 * @version Id: ClassUtils.java, v 0.1 2022年07月28日 16:51 caihongwen Exp $
 */
@UtilityClass
@Slf4j
public class ClassUtils {

    /**
     * 从指定的 package 中获取所有的 Class
     *
     * @param packageName String packageName
     * @return class list
     */
    public static List<Class<?>> getClasses(String packageName) throws OpcRpcRuntimeException {

        // 第一个class类的集合
        List<Class<?>> classes = new ArrayList<>();
        // 获取包的名字 并进行替换
        String packageDirName = packageName.replace('.', '/');
        // 定义一个枚举的集合 并进行循环来处理这个目录下的things
        Enumeration<URL> dirs;
        try {
            dirs = Thread.currentThread().getContextClassLoader().getResources(packageDirName);
            // 循环迭代下去
            while (dirs.hasMoreElements()) {
                URL url = dirs.nextElement();
                // 得到协议的名称
                String protocol = url.getProtocol();
                if ("file".equals(protocol)) {
                    // 获取包的物理路径
                    String filePath = URLDecoder.decode(url.getFile(), "UTF-8");
                    // 以文件的方式扫描整个包下的文件 并添加到集合中
                    classes.addAll(findClassByDirectory(packageName, filePath));
                } else if ("jar".equals(protocol)) {
                    classes.addAll(findClassInJar(packageName, url));
                }
            }
        } catch (IOException e) {
            throw new OpcRpcRuntimeException(ExceptionCode.REGISTER_LOAD_CLASS_ERROR, e);
        }

        return classes;
    }

    /**
     * 以文件的形式来获取包下的所有Class
     *
     * @param packageName packageName
     * @param packagePath packagePath
     */
    public static List<Class<?>> findClassByDirectory(String packageName, String packagePath) {
        // 获取此包的目录 建立一个File
        File dir = new File(packagePath);
        if (!dir.exists() || !dir.isDirectory()) {
            return Collections.emptyList();
        }

        File[] dirs = dir.listFiles();
        List<Class<?>> classes = new ArrayList<>();
        // 循环所有文件
        for (File file : dirs) {
            // 如果是目录 则继续扫描
            if (file.isDirectory()) {
                classes.addAll(findClassByDirectory(packageName + "." + file.getName(), file.getAbsolutePath()));
            } else if (file.getName().endsWith(".class")) {
                // 如果是java类文件，去掉后面的.class 只留下类名
                String className = file.getName().substring(0, file.getName().length() - ".class".length());
                try {
                    classes.add(Class.forName(packageName + '.' + className));
                } catch (ClassNotFoundException e) {
                    // skip when ClassNotFoundException
                    log.warn("FindClassByDirectory error, {} ClassNotFound.", packageName + '.' + className, e);
                }
            }
        }

        return classes;
    }

    public static List<Class<?>> findClassInJar(String packageName, URL url) throws OpcRpcRuntimeException {

        List<Class<?>> classes = new ArrayList<>();
        String packageDirName = packageName.replace('.', '/');
        // 定义一个JarFile
        JarFile jar;
        try {
            // 获取jar
            jar = ((JarURLConnection) url.openConnection()).getJarFile();
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                // 获取jar里的一个实体 可以是目录 和一些jar包里的其他文件 如META-INF等文件
                JarEntry entry = entries.nextElement();
                if (entry.isDirectory()) {
                    continue;
                }

                String name = entry.getName();
                if (name.charAt(0) == '/') {
                    // 获取后面的字符串
                    name = name.substring(1);
                }

                // 如果前半部分和定义的包名相同
                if (name.startsWith(packageDirName) && name.endsWith(".class")) {
                    // 去掉后面的".class"
                    String className = name.substring(0, name.length() - ".class".length()).replace('/', '.');
                    try {
                        // 添加到classes
                        classes.add(Class.forName(className));
                    } catch (ClassNotFoundException e) {
                        // skip when ClassNotFoundException
                        log.warn("FindClassByDirectory error, {} ClassNotFound.", packageName + '.' + className, e);
                    }
                }
            }
        } catch (IOException e) {
            throw new OpcRpcRuntimeException(ExceptionCode.REGISTER_LOAD_CLASS_ERROR, e);
        }

        return classes;
    }

    public static void main(String[] args) {
        getClasses("io.opc.rpc.core.request").forEach(System.out::println);
        System.out.println();
        getClasses("io.opc.rpc.core.response").forEach(System.out::println);
    }

}
