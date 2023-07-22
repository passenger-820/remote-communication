# Remote-Communication

尝试手写rpc框架

* rc-demo
  * rc-api
    * 示例api
  * rc-provider-demo 服务提供方
    * 实现了示例api
    * 使用RcBootstrap的一些方法配置与启动
  * rc-consumer-demo 服务调用方
    * 使用RcBootstrap的一些方法配置与启动
* rc-framework
  * rc-common
    * utils
      * zookeeper
        * zookeeperNode类
        * zookeeper工具类
          * 创建zookeeper实例
          * 创建zookeeper节点实例
          * 关闭zookeeper连接
          * 节点是否存在
          * 获取子节点
      * NetUtils
        * 获取局域网IP
      * Constant
        * zookeeper 连接 参数
        * node path 名称
  * rc-core
    * discovery 服务注册与发现中心
      * impl 具体的实现，包括zk，nacos（伪）
      * Registry
      * AbstractRegistry
      * RegistryConfig 配置服务中心
        * 持有typedConnectingString
        * 解析前者，并获得服务中心Type和Host
        * 根据tCS使用简单工厂设计模式获取注册中心
    * ProtocolConfig 配置序列化协议
    * ServiceConfig 使用RcBootstrap发布服务时配置该服务
    * ReferenceConfig 使用RcBootstrap消费服务时生成动态代理对象
    * RcBootstrap 辅助启动类，饿汉式单例
      * provider和consumer启动时的各种配置方法都在此类
* rc-manager
  * 负责管理服务