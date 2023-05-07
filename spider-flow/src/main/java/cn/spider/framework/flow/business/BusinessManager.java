package cn.spider.framework.flow.business;

import cn.spider.framework.common.utils.WeightAlgorithm;
import cn.spider.framework.db.list.RedisList;
import cn.spider.framework.db.map.RedisMap;
import cn.spider.framework.db.map.RocksDbMap;
import cn.spider.framework.db.util.RocksdbUtil;
import cn.spider.framework.flow.business.data.BusinessFunctions;
import cn.spider.framework.flow.business.data.DerailFunctionVersion;
import cn.spider.framework.flow.business.data.FunctionWeight;
import cn.spider.framework.flow.business.enums.FunctionStatus;
import cn.spider.framework.flow.exception.BusinessException;
import cn.spider.framework.flow.exception.ExceptionEnum;
import com.alibaba.fastjson.JSON;
import io.vertx.core.WorkerExecutor;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
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

    private final String BUSINESS_LIST = "BUSINESS_LIST";
    private final String BUSINESS_CONFIG = "BUSINESS_CONFIG";

    private RedisTemplate redisTemplate;

    private RedisMap redisMap;

    public BusinessManager(RedisTemplate redisTemplate, WorkerExecutor workerExecutor) {
        this.redisTemplate = redisTemplate;
        this.redisMap = new RedisMap(redisTemplate, BUSINESS_CONFIG, workerExecutor);
    }

    // 注册功能
    public String registerBusinessFunction(BusinessFunctions businessFunctions) {

        if (StringUtils.isEmpty(businessFunctions.getId())) {
            // 给一个uuid
            businessFunctions.setId(UUID.randomUUID().toString());
        }

        RedisList businessList = new RedisList(redisTemplate, businessFunctions.getId());

        List<BusinessFunctions> functions = businessList.queueData()
                .stream()
                .map(item -> JSON.parseObject(item, BusinessFunctions.class))
                .collect(Collectors.toList());

        // 把跟自己一样版本的进行替换
        List<BusinessFunctions> functionsOld = functions
                .stream()
                .filter(item -> !StringUtils.equals(item.getVersion(), businessFunctions.getVersion()))
                .collect(Collectors.toList());

        functionsOld.stream().forEach(item -> {
            businessList.leaveQueue(JSON.toJSONString(item));
        });

        businessList.addQueue(JSON.toJSONString(businessFunctions));
        return businessFunctions.getId();
    }

    // 配置权重
    public void functionWeightConfig(FunctionWeight weight) {
        String key = FUNCTION_WEIGHT_CONFIG_PREFIX + weight.getFunctionId();
        redisMap.put(key, weight);
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
        RedisList businessList = new RedisList(redisTemplate, functionId);

        List<BusinessFunctions> functions = businessList.queueData()
                .stream()
                .map(item -> JSON.parseObject(item, BusinessFunctions.class))
                .collect(Collectors.toList());

        // 没有查询到流程实例id,固然报错
        if (CollectionUtils.isEmpty(functions)) {
            throw new BusinessException(ExceptionEnum.START_ID_NO_FIND.getExceptionCode(), "start id no find!");
        }

        String weightKey = FUNCTION_WEIGHT_CONFIG_PREFIX + functionId;
        FunctionWeight weight = (FunctionWeight) redisMap.get(weightKey);
        List<BusinessFunctions> functionsNew = functions.stream().filter(item -> item.getStatus() == FunctionStatus.OPEN).collect(Collectors.toList());
        if (Objects.isNull(weight)) {
            if (CollectionUtils.isEmpty(functionsNew)) {
                throw new BusinessException(ExceptionEnum.START_ID_NO_FIND.getExceptionCode(), "start id no find!");
            }
            return functionsNew.get(0);
        }

        String version = WeightAlgorithm.getServerByWeight(weight.getWeightConfig());
        // 根据版本获取对应的数据
        List<BusinessFunctions> functionsNewVersion = functionsNew
                .stream()
                .filter(item -> StringUtils.equals(item.getVersion(), version))
                .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(functionsNewVersion)) {
            throw new BusinessException(ExceptionEnum.START_ID_NO_FIND.getExceptionCode(), "start id no find!");
        }

        return functionsNewVersion.get(0);
    }
}
