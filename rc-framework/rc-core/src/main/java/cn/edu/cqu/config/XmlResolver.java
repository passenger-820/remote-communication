package cn.edu.cqu.config;

import cn.edu.cqu.IdGenerator;
import cn.edu.cqu.ProtocolConfig;
import cn.edu.cqu.compress.Compressor;
import cn.edu.cqu.compress.CompressorFactory;
import cn.edu.cqu.discovery.RegistryConfig;
import cn.edu.cqu.loadbalance.LoadBalancer;
import cn.edu.cqu.serialize.Serializer;
import cn.edu.cqu.serialize.SerializerFactory;
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
import java.util.Objects;

@Slf4j
public class XmlResolver {
    /**
     * 从配置文件读取配置信息 不使用dom4j，使用jdk原生的
     * @param configuration 配置上下文
     */
    public void loadConfigFromXml(Configuration configuration) {
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


            // 处理使用的序列化和压缩方式
            configuration.setSerializeType(resolveSerializeType(doc, xPath));
            configuration.setCompressType(resolveCompressType(doc, xPath));

            /*
             todo 这里为了简单，就只解析一个，没有配置多个，也没与去解析成list
             */
            // 配置新的序列化和压缩方式的包装类，并将其纳入工厂中
            ObjectWrapper<Compressor> compressorWrapper = resolveCompressor(doc, xPath);
            CompressorFactory.addCompressor(compressorWrapper);
            ObjectWrapper<Serializer> serializerWrapper = resolveSerializer(doc, xPath);
            SerializerFactory.addSerializer(serializerWrapper);

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
     * 解析序列化器包装类
     * @param doc 文档
     * @param xPath xpath解析器
     * @return ObjectWrapper<Serializer>
     */
    private ObjectWrapper<Serializer> resolveSerializer(Document doc, XPath xPath) {
        String expression = "/configuration/serializer";
        Serializer serializer = parseObject(doc,xPath,expression,null);
        Byte code = Byte.valueOf(Objects.requireNonNull(parseString(doc, xPath, expression, "code")));
        String type = parseString(doc, xPath, expression, "type");
        ObjectWrapper<Serializer> serializerWrapper = new ObjectWrapper<>(code,type,serializer);
        return serializerWrapper;

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
     * 解析压缩器包装类
     * @param doc 文档
     * @param xPath xpath解析器
     * @return ObjectWrapper<Compressor>
     */
    private ObjectWrapper<Compressor> resolveCompressor(Document doc, XPath xPath) {
        String expression = "/configuration/compressor";
        Compressor compressor = parseObject(doc, xPath, expression, null);
        Byte code = Byte.valueOf(Objects.requireNonNull(parseString(doc, xPath, expression, "code")));
        String type = parseString(doc, xPath, expression, "type");
        ObjectWrapper<Compressor> compressorWrapper = new ObjectWrapper<>(code,type,compressor);
        return compressorWrapper;
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

}
