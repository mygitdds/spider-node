package cn.spider.framework.flow.business;

import cn.spider.framework.container.sdk.data.SelectFunctionResponse;
import cn.spider.framework.container.sdk.interfaces.BusinessService;
import cn.spider.framework.flow.business.data.BusinessFunctions;
import cn.spider.framework.flow.business.data.DerailFunctionVersion;
import cn.spider.framework.flow.business.data.FunctionWeight;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;

/**
 * @BelongsProject: spider-node
 * @BelongsPackage: cn.spider.framework.flow.business
 * @Author: dengdongsheng
 * @CreateTime: 2023-04-12  14:23
 * @Description: 设置业务参数
 * @Version: 1.0
 */
@Component
public class BusinessServiceImpl implements BusinessService {

    @Resource
    private BusinessManager businessManager;


    @Override
    public Future<JsonObject> registerFunction(JsonObject data) {
        String functionId = null;
        try {
            BusinessFunctions businessFunctions = data.mapTo(BusinessFunctions.class);
            functionId = businessManager.registerBusinessFunction(businessFunctions);
        } catch (Exception e) {
            return Future.failedFuture(e);
        }
        return Future.succeededFuture(new JsonObject().put("functionId", functionId));
    }

    @Override
    public Future<JsonObject> selectFunction(JsonObject data) {
        List<Object> business = null;
        try {
            business = businessManager.queryBusinessFunctions();
        } catch (Exception e) {
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
}
