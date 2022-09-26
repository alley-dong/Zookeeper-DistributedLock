package com.dongchanglong.zookeeper;

public class MyConf {

    // 这个类 才是你未来最关心的地方，因为在企业当中 你可能会存复杂的json或者xml 将它变成二进制 存到zk的节点当中去。

    private String conf;

    public String getConf() {
        return conf;
    }
    public void setConf(String conf) {
        this.conf = conf;
    }
    @Override
    public String toString() {
        return "MyConf{" +
                "conf='" + conf + '\'' +
                '}';
    }
}
