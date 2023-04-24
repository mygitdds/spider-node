package cn.spider.framework.common.utils;

import io.vertx.core.Vertx;
import io.vertx.core.shareddata.LocalMap;
import io.vertx.core.shareddata.SharedData;

/**
 * @BelongsProject: spider-node
 * @BelongsPackage: cn.spider.framework.common.utils
 * @Author: dengdongsheng
 * @CreateTime: 2023-04-07  19:57
 * @Description: TODO
 * @Version: 1.0
 */
public class BrokerInfoUtil {
    public static String queryBrokerName(Vertx vertx){
        SharedData sharedData = vertx.sharedData();
        LocalMap<String, String> localMap = sharedData.getLocalMap("config");
        return localMap.get("broker-name");
    }

    public static String queryBrokerIp(Vertx vertx){
        SharedData sharedData = vertx.sharedData();
        LocalMap<String, String> localMap = sharedData.getLocalMap("config");
        return localMap.get("broker-ip");
    }

    public static int queryTranscriptNum(Vertx vertx){
        SharedData sharedData = vertx.sharedData();
        LocalMap<String, String> localMap = sharedData.getLocalMap("config");
        return Integer.parseInt(localMap.get("broker-transcript-num"));
    }


}
