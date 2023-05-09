package cn.spider.framework.flow.business;

import cn.spider.framework.common.utils.ExceptionMessage;
import cn.spider.framework.container.sdk.data.SelectFunctionResponse;
import cn.spider.framework.container.sdk.interfaces.BusinessService;
import cn.spider.framework.flow.business.data.*;
import com.alibaba.fastjson.JSON;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * @BelongsProject: spider-node
 * @BelongsPackage: cn.spider.framework.flow.business
 * @Author: dengdongsheng
 * @CreateTime: 2023-04-12  14:23
 * @Description: 设置业务参数
 * @Version: 1.0
 */
@Slf4j
@Component
public class BusinessServiceImpl implements BusinessService {

    @Resource
    private BusinessManager businessManager;


    @Override
    public Future<JsonObject> registerFunction(JsonObject data) {
        String functionId = null;
        try {
            BusinessFunctions businessFunctions = data.mapTo(BusinessFunctions.class);
            if(!checkRegisterFunction(businessFunctions)){
                return Future.failedFuture(new Throwable("字段信息不完善"));
            }
            businessFunctions.setCreateTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            functionId = businessManager.registerBusinessFunction(businessFunctions);
        } catch (Exception e) {
            return Future.failedFuture(e);
        }
        return Future.succeededFuture(new JsonObject().put("functionId", functionId));
    }



    // check
    public Boolean checkRegisterFunction(BusinessFunctions businessFunctions) {
        if(StringUtils.isEmpty(businessFunctions.getName())){
            return false;
        }
        if(StringUtils.isEmpty(businessFunctions.getVersion())){
            return false;
        }

        if(StringUtils.isEmpty(businessFunctions.getStartId())){
            return false;
        }

        if(StringUtils.isEmpty(businessFunctions.getBpmnName())){
            return false;
        }
        return true;
    }

    @Override
    public Future<JsonObject> selectFunction(JsonObject data) {
        List<Object> business = null;
        try {
            business = businessManager.queryBusinessFunctions();
        } catch (Exception e) {
            log.error("selectFunction fail {}", ExceptionMessage.getStackTrace(e));
            Future.failedFuture(e);
        }
        SelectFunctionResponse response = new SelectFunctionResponse();
        response.setFunctions(business);
        JsonObject result = JsonObject.mapFrom(response);
        return Future.succeededFuture(result);
    }

    @Override
    public Future<Void> configureDerail(JsonObject data) {
        try {
            DerailFunctionVersion derailFunctionVersion = data.mapTo(DerailFunctionVersion.class);
            businessManager.derailFunctionVersion(derailFunctionVersion);
        } catch (Exception e) {
            return Future.failedFuture(e);
        }
        return Future.succeededFuture();
    }

    @Override
    public Future<Void> configureWeight(JsonObject data) {
        try {
            FunctionWeight functionWeight = data.mapTo(FunctionWeight.class);
            businessManager.functionWeightConfig(functionWeight);
        } catch (Exception e) {
            return Future.failedFuture(e);
        }
        return Future.succeededFuture();
    }

    @Override
    public Future<Void> deleteFunction(JsonObject data) {
        DeleteBusinessFunctionRequest request = data.mapTo(DeleteBusinessFunctionRequest.class);
        businessManager.deleteFunction(request.getFunctionId());
        return Future.succeededFuture();
    }

    @Override
    public Future<Void> stateChange(JsonObject data) {
        try {
            FunctionStateChangeRequest request = data.mapTo(FunctionStateChangeRequest.class);
            businessManager.updateStatus(request.getFunctionId(),request.getStatus());
        } catch (Exception e) {
            return Future.failedFuture(e);
        }
        return Future.succeededFuture();
    }

    @Override
    public Future<Void> deleteAll() {
        try {
            businessManager.deleteAll();
        } catch (Exception e) {
            log.error("deleteAll fail {}", ExceptionMessage.getStackTrace(e));
            return Future.failedFuture(e);
        }
        return Future.succeededFuture();
    }
}
