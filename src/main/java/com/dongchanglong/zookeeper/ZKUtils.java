package com.dongchanglong.zookeeper;

import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

public class ZKUtils {

    private static ZooKeeper zk;

    // !!!! 可以在设置zk访问主机的时候 指定工作的目录 /testConf作为当前客户端的根目录
    private static String address = "192.168.111.128:2181,192.168.111.129:2181,192.168.111.130:2181,192.168.111.131:2181/testConf";

    // 对session的watch和节点（path）的watch进行区分。因为session平时用的很少。
    private static DefaultWatch defaultWatch = new DefaultWatch();

    // 当newzk的时候他会很快的返回  但是还有一个异步的事情 是真正的去连接zk 然后创建session，如果去调用zk方法 其实zk是没有建立完成的。
    static CountDownLatch init  = new CountDownLatch(1);

    public static ZooKeeper getZk() {
        // 创建zk连接
        try {
            zk  = new ZooKeeper(address,1000,defaultWatch);
            // 将CountDownLatch赋值给watch
            defaultWatch.setCc(init);
            // 阻塞 等待zk创建成功后回调
            init.await();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return zk;
    }

}
