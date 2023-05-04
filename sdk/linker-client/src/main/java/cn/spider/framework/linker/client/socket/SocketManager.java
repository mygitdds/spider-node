package cn.spider.framework.linker.client.socket;

import cn.spider.framework.common.utils.ExceptionMessage;
import cn.spider.framework.linker.client.timer.BusinessTimer;
import cn.spider.framework.linker.client.util.IpUtil;
import com.google.common.collect.Maps;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.NetClient;
import io.vertx.core.net.NetClientOptions;
import io.vertx.core.net.NetSocket;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @program: spider-node
 * @description: 链接管理
 * @author: dds
 * @create: 2023-03-02 21:22
 */
@Slf4j
public class SocketManager {

    // 服务器启动-执行该逻辑
    private NetClient client;
    // 通信套接字

    private String workerName;

    private String workerIp;

    private Set<String> spiderServerIps;

    private BusinessTimer businessTimer;

    private Map<String, NetSocket> serverMap;

    public SocketManager(Vertx vertx, String workerName, String spiderServerIp, BusinessTimer businessTimer) {
        NetClientOptions options = new NetClientOptions()
                .setLogActivity(true)
                .setConnectTimeout(10000);
        this.client = vertx.createNetClient(options);
        this.workerName = workerName;
        this.businessTimer = businessTimer;
        try {
            workerIp = IpUtil.buildLocalHost();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        this.serverMap = Maps.newHashMap();
        String[] serverIps = spiderServerIp.split(",");
        this.spiderServerIps = new HashSet<>(Arrays.asList(serverIps));
        connect();

    }

    public void connect() {
        for (String serverIp : spiderServerIps) {
            connectSpiderServer(serverIp);
        }
    }

    public void monitorSocket(NetSocket socket, String serverIp) {
        // 监听客户端的退出连接
        socket.closeHandler(close -> {
            log.warn("客户端退出后重试");
            String serverIpNew = serverIp;
            // 移除进行重连
            this.serverMap.remove(serverIpNew);
            // 取消定时任务
            this.businessTimer.cancelHeart(serverIpNew);
            // 进行链接
            connectSpiderServer(serverIpNew);
        });
    }

    public void connectSpiderServer(String serverIp) {
        this.client.connect(9063, serverIp, res -> {
            if (res.succeeded()) {
                NetSocket socket = res.result();
                this.serverMap.put(serverIp, socket);
                // 回写服务信息
                JsonObject clientInfo = new JsonObject();
                clientInfo.put("ip", workerIp);
                clientInfo.put("workerName", this.workerName);
                clientInfo.put("isHeart", false);
                socket.write(Buffer.buffer(clientInfo.toString()));
                monitorSocket(res.result(), serverIp);
                this.businessTimer.registerSocketHeart(serverIp,this);
                // 注册 heart
            } else {
                businessTimer.delayConnectServer(serverIp, this);
                log.error("跟spider-server通信进行链接失败 serverIp {} 错误信息为 {}", serverIp, ExceptionMessage.getStackTrace(res.cause()));
            }
        });
    }

    public void heart(String serverIp) {
        NetSocket socket = this.serverMap.get(serverIp);
        JsonObject clientInfo = new JsonObject();
        clientInfo.put("ip", workerIp);
        clientInfo.put("workerName", this.workerName);
        clientInfo.put("isHeart", true);
        socket.write(Buffer.buffer(clientInfo.toString()));
    }
}
