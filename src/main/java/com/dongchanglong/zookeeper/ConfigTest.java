package com.dongchanglong.zookeeper;

import org.apache.zookeeper.ZooKeeper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;


public class ConfigTest {


    ZooKeeper zk;

    @Before
    public void conn(){
        zk = ZKUtils.getZk();
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
    public void getConf(){

        // 为了避免一层层的内部实现，创建WatchCallBack
        // 既是一个Watch、也是exists和getData的CallBack
        WatchCallBack watchCallBack = new WatchCallBack();
        watchCallBack.setZk(zk);

        // 因为使用异步  取数据是跑的另一个线程， 当前这个是工作的线程， 要准备一个取数据的类MyConf，最终取到的数据要存到这个类中，解决了两个线程中数据传递的问题
        MyConf myConf = new MyConf();
        watchCallBack.setConf(myConf);

        // getData和exists更倾向于后者，判断是否存在  通过异步的方式  回调后 如果存在再去取。 一步一步地执行。
        // zk.exists("/Appconf",watchCallBack,watchCallBack,"ABC");

        // 上面那行注释掉的代码 被封装了
        watchCallBack.aWait();
        /*
         * 这里有两种情况：
         *
         * 1.节点不存在的时候：当节点不存在的时候 watchCallBack.aWait() 为了能取到节点数据 会进行阻塞。
         * 如果这个时候有人 新增了被监控的那个节点，那么就会触发节点创建事件NodeCreated，NodeCreated中继续获取getData，那么getData回调就会释放阻塞。
         *
         * 2.节点存在的时候：watchCallBack.aWait() 为了能取到节点数据 会进行阻塞。exists回调就会取到stat的对象，取到对象那么就会继续取getData，
         * 进入getData回调的时候 就会将阻塞释放。
         *
        */
        while (true){

            // 如果watch事件监控到了delete事件，那么就将conf清空，在这里进行判断
            if (myConf.getConf().equals("")){
                System.out.println("conf diu le---------------");

                // 配置被清空了 那么这个地方就要被阻塞
                // 这个代码的语义就是  我想要的节点不存在就阻塞。所以不需要自己写阻塞。只需要调用exists就行。
                watchCallBack.aWait();
            }
            System.out.println(watchCallBack.getConf());

            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }
}
