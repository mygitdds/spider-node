package cn.spider.framework.flow.business;

import cn.spider.framework.db.list.RedisList;
import cn.spider.framework.db.map.RedisMap;
import cn.spider.framework.flow.business.data.BusinessFunctions;
import cn.spider.framework.flow.business.data.DerailFunctionVersion;
import cn.spider.framework.flow.business.data.FunctionWeight;
import com.alibaba.fastjson.JSON;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @BelongsProject: spider-node
 * @BelongsPackage: cn.spider.framework.flow.business
 * @Author: dengdongsheng
 * @CreateTime: 2023-04-11  15:42
 * @Description: 业务功能的管理
 * @Version: 1.0
 */
public class BusinessManager {

    private final String FUNCTION_MAP_PREFIX = "FUNCTION_MAP_PREFIX";

    private final String FUNCTION_WEIGHT_CONFIG_PREFIX = "FUNCTION_WEIGHT_CONFIG_PREFIX";

    private final String BUSINESS_FUNCTION_CONFIG = "BUSINESS_FUNCTION_CONFIG";

    private RedisTemplate redisTemplate;

    private RedisMap businessMap;

    public BusinessManager(RedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.businessMap = new RedisMap(redisTemplate, BUSINESS_FUNCTION_CONFIG);
    }

    // 注册功能
    public String registerBusinessFunction(BusinessFunctions businessFunctions) {

        if (StringUtils.isEmpty(businessFunctions.getId())) {
            // 给一个uuid
            businessFunctions.setId(UUID.randomUUID().toString());
        }
        businessMap.put(businessFunctions.getId(), businessFunctions);
        return businessFunctions.getId();
    }

    public List<Object> queryBusinessFunctions() {
        Map<String, Object> functionMap = businessMap.getHashObject();
        return functionMap.values().stream().collect(Collectors.toList());
    }

    // 配置权重
    public void functionWeightConfig(FunctionWeight weight) {

    }

    /**
     * 开关版本-- 只对后续有效-- 移除权重
     */
    public void derailFunctionVersion(DerailFunctionVersion derailFunctionVersion) {

        RedisList businessList = new RedisList(redisTemplate, derailFunctionVersion.getFunctionId());

        List<BusinessFunctions> functions = businessList.queueData()
                .stream()
                .map(item -> JSON.parseObject(item, BusinessFunctions.class))
                .collect(Collectors.toList());
        functions.forEach(item -> {
            if (StringUtils.equals(item.getVersion(), derailFunctionVersion.getVersion())) {
                item.setStatus(derailFunctionVersion.getFunctionStatus());
            }
        });

        // 把跟自己一样版本的进行替换
        List<BusinessFunctions> functionsOld = functions
                .stream()
                .filter(item -> !StringUtils.equals(item.getVersion(), derailFunctionVersion.getVersion()))
                .collect(Collectors.toList());

        functionsOld.stream().forEach(item -> {
            businessList.leaveQueue(JSON.toJSONString(item));
        });
        businessList.addQueue(JSON.toJSONString(derailFunctionVersion));
    }

    /**
     * 获取startId
     *
     * @param functionId
     * @return
     */
    public BusinessFunctions queryStartIdByFunctionId(String functionId) {
        BusinessFunctions functions = (BusinessFunctions) this.businessMap.get(functionId);
        return functions;
    }
}
