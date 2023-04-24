package cn.spider.framework.flow.funtion;

import cn.spider.framework.annotation.enums.ScopeTypeEnum;
import cn.spider.framework.common.utils.IdWorker;
import cn.spider.framework.common.utils.SnowFlake;
import cn.spider.framework.common.utils.SnowIdDto;
import cn.spider.framework.common.utils.SpringUtil;
import cn.spider.framework.container.sdk.data.StartFlowRequest;
import cn.spider.framework.flow.bus.InScopeData;
import cn.spider.framework.flow.business.BusinessManager;
import cn.spider.framework.flow.business.data.BusinessFunctions;
import cn.spider.framework.flow.engine.example.enums.FlowExampleRole;
import cn.spider.framework.flow.engine.facade.TaskResponse;
import cn.spider.framework.container.sdk.interfaces.FlowService;
import cn.spider.framework.flow.engine.StoryEngine;
import cn.spider.framework.flow.engine.facade.ReqBuilder;
import cn.spider.framework.flow.engine.facade.StoryRequest;
import cn.spider.framework.flow.load.loader.ClassLoaderManager;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Objects;
import java.util.UUID;

/**
 * @BelongsProject: spider-node
 * @BelongsPackage: cn.spider.framework.flow.funtion
 * @Author: dengdongsheng
 * @CreateTime: 2023-03-26  20:07
 * @Description: TODO
 * @Version: 1.0
 */
@Component
public class FlowServiceImpl implements FlowService {

    @Resource
    private StoryEngine storyEngine;

    @Resource
    private BusinessManager businessManager;

    @Resource
    private RedisTemplate redisTemplate;

    @Resource
    private ClassLoaderManager classLoaderManager;

    private final String REQUEST_PREFIX = "REQUEST_PREFIX";

    /**
     * 执行实例核心类
     * @param data
     * @return
     */
    @Override
    public Future<JsonObject> startFlow(JsonObject data) {
        Promise<JsonObject> promise = Promise.promise();
        StartFlowRequest request = data.mapTo(StartFlowRequest.class);
        BusinessFunctions functions  = businessManager.queryStartIdByFunctionId(request.getFunctionId());

        String requestId = buildRequestId()+"";
        StoryRequest<Object> req = ReqBuilder.returnType(Object.class)
                .startId(functions.getStartId())
                .functionName(functions.getName())
                .startFlowRequest(request)
                 // 默认给leader
                .flowExampleRole(FlowExampleRole.LEADER)
                .functionId(functions.getId())
                .request(request.getRequest(classLoaderManager.queryClassLoader(request.getRequestClassType())))
                .build();
        if(Objects.nonNull(request.getVariableKey())){
            InScopeData varScopeData = new InScopeData(ScopeTypeEnum.VARIABLE,requestId);
            varScopeData.put(request.getVariableKey(),varScopeData);
            req.setVarScopeData(varScopeData);
        }
        Future<TaskResponse<Object>> fire = storyEngine.fire(req);
        fire.onSuccess(suss -> {
            TaskResponse<Object> result = suss;
            if(result.isSuccess()){
                JsonObject resultJson = new JsonObject();
                if (!Objects.isNull(result.getResult())) {
                    resultJson =  new JsonObject(JSON.toJSONString(result.getResult()));
                }
                promise.complete(resultJson);
            }else {
                promise.fail(result.getResultException());
            }
        }).onFailure(fail -> {
            promise.fail(fail);
        });
        return promise.future();
    }

    private Long buildRequestId() {
        SnowIdDto snowIdDto = IdWorker.calculateDataIdAndWorkId2(this.redisTemplate, REQUEST_PREFIX);
        SnowFlake snowFlake = new SnowFlake(snowIdDto.getWorkerId(), snowIdDto.getDataCenterId(), snowIdDto.getTimestamp());
        return snowFlake.nextId();
    }


}
