package com.dongchanglong.zookeeper.lock;

import com.dongchanglong.zookeeper.ZKUtils;
import org.apache.zookeeper.ZooKeeper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TestLock {

    ZooKeeper zk;

    @Before
    public void conn(){
        zk = ZKLockUtils.getZk();
    }

    @After
    public void close(){
        try {
            zk.close();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    @Test
    public void lcok(){

        // 抛出十个线程
        for (int i = 0; i < 10; i++) {
            new Thread(){
                @Override
                public void run() {
                    // 每一个线程要做什么：

                    // 每个线程都有一个自己的对象
                    WatchCallBack watchCallBack = new WatchCallBack();
                    watchCallBack.setZk(zk);

                    // 获取当前线程的名字
                    String threadName = Thread.currentThread().getName();
                    watchCallBack.setThreadName(threadName);

                    // 去抢锁
                    watchCallBack.tryLock();

                    // 干活
                    try {
                        Thread.sleep(2000);
                        System.out.println(threadName+"-----working-----------");
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    // 释放锁
                    watchCallBack.unLock();


                    // 但是这个地方有问题， 就是当把Thread.sleep 注释掉的时候，会发现第一个线程执行完 就不执行了。
                    // 因为 执行的实在是太快了，还没等第二个进行监控第一个，第一个就已经释放锁了，第二个就再也监控不到第一个的删除事件了。

                }
            }.start();

        }

        while (true){

        }
    }

}
