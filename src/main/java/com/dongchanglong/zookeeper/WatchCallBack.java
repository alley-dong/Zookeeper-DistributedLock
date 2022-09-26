package com.dongchanglong.zookeeper;

import org.apache.zookeeper.AsyncCallback;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;

import java.util.concurrent.CountDownLatch;

public class WatchCallBack implements Watcher, AsyncCallback.StatCallback, AsyncCallback.DataCallback {

    // 分别实现Watch来监控path、exists是状态的callBack、getData的callBack


    // 将zk通过set的方式获取
    ZooKeeper zk;
    // 最终取到的数据是要存到这个类种
    MyConf conf;
    CountDownLatch countDownLatch = new CountDownLatch(1);

    // 没有data的 是 exists的callBack
    @Override
    public void processResult(int i, String s, Object o, Stat stat) {
        // 判断不为空 则可以获取getData
        if (stat != null){
            // 因为它本身就是Watch 所以调用this就行 获取自己的Watch、CallBack也是同理
            zk.getData("/Appconf",this,this,"ABC123");
        }
    }

    // 有data的是 getData的callBack
    @Override
    public void processResult(int i, String s, Object o, byte[] bytes, Stat stat) {
        // 当getData的方法被回调了  要先判断是否有数据返回
        if (bytes != null){
            // 将获取到的数据存入MyConf中
            conf.setConf(new String(bytes));

            // 取到了Myconf 则释放exists的阻塞
            countDownLatch.countDown();
        }
    }

    // 假设我现在已经取到了conf ，但是如果别人给他修改了 就需要靠Watch监控
    @Override
    public void process(WatchedEvent watchedEvent) {
        switch (watchedEvent.getType()) {
            case None:
                break;
            case NodeCreated:
                // 当节点被创建了  就需要继续取getData 因为在 exists的时候将代码阻塞了
                zk.getData("/Appconf",this,this,"ABC123");
                break;
            case NodeDeleted:  // 节点被删除
                // 如果节点不小心被删除了，首先要将conf清空 并在业务代码判断
                conf.setConf("");

                // 同时要给 countDownLatch 重新赋值。 让业务代码 重新走exists流程，等待节点数据有则打印 无则阻塞。
                countDownLatch = new CountDownLatch(1);
                break;
            case NodeDataChanged:  // 节点被变更
                // 如果被变更了，那么就要将取数据的getData再执行一边，之后就会回调并更新配置
                zk.getData("/Appconf",this,this,"ABC123");
                break;
            case NodeChildrenChanged:
                break;
        }
    }

    // 这个方法 是将ConfigTest中的代码进行了封装
    public void aWait(){
        zk.exists("/Appconf",this,this,"ABC");
        try {
            // 要对这里进行阻塞   取到数据之后进行释放
            countDownLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public MyConf getConf() {
        return conf;
    }
    public void setConf(MyConf conf) {
        this.conf = conf;
    }
    public void setZk(ZooKeeper zk) {
        this.zk = zk;
    }
    public ZooKeeper getZk() {
        return zk;
    }
}
