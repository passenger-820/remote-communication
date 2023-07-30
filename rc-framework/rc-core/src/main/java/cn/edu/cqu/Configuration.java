package cn.edu.cqu;

import cn.edu.cqu.compress.Compressor;
import cn.edu.cqu.compress.impl.GzipCompressor;
import cn.edu.cqu.discovery.RegistryConfig;
import cn.edu.cqu.loadbalance.LoadBalancer;
import cn.edu.cqu.loadbalance.impl.RoundRobinLoadBalancer;
import cn.edu.cqu.serialize.Serializer;
import cn.edu.cqu.serialize.impl.HessianSerializer;
import cn.edu.cqu.serialize.impl.JdkSerializer;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.*;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;

/**
 * 全局的配置类，代码配置-->xml配置-->默认项
 */
@Data
@Slf4j
public class Configuration {
    // 端口号
    private int port = 8088;

    // 应用名
    private String appName = "default";

    // 注册中心配置
    private RegistryConfig registryConfig = new RegistryConfig("zookeeper://127.0.0.1:2181");

    // 序列化方式
    private String serializeType = "jdk";
    // 序列化器
    private Serializer serializer = new JdkSerializer();
    // 序列化配置
    private ProtocolConfig protocolConfig = new ProtocolConfig("jdk");

    // 压缩方式
    private String compressType = "gzip";
    // 压缩器
    private Compressor compressor = new GzipCompressor();

    // 负载均衡器
    private LoadBalancer loadBalancer = new RoundRobinLoadBalancer();
    // TODO: 2023/7/30 暂时不写loadBalancerType了

    // Id生成器
    private IdGenerator idGenerator = new IdGenerator(2,4);



    // 读xml，就在构造器里实现，能从xml拿到就拿到，拿不到就走默认
    public Configuration() {
        // 读取xml里的配置配置信息
        loadXmlConfiguration(this);
        // 构造时读取xml配置，xml读不到的，就是用上面默认的，如果后期通过代码修改了，那就依照修改后的

    }

    /**
     * 从配置文件读取配置信息 不使用dom4j，使用jdk原生的
     * @param configuration
     */
    private void loadXmlConfiguration(Configuration configuration) {
        try {
            // 创建解析器工厂和解析器
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            // 禁用DTD校验：可以通过调用setValidating(false)方法来禁用DTD校验。
            factory.setValidating(false);
            // 禁用外部实体解析：可以通过调用setFeature(String name, boolean value)方法并将“http://apache.org/xml/features/nonvalidating/load-external-dtd”设置为“false”来禁用外部实体解析。
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            DocumentBuilder builder = factory.newDocumentBuilder();

            // 读取XML文件
            InputStream inputStream = ClassLoader.getSystemClassLoader().getResourceAsStream("rc.xml");
            // 拿到document
            Document doc = builder.parse(inputStream);

            // 获取XPath的解析器
            XPathFactory xPathFactory = XPathFactory.newInstance();
            XPath xPath = xPathFactory.newXPath();

            // 解析所有标签
            configuration.setPort(resolvePort(doc,xPath));
            configuration.setAppName(resolveAppName(doc,xPath));
            configuration.setRegistryConfig(resolveRegistryConfig(doc, xPath));


            // 序列化
            configuration.setSerializeType(resolveSerializeType(doc, xPath));
            configuration.setSerializer(resolveSerializer(doc, xPath));
            configuration.setProtocolConfig(new ProtocolConfig(this.serializeType));
            // 压缩
            configuration.setCompressType(resolveCompressType(doc, xPath));
            configuration.setCompressor(resolveCompressor(doc, xPath));

            // 负载均衡
            configuration.setLoadBalancer(resolveLoadBalancer(doc, xPath));
            // TODO: 2023/7/30 暂时没有管loadBalancerType

            // id生成器
            configuration.setIdGenerator(resolveIdGenerator(doc, xPath));

            // 如果有新增的标签，这里自行修改

        } catch (ParserConfigurationException | IOException | SAXException e) {
            // 读不到，走默认的就完了
            log.error("解析配置文件时发生异常，将使用默认配置",e);
        }
    }



    /**
     * 解析端口号
     * @param doc 文档
     * @param xPath xpath解析器
     * @return 端口号
     */
    private int resolvePort(Document doc, XPath xPath) {
        String expression = "/configuration/port";
        String port = parseString(doc, xPath, expression);
        assert port != null;
        return Integer.parseInt(port);
    }

    /**
     * 解析appName
     * @param doc 文档
     * @param xPath xpath解析器
     * @return appName
     */
    private String resolveAppName(Document doc, XPath xPath) {
        String expression = "/configuration/appName";
        String appName = parseString(doc, xPath, expression);
        return appName;
    }

    /**
     * 解析注册中心
     * @param doc   文档
     * @param xPath xpath解析器
     * @return RegistryConfig
     */
    private RegistryConfig resolveRegistryConfig(Document doc, XPath xPath) {
        String expression = "/configuration/registry";
        String url = parseString(doc, xPath, expression, "url");
        return new RegistryConfig(url);
    }

    /**
     * 解析序列化的方式
     * @param doc   文档
     * @param xpath xpath解析器
     * @return 序列化的方式
     */
    private String resolveSerializeType(Document doc, XPath xpath) {
        String expression = "/configuration/serializeType";
        return parseString(doc, xpath, expression, "type");
    }

    /**
     * 解析序列化器
     * @param doc 文档
     * @param xPath xpath解析器
     * @return Serializer
     */
    private Serializer resolveSerializer(Document doc, XPath xPath) {
        String expression = "/configuration/serializer";
        return parseObject(doc,xPath,expression,null);

    }

    /**
     * 解析压缩的算法名称
     * @param doc   文档
     * @param xPath xpath解析器
     * @return 压缩算法名称
     */
    private String resolveCompressType(Document doc, XPath xPath) {
        String expression = "/configuration/compressType";
        return parseString(doc, xPath, expression, "type");
    }

    /**
     * 解析压缩器
     * @param doc 文档
     * @param xPath xpath解析器
     * @return Compressor
     */
    private Compressor resolveCompressor(Document doc, XPath xPath) {
        String expression = "/configuration/compressor";
        return parseObject(doc,xPath,expression,null);
    }

    /**
     * 解析负载均衡器
     * @param doc   文档
     * @param xPath xpath解析器
     * @return LoadBalancer
     */
    private LoadBalancer resolveLoadBalancer(Document doc, XPath xPath) {
        String expression = "/configuration/loadBalancer";
        return parseObject(doc,xPath,expression,null);
    }

    /**
     * 解析id生成器
     * @param doc 文档
     * @param xPath xpath解析器
     * @return IdGenerator实例
     */
    private IdGenerator resolveIdGenerator(Document doc, XPath xPath) {
        String expression = "/configuration/idGenerator";
        // TODO: 2023/7/30 其实这样也是写死了，算是只适用于雪花算法的了
        String aClass = parseString(doc, xPath, expression, "class");
        String dataCenterId = parseString(doc, xPath, expression, "dataCenterId");
        String machineId = parseString(doc, xPath, expression, "MachineId");

        try {
            Class<?> clazz = Class.forName(aClass);
            Object instance = clazz.getConstructor(new Class[]{long.class, long.class})
                    .newInstance(Long.parseLong(dataCenterId), Long.parseLong(machineId));
            return (IdGenerator) instance;
        } catch (ClassNotFoundException | NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }




    /**
     * 获得一个节点文本值   <port>7777</>
     * @param doc        文档对象
     * @param xpath      xpath解析器
     * @param expression xpath表达式
     * @return 节点的值
     */
    private String parseString(Document doc, XPath xpath, String expression) {
        try {
            XPathExpression expr = xpath.compile(expression);
            Node targetNode = (Node) expr.evaluate(doc, XPathConstants.NODE);
            // 节点文本值
            return targetNode.getTextContent();
        } catch (XPathExpressionException e) {
            log.error("解析表达式时发生异常。", e);
        }
        return null;
    }

    /**
     * 获得一个节点属性的值   <port num="7777"></>
     * @param doc           文档对象
     * @param xpath         xpath解析器
     * @param expression    xpath表达式
     * @param AttributeName 节点名称
     * @return 节点的值
     */
    private String parseString(Document doc, XPath xpath, String expression, String AttributeName) {
        try {
            XPathExpression expr = xpath.compile(expression);
            Node targetNode = (Node) expr.evaluate(doc, XPathConstants.NODE);
            // 节点属性的值
            return targetNode.getAttributes().getNamedItem(AttributeName).getNodeValue();
        } catch (XPathExpressionException e) {
            log.error("解析表达式时发生异常。", e);
        }
        return null;
    }


    /**
     * 解析一个节点，返回一个实例
     * @param doc      文档对象
     * @param xPath         xpath解析器
     * @param expression    xpath表达式
     * @param paramTypes    参数列表
     * @param params        参数
     * @param <T>           泛型
     * @return 配置的实例
     */
    private <T> T parseObject(Document doc, XPath xPath, String expression, Class<?>[] paramTypes,Object... params) {
        try {
            XPathExpression expr = xPath.compile(expression);
            // 使用表达式获取document上的节点
            Node targetNode = (Node) expr.evaluate(doc, XPathConstants.NODE);
            // 获取节点中参数为"class"属性的节点
            String className = targetNode.getAttributes().getNamedItem("class").getNodeValue();
            final Class<?> aClass = Class.forName(className);
            Object instance = null;
            if (paramTypes == null){
                instance = aClass.getConstructor().newInstance();
            } else {
                instance = aClass.getConstructor().newInstance(params);
            }
            return (T) instance;
        } catch (ClassNotFoundException | NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException | XPathExpressionException e) {
            log.error("解析表达式时发生异常。",e);
        }

        return null;
    }

    public static void main(String[] args) {
        Configuration configuration = new Configuration();
    }
}
