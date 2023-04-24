package cn.spider.framework.linker.client.socket;

import cn.spider.framework.linker.client.util.IpUtil;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.NetClient;
import io.vertx.core.net.NetClientOptions;
import io.vertx.core.net.NetSocket;
import org.springframework.stereotype.Component;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.util.Enumeration;

/**
 * @program: spider-node
 * @description: 链接管理
 * @author: dds
 * @create: 2023-03-02 21:22
 */
public class SocketManager {

    // 服务器启动-执行该逻辑
    private NetClient client;
    // 通信套接字
    private NetSocket socket;

    private String workerName;

    private String workerIp;

    public SocketManager(Vertx vertx,String workerName) {
        NetClientOptions options = new NetClientOptions()
                .setLogActivity(true)
                .setConnectTimeout(10000);
        this.client = vertx.createNetClient(options);
        this.workerName = workerName;
        try {
            workerIp = IpUtil.buildLocalHost();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        connect();

    }

    public void connect(){
        this.client.connect(9063, "localhost", res -> {
            if (res.succeeded()) {
                this.socket = res.result();
                // 回写服务信息
                JsonObject clientInfo = new JsonObject();
                clientInfo.put("ip",workerIp);
                clientInfo.put("workerName",this.workerName);
                this.socket.write(Buffer.buffer(clientInfo.toString()));
                monitorSocket();
            } else {
                System.out.println("Failed to connect: " + res.cause().getMessage());
            }
        });
    }

    public void monitorSocket(){
        // 监听客户端的退出连接
        this.socket.closeHandler(close -> {
            System.out.println("客户端退出连接");
            //重新发起链接
            try {
                this.connect();
            } catch (Exception e) {
                // 重试。
            }
        });
    }
}
