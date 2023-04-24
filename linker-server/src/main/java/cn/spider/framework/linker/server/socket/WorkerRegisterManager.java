package cn.spider.framework.linker.server.socket;
import cn.spider.framework.common.utils.BrokerInfoUtil;
import cn.spider.framework.linker.server.enums.ClientStatus;
import com.alibaba.fastjson.JSON;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.NetServer;
import io.vertx.core.net.SocketAddress;
import lombok.extern.slf4j.Slf4j;

/**
 * @program: spider-node
 * @description: 下游服务注册管理
 * @author: dds
 * @create: 2023-03-01 22:14
 */
@Slf4j
public class WorkerRegisterManager {
    /**
     * socket-server
     */
    private NetServer netServer;
    /**
     * client管理者
     */
    private ClientRegisterCenter clientRegisterCenter;

    private Vertx vertx;

    public WorkerRegisterManager(NetServer netServer, ClientRegisterCenter clientRegisterCenter,Vertx vertx) {
        this.netServer = netServer;
        this.clientRegisterCenter = clientRegisterCenter;
        this.vertx = vertx;
        init();
    }

    private void init() {
        createConnect();
        startNetServer();
    }

    /**
     * 开启 接受创建链接，关闭链接，需要做的事情
     */
    public void createConnect() {
        netServer.connectHandler(socket -> {

            socket.handler(buffer -> {
                // 在这里应该解析报文，封装为协议对象，并找到响应的处理类，得到处理结果，并响应
                SocketAddress socketAddress = socket.remoteAddress();
                String ip = socketAddress.host();
                ClientInfo clientInfo = JSON.parseObject(buffer.toString(), ClientInfo.class);
                clientInfo.setClientStatus(ClientStatus.NORMAL);
                clientInfo.setRemoteAddress(ip);
                log.info("接收到的数据为 {}", JSON.toJSONString(clientInfo));
                // 按照协议响应给客户端
                clientRegisterCenter.registerClient(clientInfo);
                socket.write(Buffer.buffer("Hello Vertx from Server!"));
            });

            // 监听客户端的退出连接
            socket.closeHandler(close -> {
                // 退出，移除，client
                SocketAddress socketAddress = socket.remoteAddress();
                String ip = socketAddress.host();
                // 移除ip对应的数据,防止下次被选中
                clientRegisterCenter.destroy(ip);
            });
        });
    }

    public void startNetServer(){
        String brokerIp = BrokerInfoUtil.queryBrokerIp(this.vertx);
        netServer.listen(9063, brokerIp, res -> {
            if (res.succeeded()) {
                System.out.println("服务器启动成功");
            }
        });
    }

}
