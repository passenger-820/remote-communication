package cn.edu.cqu.zookeeper;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

public class ZookeeperTest {
    ZooKeeper zooKeeper;

    @Before
    public void createZk(){
        // 连接参数，如果是多个，可以为"127.0.0.1:3000,127.0.0.1:3001,127.0.0.1:3002"
        String connectString = "127.0.0.1:2181";
        // 超时时间
        int timeout = 10000;
        try {
            zooKeeper = new ZooKeeper(connectString,timeout,new MyWatcher()); // watcher先为null，用默认的;自己写了MyWatcher后可以替换掉
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testCreatePersistentNode(){
        try {
            String result = zooKeeper.create("/cqu", "hello rc".getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
            System.out.println("result = " + result);
            /*
            ...
            19:53:08.029 [main-SendThread(127.0.0.1:2181)] DEBUG org.apache.zookeeper.ClientCnxn - Reading reply session id: 0x1000041880f0001, packet:: clientPath:null serverPath:null finished:false header:: 1,1  replyHeader:: 1,18,0  request:: '/cqu,#68656c6c6f207263,v{s{31,s{'world,'anyone}}},0  response:: '/cqu
result = /cqu
             */
        } catch (KeeperException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testDeletePersistentNode(){
        try {
            // version:是cap理论的， 乐观锁， -1表示无视版本号
            zooKeeper.delete("/cqu",-1);
        } catch (KeeperException | InterruptedException e) {
            e.printStackTrace();
        } finally {
            if (zooKeeper != null){
                try {
                    zooKeeper.close();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Test
    public void testExistPersistentNode(){
        try {
            // version:是cap理论的， 乐观锁， -1表示无视版本号
            Stat stat = zooKeeper.exists("/cqu", null);

            zooKeeper.setData("/cqu","hi cqu".getBytes(),-1);

            // 当前节点的数据版本
            int version = stat.getVersion();
            System.out.println("version = " + version);
            // 当前节点的acl数据版本
            int aversion = stat.getAversion();
            System.out.println("aversion = " + aversion);
            // 当前节点的子节点的数据版本
            int cversion = stat.getCversion();
            System.out.println("cversion = " + cversion);

        } catch (KeeperException | InterruptedException e) {
            e.printStackTrace();
        } finally {
            if (zooKeeper != null){
                try {
                    zooKeeper.close();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Test
    public void testWatcher(){
        try {
            zooKeeper.exists("/cqu",true); // true表示使用默认的watcher，此为我们的MyWatcher
            while (true){
                Thread.sleep(1000);
            }
        } catch (KeeperException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            if (zooKeeper != null){
                try {
                    zooKeeper.close();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }


}
