package com.dongchanglong.zookeeper.lock;

import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class WatchCallBack implements Watcher,AsyncCallback.StringCallback, AsyncCallback.Children2Callback,AsyncCallback.StatCallback {
    // 分别实现 Watch和 create要求的CallBack类型

    ZooKeeper zk;
    // 当前线程的名字
    String threadName;
    // 获取到的临时序列节点名称
    String pathName;
    CountDownLatch countDownLatch = new CountDownLatch(1);


    // 获得锁
    public void tryLock(){

        // 创建 临时序列节点
        // 每个线程都会创建自己的临时节点，都会回调
        zk.create("/lock",threadName.getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL,this,"ABC");

        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    // 释放锁
    public void unLock(){
        try {
            // 删除节点
            zk.delete(pathName,-1);
            System.out.println(threadName+"---------working over---------");
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (KeeperException e) {
            e.printStackTrace();
        }
    }

    // 事件回调通知
    @Override
    public void process(WatchedEvent watchedEvent) {
        switch (watchedEvent.getType()) {
            case None:
                break;
            case NodeCreated:
                break;
            case NodeDeleted:
                // 当第一个 节点被删除的时候  并不是所有人都getChildren，只有第二个收到了删除回调事件 重新获得children 判断自己是不是第一个。
                zk.getChildren("/",false,this,"321");
                break;
            case NodeDataChanged:
                break;
            case NodeChildrenChanged:
                break;
        }
    }

    // 创建节点回调
    // 回调的时候 如果创建成功会将临时 序列id返回
    @Override
    public void processResult(int i, String s, Object o, String name) {
        if (name != null){
            System.out.println(threadName + "===== create 临时序列节点======"+name);
            // 赋值
            pathName = name;
            // 当十个线程创建节点回调的时候 只有第一个人才能获得锁，就是序列id最小的，所以要去testLock这个根目录下获取所有的节点 判断自己是不是最小的
            zk.getChildren("/",false,this,"321");

        }
    }

    // 重点逻辑！！！
    // 获取根目录下子节点的回调
    @Override
    public void processResult(int i, String s, Object o, List<String> children, Stat stat) {

        /*
         * 十个线程同时zk.getChildren获得父目录的节点，获取完之后一定会callBack回调，
         * 也就是只要进入了当前的这个回调方法   就是自己一定是创建完了 且看到了自己 以及自己前面的所有节点。
         */

        // children里面都是乱序的
        // 排序
        Collections.sort(children);
        // 因为pathName是带/ 所以要截取。并判断出现的位置是否是第一个。
        int i1 = children.indexOf(pathName.substring(1));
        // 十个线程都去判定自己是不是第一个出现的 是的话就释放阻塞 处理他自己的业务逻辑
        if (i1 == 0){
            System.out.println(threadName + "===== I am first........");
            try {
                // 解决 当业务代码执行太快的情况
                // 谁抢到了锁 谁就把锁的信息 写到锁目录的node里面去
                zk.setData("/",pathName.getBytes(),-1);

                // 释放阻塞
                countDownLatch.countDown();
            } catch (KeeperException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }else {
            // 如果不是第一个出现的  取出当前节点的前一个节点
            // !!!! 并且这里要设置一个Watch监控，做这步的目的就是为了 监控前面的那个节点未来被删除或者消失了 有一个事件产生， 而从产生再次去判断是否要去抢锁。
            // 并且需要对zk.exists 进行callBack，因为zk.exists未必能成功 。
            // !!!! 剩下九个线程都zk.exists之后  他们都监控的是前面那个节点，
            zk.exists("/"+children.get(i1-1),this,this,"ABC");
        }
    }

    // exists的回调
    @Override
    public void processResult(int i, String s, Object o, Stat stat) {

    }

    public ZooKeeper getZk() {
        return zk;
    }
    public void setZk(ZooKeeper zk) {
        this.zk = zk;
    }
    public String getThreadName() {
        return threadName;
    }
    public void setThreadName(String threadName) {
        this.threadName = threadName;
    }
    public String getPathName() {
        return pathName;
    }
    public void setPathName(String pathName) {
        this.pathName = pathName;
    }


}
