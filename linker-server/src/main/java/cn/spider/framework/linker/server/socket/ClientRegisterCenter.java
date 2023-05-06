package cn.spider.framework.linker.server.socket;

import cn.spider.framework.linker.server.enums.ClientStatus;
import cn.spider.framework.proto.grpc.VertxTransferServerGrpc;
import com.google.common.collect.Lists;
import io.grpc.ManagedChannel;
import io.vertx.core.Vertx;
import io.vertx.grpc.VertxChannelBuilder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @program: spider-node
 * @description: 客户端的注册中心- 当客户端启动完成的时候会进行上报
 * @author: dds
 * @create: 2023-02-24 17:32
 */

public class ClientRegisterCenter {
    // client的map便于调用
    private Map<String, List<ClientInfo>> clientMap;

    private Vertx vertx;
    // ip-workerName映射
    private Map<String, String> ipWorkerInsinuate;

    public ClientRegisterCenter(Vertx vertx) {
        this.vertx = vertx;
        this.clientMap = new HashMap<>();
        this.ipWorkerInsinuate = new HashMap<>();
    }

    /**
     * 注册-client
     *
     * @param clientInfo
     */
    public void registerClient(ClientInfo clientInfo) {
        if (!this.clientMap.containsKey(clientInfo.getWorkerName())) {
            this.clientMap.put(clientInfo.getWorkerName(), Lists.newArrayList(clientInfo));
        }
        // 初始化vertx-grpc客户端
        ManagedChannel channel = VertxChannelBuilder
                .forAddress(vertx, clientInfo.getIp(), 9974)
                .usePlaintext()
                .build();
        // 构造grpc代理
        VertxTransferServerGrpc.TransferServerVertxStub serverVertxStub = VertxTransferServerGrpc.newVertxStub(channel);
        // 设置代理,方便后续调用
        clientInfo.setServerVertxStub(serverVertxStub);
        List<ClientInfo> clientInfos = clientMap.get(clientInfo.getWorkerName());
        clientInfos.add(clientInfo);
        ipWorkerInsinuate.put(clientInfo.getIp(), clientInfo.getWorkerName());
        // 建立长连接-->当断开的时候,及时去除ClientInfo
    }

    public void destroy(String ip) {
        String workerName = ipWorkerInsinuate.get(ip);
        List<ClientInfo> clientInfos = clientMap.get(workerName);
        List<ClientInfo> clientInfosNew = clientInfos.stream().filter(item-> !item.getIp().equals(ip)).collect(Collectors.toList());
        clientMap.put(workerName,clientInfosNew);
        // 移除隐射关系
        ipWorkerInsinuate.remove(ip);
    }

    public ClientInfo queryClientInfo(String workerName) {
        List<ClientInfo> clientInfos = clientMap.get(workerName);
        for(ClientInfo info : clientInfos){
            if(info.getClientStatus().equals(ClientStatus.NORMAL) && Objects.nonNull(info.getServerVertxStub())){
                return info;
            }
        }
        return null;
    }

}
