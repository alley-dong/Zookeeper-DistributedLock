package com.dongchanglong.zookeeper;

import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import java.util.concurrent.CountDownLatch;

public class DefaultWatch implements Watcher {

    CountDownLatch cc;
    public void setCc(CountDownLatch cc) {
        this.cc = cc;
    }

    //  创建session连接的watch监控回调
    @Override
    public void process(WatchedEvent watchedEvent) {
        switch (watchedEvent.getState()) {
            case Unknown:
                break;
            case Disconnected:
                break;
            case NoSyncConnected:
                break;
            case SyncConnected:
                // 只有连接成功了  才会停止await 阻塞
                cc.countDown();
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
        System.out.println(watchedEvent.toString());
    }
}
