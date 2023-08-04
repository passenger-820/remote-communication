# Remote-Communication

尝试手写rpc框架

### rc-demo

#### rc-api

* 示例api
* @ReTry注解用于配置服务调用的重试状态

#### rc-provider-demo 服务提供方

* 实现了示例api
* 使用RcBootstrap的一些方法配置与启动

#### rc-consumer-demo 服务调用方

* 使用RcBootstrap的一些方法配置与启动

### rc-framework

#### rc-common

##### annotation

> RcApi

* 在类上使用，在运行时生效
* 使用此注解的接口的实现类，将会“分组”

> ReTry

* 在方法上使用，在运行时生效
* 使用此注解的接口的方法，将会视情况“重试”

##### exceptions

* 继承自RuntimeException的各种类

##### utils

> zookeeper

* zookeeperNode类

  * path 和 data

  > zookeeper工具类

  * 创建zookeeper实例 `createZooKeeper`
  * 创建zookeeper节点实例 `createNode`
  * 关闭zookeeper连接 `close`
  * 节点是否存在 `exists`
  * 获取子节点 `getChildren`

> DateUtils 日期工具类

* 获取日期
* 获取时间戳

> NetUtils 网络工具类

* 网上找的工具类，用于获取局域网IP

##### Constant 

与zookeeper相关的常量

* 默认连接地址
* 默认超时时间
* 根节点
* provider基础节点
* consumer基础节点

##### IdGenerator

分布式Id生成器，借助雪花（snowflake）算法

#### rc-core

##### transport

> message

* `MessageFormatConstant `报文协议规定的长度常量
  * 魔术值
  * 魔术值 占的字节数
  * 版本号
  * 版本号 占的字节数
  * 头部区 的长度
  * 头部区 占的字节数
  * 总长度 占的字节数
  * 最大帧数
* `RcRequest `服务调用方发起的请求内容
  * 请求id
  * 请求类型
  * 压缩类型
  * 序列化方式
  * 时间戳
  * 具体的消息体 RequestPayloa
* `RequestPayload`请求调用方 所请求的 接口方法的描述
  * 接口名--全限定名
  * 方法名
  * 参数类型
  * 具体参数
  * 返回值的封装类型
* `ReResponse`服务提供方的响应内容
  * 请求id
  * 时间戳
  * 压缩类型
  * 序列化方式
  * 响应码
  * 响应

##### discovery

> Registry接口

* register 配置与发布服务
* lookup 从注册中心拉取服务列表

> RegistryConfig

注册中心的配置，根据url，解析出注册中心的类型和地址

>AbstractRegistry

提炼共享内容，也可以做共享方法，暂时无共享内容

> impl  extends AbstractRegistry

* ZookeeperRegistry

* NacosRegistry

##### watcher

> UpAndDownLinesWatcher 

* 订阅zookeeper节点动态上下线事件

* 监听节点子节点变化事件

* 对于有变化的节点，通过解析path，拿到服务名，然后由注册中心借由服务名查询服务所在的addresses

* 对于动态上线，新的address一定在addresses中，不在CHANNEL_CACHE中

* 对于动态下线，已下线的address就不在addresses中了，根本拉取不到，但可能还在CHANNEL_CACHE中（心跳检测尚未将其删掉）

* ```java
  /**
   * watchedEvent.getType
   *  Event.EventType.None
   *      watchedEvent.getState()
   *          Event.KeeperState.SyncConnected
   *          Event.KeeperState.AuthFailed
   *          Event.KeeperState.Disconnected
   *          Event.KeeperState.Expired
   *  Event.EventType.NodeCreated
   *  Event.EventType.NodeDeleted
   *  Event.EventType.NodeDataChanged
   *  Event.EventType.NodeChildrenChanged
   */
  ```

##### channelHandler

> ConsumerChannelInitializer  Consumer的通道初始化器

* netty自带的日志处理器
* 消息编码器，封装为定义好的报文 出站 RcResponse --> byteBuf
* 入站的解码器   byteBuf --> RcResponse
* 处理响应的

> handler  这里我按照pipeline写

* `RcRequestEncoder` 
  * consumer 出站 对请求编码
  * 按照报文协议，将RcRequest封装为二进制字节流：byteBuf
  * 期间有序列化和压缩
  * 需要注意，心跳请求不需要封装RequestPayload
* `RcRequestDecoder` 
  * provider 入站 对请求解码
  * 期间有解压缩和反序列化
  * 需要注意，对于心跳请求的响应不需解析RequestPayload
* `MethodCallHandler` 
  * provider 入站 执行方法调用
  * 封装部分响应
  * 如果启动挡板，则直接写出响应
  * 否则处理请求，需结合限流器使用
  * 对于请求方法的调用，使用的是反射
    * 从SERVICES_LIST寻找合适的类，完成方法调用
    * 拿到具体的方法实现类
    * 通过反射调用 a.获取方法对象 b.执行invoke方法
* `RcResponseEncoder` 
  * provider 出站 对响应编码
  * 注意，心跳请求不用封装响应体
  * 这里特别需要关注的是，byteBuf写总长度时，body长度是未知的，因此可以先写“空”这部分。等都写完了，保存当前写指针，回到“空”的起始点，写body，然后回到当前写指针。
* `RcResponseDecoder ` 
  * consumer 入站 对响应解码
* `MySimpleChannelInboundHandler`
  * consumer 入站 对响应进行分析
  * 针对不同的响应码，做不同处理，这里结合熔断器

##### enumeration

>RequestTypeEnum

* ORDINARY
* HEARTBEAT

>ResponseCodeEnum

* METHOD_SUCCESS
* HEARTBEAT_SUCCESS
* RATE_LIMIT
* RESOURCE_NOT_FOUND
* FAIL
* CLOSING

##### compress

压缩协议、压缩方式

* 接口，能够【压缩】与【解压】
* 工厂
  * 维护两个缓存，根据【压缩Type】和【压缩Code】缓存
  * 使用简单工厂，提前构建好缓存，可以根据type或code获得压缩器的包装类`ObjectWrapper<Compressor>`
  * 可以通过`addCompressor`添加缓存，当然缓存为`ObjectWrapper<Compressor>`
* impl
  * `GzipCompressor`

##### serialize

序列化器

* 接口，能够【序列化】与【反序列化】
* 工厂
  * 细节与压缩器一致
* impl
  * `JdkSerializer`
  * `JsonSerializer` 有点问题
  * `HessianSerializer`

##### loadbalance

负载均衡，在consumer端实现

> LoadBalancer 接口

* `selectServiceAddress `根据服务名，选择一个可用的服务
* `reBalance `当感知节点发生动态上下线，需要重新进行负载均衡

>AbstractLoadBalancer

* 维护了一个<serviceName,selector>缓存
* 接口里的方法也都实现了
* abstract getSelector 具体的负载均衡策略所使用的选择器，由子类负责

> impl  extends AbstractLoadBalancer 

* RoundRobinLoadBalancer 轮询
  * 内部类RoundRobinSelector的内部维护了一个服务列表的缓存
  * AtomicInteger index做游标
  * getNext中，按游标顺序往后取，到末尾了就从头开始

* MinResponseTimeLoadBalancer 最短响应时间
  * 内部类MinResponseTimeSelector的内部先获取ANSWER_TIME_CHANNEL_CACHE中最小的
  * getNext中直接从CHANNEL_CACHE返回第一个可用的channel

* ConsistentHashLoadBalancer 一致性hash

  * 内部类ConsistentHashSelector维护一个服务环的缓存

  * 构造时，需将每一个节点转化为虚拟节点，然后挂在到环上

  * ```
    hash(address.toString() + "-" + i); // i是虚拟节点索引
    ```

  * hash算法是自行实现的，不怎么样，分布不均匀，数值也比较大

    * ```java
      private int hash(String s) {
                  MessageDigest md;
                  try {
                      md = MessageDigest.getInstance("MD5");
                  } catch (NoSuchAlgorithmException e){
                      throw new RuntimeException(e);
                  }
                  byte[] digest = md.digest(s.getBytes());
                  // md5得到的结果是一个字节数组，但需要int 4个字节
                  int res = 0;
                  for (int i = 0; i < 4; i++) { // 只取4个字节
                      res = res << 8;
                      if (digest[i] < 0){
                          //   1111 1111 1111 1111 1111 1111 1111 1101     digest[i]是负数，的补码，让那些1消失，不然之后|有很多负数
                          // & 0000 0000 0000 0000 0000 0000 1111 1111     & 4B的255
                          //   0000 0000 0000 0000 0000 0000 1111 1101
                          // | 0000 0000 0000 0000 0000 0000 0000 0000
                          //   0000 0000 0000 0000 0000 0000 1111 1101
                          res = res | (digest[i] & 255);
                      } else {
                          res = res | digest[i];
                      }
                  }
                  return res;
      
              }
      ```

##### config

> Configuration 全局的配置类

* 分组信息
* 端口号

* 应用名
*  注册中心配置
* 序列化类型
* 压缩类型
* 负载均衡器
* Id生成器
* 每一个ip配置一个限流器 
* 每一个ip配置一个断路器

在构造器中，配置的决定性，最终决定方式是代码配置

```java
 public Configuration() {
        // 1、成员变量的默认配置

        // 2、spi机制发现相关配置项
        SpiResolver spiResolver = new SpiResolver();
        spiResolver.loadConfigFromSpi(this);

        // 3、读取xml里的配置配置信息
        XmlResolver xmlResolver = new XmlResolver();
        xmlResolver.loadConfigFromXml(this);

        // 4、编程配置项，RcBootStrap提供

    }
```

>SpiResolver (Service Provider Interfaces)
>
>服务自动发现机制，就是去目标文件下加载信息，做一些特殊处理
> * META-INF-services-文件名：接口全限定名-文件内容：具体的实现的全限定名
> * 本项目对文件内容做了改变，增加了【code】和【type】

* `loadConfigFromSpi`  借助`SpiHandler.getList`得到`ObjectWrapper<T>`

> XmlResolver 从配置文件读取配置信息 不使用dom4j，使用jdk原生的

内容比较多，请查看源码。核心工具方法`parseString`和`parseObject`，其余resolve方法都是这二者的衍生。

> ObjectWrapper<T> 包装类，给实现类增加了code和type

```java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ObjectWrapper<T>  {
    private Byte code;
    private String type;
    private T impl;
}
```

##### spi

> SpiHandler  一个简易版本的spi

* 维护了一个对构造出的对象的缓存`Map<Class<?>, List<ObjectWrapper<?>>> SPI_IMPLEMENT`

* 维护了一个spi原始文件的相关内容的缓存`Map<String, List<String >> SPI_CONTENT`，减少IO开销
* 在静态代码块中完成spi缓存构建
* 提供构建`SPI_IMPLEMENT`的方法
* 还提供获取包装类和包装类集合的方法，优先从`SPI_IMPLEMENT`获取，拿不到再通过反射构建实例。

##### builder

* xml
  * `rc-config.dtd` 对通过xml加载配置所使用的xml进行约束

##### core

> HeartbeatDetector 心跳请求
>
> 探活，感知哪些还是正常，哪些不对劲

* 新开线程，使用周期性任务，发送心跳请求
* 多次重试都失败，则从`CHANNEL_CACHE`下线

> ShoutDownHolder 挡板相关的常量 

* `AtomicBoolean BAFFLE` 请求挡板 启用或不启用 false：不启用
* `LongAdder REQUEST_COUNT` 请求计数器

> RcShoutDownHook 服务提供方关闭的钩子函数
>
> 一旦关闭服务提供方，MethodCallHandler就会返回对应响应码
>
> 启动服务时加入Runtime.getRuntime().addShutdownHook(new RcShoutDownHook())

* 服务关闭时，每100ms统计剩余请求数，到0就放行
* 或者超过10s，也放行

##### protection

> RateLimiter 限流器

* allowRequest  判断请求是否可以放行

> impl->TokenBucketRateLimiter 令牌桶限流器

* 最高 token数 容量
* 剩余可用 token数
* 添加令牌速率
* 上一次放token时间
* allowRequest()
  * 达到添加token的条件，解按照一定规则放token，不超过容量
  * 如果tokens用完了，就限流，否则只是减少token，放行

> CircuitBreaker  熔断器，就不抽象了
>
> 本例简化，从open、halfOpen、close简化到只有open和close两种状态

* isOpen 
* 请求 总量
* 请求 异常数
* 异常 阈值
* 异常 比例
* isBreak()
  * 根据是否超过最大异常数，或者超过最大异常比例，判断断路器时闭合还是开启
  * true: 是开启状态  false: 是闭合状态

##### proxy

> handler

* RcConsumerInvocationHandler
  * Consumer的ReferenceConfig的代理部分所需的InvocationHandler
  * 内含对代理方法的重试的逻辑

##### NettyBootstrapInitializer 

* 提供Netty的Bootstrap单例，客户端需要的辅助类

##### ProtocolConfig （废弃）

* 配置序列化协议

##### ServiceConfig 

* 使用RcBootstrap发布服务时配置该服务

##### ReferenceConfig 

* 使用RcBootstrap消费服务时生成动态代理对象

##### RcBootstrap 

* 辅助启动类，饿汉式单例

* provider和consumer启动时的各种配置方法都在此类

### rc-manager

* 负责管理服务