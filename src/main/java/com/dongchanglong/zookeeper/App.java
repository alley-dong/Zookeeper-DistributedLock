package com.dongchanglong.zookeeper;

import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

/**
 * Hello world!
 *
 */
public class App 
{
    public static void main( String[] args ) throws IOException, InterruptedException, KeeperException {
        System.out.println( "Hello World!" );

        // 因为zookeeper有session  所以没有连接池的概念

        // 线程安全的  防止zk没有链接成功  就调用zk（当newzk的时候他会很快的返回  但是还有一个异步的事情 是真正的去连接zk 然后创建session）
        final CountDownLatch cd = new CountDownLatch(1);

        // 如果有很多客户端连接  其实指不定连接的是谁  因为他会走一次负载  每个人都可以读写 写会转给leader处理
        // session的超时时间  通过下面代码 当启动代码创建了ooxx  等代码运行完毕ooxx三秒后消失
        // watch观察回调  它分为两类：
        // 第一类：new zk的时候传入的watch 这个watch是session级别的 跟path、node是没有关系，其他节点的事件是收不到的 他只能知道关于session的连接和连接别人的过程, 当被连接的zk挂了 那么这个事件会被回调
        final ZooKeeper zooKeeper = new ZooKeeper("192.168.111.128:2181,192.168.111.129:2181,192.168.111.130:2181,192.168.111.131:2181", 3000, new Watcher() {
            // 回调方法
            @Override
            public void process(WatchedEvent watchedEvent) {
                Event.KeeperState state = watchedEvent.getState();
                String path = watchedEvent.getPath();
                Event.EventType type = watchedEvent.getType();
                System.out.println(watchedEvent.toString());
                switch (state) {
                    case Unknown:
                        break;
                    case Disconnected:
                        break;
                    case NoSyncConnected:
                        break;
                    case SyncConnected:
                        // 只有链接成功之后 减一  下面的阻塞状态才能结束  继续向下走
                        cd.countDown();
                        System.out.println("连接成功====");
                        break;
                    case AuthFailed:
                        break;
                    case ConnectedReadOnly:
                        break;
                    case SaslAuthenticated:
                        break;
                    case Expired:
                        break;
                }
            }
        });

        // 在这里发生阻塞 ，如果不阻塞下面获取zk状态的代码 在zk没链接成功的时候 就会 ing.....
        cd.await();
        // 获取zk状态
        ZooKeeper.States state = zooKeeper.getState();
        switch (state) {
            case CONNECTING:
                System.out.println("ing........");
                break;
            case ASSOCIATING:
                break;
            case CONNECTED:
                System.out.println("ed..........");
                break;
            case CONNECTEDREADONLY:
                break;
            case CLOSED:
                break;
            case AUTH_FAILED:
                break;
            case NOT_CONNECTED:
                break;
        }


        // node有两种创建方式：一种是同步阻塞的、一种是异步回调的
        // 参数分别是节点名称、数据（二进制安全的byte数组）、访问权限、节点类型（持久、序列、临时）
        String pathName = zooKeeper.create("/ooxx", "oldData".getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);


        // Watch的第二类： 在上面建立zookeeper连接的时候已经放了一个watch，现在是针对这个ooxx路径放的另外一个watch 在取数据的同时 注册一个watch  如果未来这个ooxx被修改了那么就会回调process
        // Watch只会监控一次！！！ 如果想继续回调需要在process中通过 true来继续监控
        // watch注册 只会出现在 读类型的方法中 比如get、exites等   因为写是产生事件的
        // stat 是元数据 就是事务id这一类
        final Stat stat = new Stat();
        byte[] data = zooKeeper.getData("/ooxx", new Watcher() {
            @Override
            public void process(WatchedEvent watchedEvent) {
                System.out.println(watchedEvent);

                // 继续监控这个节点
                try {
                    // !!!!!! 当为true的时候会使用new zk的那个监控， 若想用刚才的那个watch 只需要改成this 就行
                    byte[] data1 = zooKeeper.getData("/ooxx", this, stat);
                } catch (KeeperException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }, stat);
        System.out.println(new String(data));

        // 修改数据会触发 创建事件的process回调方法
        Stat stat1 = zooKeeper.setData("/ooxx", "newData".getBytes(), 0);
        // 第二次修改的时候就不会再触发事件回调了  因为只会监控一次
        Stat stat2 = zooKeeper.setData("/ooxx", "newData1".getBytes(), stat1.getVersion());


        // 异步的方式
        // 没有返回值 说明不会阻塞， 请求一旦回来 会调用AsyncCallback下的processResult方法
        System.out.println("-----start-----");
        zooKeeper.getData("/ooxx", false, new AsyncCallback.DataCallback() {
            @Override
            //状态码、路径、数据、元数据
            public void processResult(int i, String s, Object o, byte[] bytes, Stat stat) {
                System.out.println("-----call back-----");
                System.out.println(new String(bytes));
                System.out.println("-----ctx-----"+ o.toString());
            }
        },"abc"); // 是object类型 可以是任意对象
        System.out.println("------over------");

        Thread.sleep(222222222);
    }
}
