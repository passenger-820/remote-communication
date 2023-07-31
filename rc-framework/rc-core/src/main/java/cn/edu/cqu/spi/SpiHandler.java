package cn.edu.cqu.spi;

import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 一个简易版本的spi
 */
@Slf4j
public class SpiHandler {
    // 缓存【构造出的对象如果需要反复使用，应当考虑使用缓存】
    // 每一个接口所对应的实现的实例
    private static final Map<Class<?>, List<Object>> SPI_IMPLEMENT = new ConcurrentHashMap<>(32);

    // 先定义一个缓存，保存spi原始文件的相关内容，减少io开销
    private static final Map<String, List<String >> SPI_CONTENT = new ConcurrentHashMap<>(8);
    // 定义base path
    private static final String BASE_PATH = "META-INF/rc-services";
    // 通过静态方法加载这些内容，加载完成时，及时保存spi休息，避免频繁io
    static {
        // TODO: 2023/7/31 怎么加载当前工程和jar包中的classpath中的资源
        // 该方法返回一个ClassLoader对象，可以用于加载类或资源文件。例如，可以使用它来加载位于类路径之外的类或配置文件。
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        URL fileUrl = classLoader.getResource(BASE_PATH);
        // 能拿到资源才干活
        if (fileUrl != null){
            File file = new File(fileUrl.getPath());
            File[] children = file.listFiles();
            // 有子文件才干活
            if (children != null || children.length > 0){
                for (File child : children) {
                    // 文件名即为接口全限定名
                    String key = child.getName();
                    List<String > value = getImplNames(child);
                    SPI_CONTENT.put(key,value);
                }
            }
        }
    }


    /**
     * 获取一个和当前服务相关的实例
     * Map<Class<?>, List<Object>> SPI_IMPLEMENT
     * @param clazz 一个服务接口的clazz实例
     * @param <T> 泛型
     * @return SPI_IMPLEMENT的一个list中的第1个具体实现
     */
    public static <T> T get(Class<?> clazz) {
        // 优先走缓存
        List<Object> impls = SPI_IMPLEMENT.get(clazz);
        if (impls != null && impls.size() > 0){
            // todo 优先返回原则，尝试获取第1个
            return (T) impls.get(0);
        }

        // 否则需要构建缓存
        buildCache(clazz);

        // 如果构建完缓存，还是没有
        List<Object> result = SPI_IMPLEMENT.get(clazz);
        if (result == null || result.size() == 0){
            return null;
        }

        // todo 优先返回原则，尝试获取第1个
        return (T) SPI_IMPLEMENT.get(clazz).get(0);
    }

    /**
     * 获取所有和当前服务相关的实例
     * Map<Class<?>, List<Object>> SPI_IMPLEMENT
     * @param clazz 一个服务接口的clazz实例
     * @param <T> 泛型
     * @return SPI_IMPLEMENT的一个list
     */
    public static <T> List<T> getList(Class<?> clazz) {
        // 优先走缓存
        List<T> impls = (List<T>) SPI_IMPLEMENT.get(clazz);
        if (impls != null && impls.size() > 0){
            // todo 优先返回原则
            return (List<T>) impls;
        }

        // 否则需要构建缓存
        buildCache(clazz);

        // todo 优先返回原则
        return (List<T>) SPI_IMPLEMENT.get(clazz);
    }

    /**
     * 构建clazz相关的缓存
     * Map<Class<?>, List<Object>> SPI_IMPLEMENT
     * @param clazz clazz
     */
    private static void buildCache(Class<?> clazz) {

        // 1、通过clazz获取与之匹配的全限定名  从缓存中获取实现类的全限定名
        String name = clazz.getName();
        List<String> implNames = SPI_CONTENT.get(name);
        if (implNames == null || implNames.size() == 0){
            return;
        }

        // 2、实例化所有实现，同时缓存
        List<Object> impls = new ArrayList<>();
        for (String implName : implNames) {
            try {
                Class<?> aClass = Class.forName(implName);
                Object impl = aClass.getConstructor().newInstance();
                // 缓存起来
                impls.add(impl);
            } catch (ClassNotFoundException | InvocationTargetException | InstantiationException | IllegalAccessException | NoSuchMethodException e) {
                // 异常也无妨，有其他方式兜底
                log.error("实例化【{}】接口的实现时发生异常。",implName,e);
            }
        }
        SPI_IMPLEMENT.put(clazz,impls);
    }

    /**
     * 获取文件内所有的实现名称
     * @param child 文件对象
     * @return  实现类的全限定名称集合
     */
    private static List<String> getImplNames(File child) {
        try (
                // 读文件流
                FileReader fileReader = new FileReader(child);
                // 装饰器包装下
                BufferedReader bufferedReader = new BufferedReader(fileReader);
        ) {
            List<String> implNames = new ArrayList<>();
            while (true){
                String line = bufferedReader.readLine();
                if (line == null || "".equals(line)) break;
                implNames.add(line);
            }
            return implNames;
        }  catch (IOException e) {
            log.error("读取spi文件时发生异常。",e);
        }
        // 读取不到就读取不到，反正前有默认，后有xml，再不济还有RcBootstrap
        return null;
    }
}
