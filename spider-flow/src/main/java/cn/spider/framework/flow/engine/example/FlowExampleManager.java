package cn.spider.framework.flow.engine.example;

import cn.spider.framework.annotation.enums.ScopeTypeEnum;
import cn.spider.framework.common.event.EventManager;
import cn.spider.framework.common.event.EventType;
import cn.spider.framework.common.event.data.EndFlowExampleEventData;
import cn.spider.framework.common.event.data.StartElementExampleData;
import cn.spider.framework.common.event.data.StartFlowExampleEventData;
import cn.spider.framework.common.event.enums.FlowExampleStatus;
import cn.spider.framework.common.utils.*;
import cn.spider.framework.db.map.RocksDbMap;
import cn.spider.framework.db.util.RocksdbUtil;
import cn.spider.framework.flow.bpmn.FlowElement;
import cn.spider.framework.flow.bpmn.ServiceTask;
import cn.spider.framework.flow.bpmn.StartEvent;
import cn.spider.framework.flow.bpmn.enums.BpmnTypeEnum;
import cn.spider.framework.flow.bus.BasicStoryBus;
import cn.spider.framework.flow.bus.ScopeData;
import cn.spider.framework.flow.bus.ScopeDataQuery;
import cn.spider.framework.flow.bus.StoryBus;
import cn.spider.framework.flow.container.component.TaskServiceDef;
import cn.spider.framework.flow.engine.FlowRegister;
import cn.spider.framework.flow.engine.StoryEngineModule;
import cn.spider.framework.flow.engine.example.data.FlowExample;
import cn.spider.framework.flow.engine.example.enums.FlowExampleRole;
import cn.spider.framework.flow.engine.facade.StoryRequest;
import cn.spider.framework.flow.engine.scheduler.SchedulerManager;
import cn.spider.framework.flow.exception.BusinessException;
import cn.spider.framework.flow.exception.ExceptionEnum;
import cn.spider.framework.flow.monitor.MonitorTracking;
import cn.spider.framework.flow.role.Role;
import cn.spider.framework.flow.util.*;
import cn.spider.framework.transaction.sdk.data.RegisterTransactionRequest;
import cn.spider.framework.transaction.sdk.data.RegisterTransactionResponse;
import cn.spider.framework.transaction.sdk.interfaces.TransactionInterface;
import com.google.common.collect.Maps;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @BelongsProject: spider-node
 * @BelongsPackage: cn.spider.framework.flow.engine
 * @Author: dengdongsheng
 * @CreateTime: 2023-03-29  18:02
 * @Description: 管控-流程实例的生命周期
 * @Version: 1.0
 */
public class FlowExampleManager {
    /**
     * StoryEngine 组成模块
     */
    private StoryEngineModule storyEngineModule;


    private TransactionInterface transactionInterface;

    private SchedulerManager schedulerManager;

    private RocksdbUtil rocksdbUtil;


    private RedisTemplate redisEnv;

    private final String FLOW_EXAMPLE_PREFIX = "FLOW_EXAMPLE_PREFIX";

    private EventManager eventManager;

    /**
     * leader的实例map
     */
    private Map<String, FlowExample> leaderFlowExampleMap;

    /**
     * 其他节点的follower
     */
    private Map<String,Map<String,FlowExample>> followerFlowExampleMap;

    public FlowExampleManager(StoryEngineModule storyEngineModule) {
        this.storyEngineModule = storyEngineModule;
        this.leaderFlowExampleMap = Maps.newHashMap();
        this.followerFlowExampleMap = Maps.newHashMap();
    }

    public void init() {
        if (Objects.nonNull(this.schedulerManager)) {
            return;
        }
        this.schedulerManager = SpringUtil.getBean(SchedulerManager.class);
        this.transactionInterface = SpringUtil.getBean(TransactionInterface.class);
        this.rocksdbUtil = SpringUtil.getBean(RocksdbUtil.class);
        this.eventManager = SpringUtil.getBean(EventManager.class);
    }

    public void registerFollowerExample(StoryRequest<Object> storyRequest,String brokerName,String exampleId){
        Role role = storyRequest.getRole();
        ScopeDataQuery scopeDataQuery = getScopeDataQuery(storyRequest);
        FlowRegister flowRegister = getFlowRegister(storyRequest, scopeDataQuery);
        BasicStoryBus storyBus = getStoryBus(storyRequest, flowRegister, role);
        storyRequest.setRequestId(exampleId);
        //构造流程实例
        FlowExample example = FlowExample.builder()
                .exampleId(exampleId)
                .flowRegister(flowRegister)
                .functionId(storyRequest.getFunctionId())
                .functionName(storyRequest.getFunctionName())
                .role(role)
                .flowExampleRole(storyRequest.getFlowExampleRole())
                .storyEngineModule(this.storyEngineModule)
                .storyBus(storyBus)
                .build();
        example.init();

        if(storyRequest.getFlowExampleRole().equals(FlowExampleRole.LEADER)){
            return;
        }
        if(!followerFlowExampleMap.containsKey(brokerName)){
            followerFlowExampleMap.put(brokerName,Maps.newHashMap());
        }
        Map<String,FlowExample> followerMap = followerFlowExampleMap.get(brokerName);

        followerMap.put(exampleId,example);
    }


    /**
     * 注册 流程实例-》返回对应的id
     *
     * @return FlowExample
     */
    public FlowExample registerExample(StoryRequest<Object> storyRequest) {
        Role role = storyRequest.getRole();
        ScopeDataQuery scopeDataQuery = getScopeDataQuery(storyRequest);
        FlowRegister flowRegister = getFlowRegister(storyRequest, scopeDataQuery);
        BasicStoryBus storyBus = getStoryBus(storyRequest, flowRegister, role);
        String exampleId = buildExampleId() + "";
        storyRequest.setRequestId(exampleId);
        FlowExample example = FlowExample.builder()
                .exampleId(exampleId)
                .flowRegister(flowRegister)
                .functionId(storyRequest.getFunctionId())
                .functionName(storyRequest.getFunctionName())
                .role(role)
                .flowExampleRole(storyRequest.getFlowExampleRole())
                .storyEngineModule(this.storyEngineModule)
                .storyBus(storyBus)
                .build();
        example.init();
        // 当是同步信息的情况下，不需要执行后续数据
        if(storyRequest.getFlowExampleRole().equals(FlowExampleRole.FOLLOWER)){
            return example;
        }
        this.leaderFlowExampleMap.put(exampleId, example);
        // 构造 事件体
        StartFlowExampleEventData eventData = StartFlowExampleEventData.builder()
                .functionName(storyRequest.getFunctionName())
                .functionId(storyRequest.getFunctionId())
                .requestId(exampleId)
                .requestClassType(storyRequest.getRequestParam().getRequestClassType())
                .requestParam(JsonObject.mapFrom(storyRequest.getRequest()))
                .startId(flowRegister.getStartEventId())
                .build();
        //发送事件
        eventManager.sendMessage(EventType.START_FLOW_EXAMPLE,eventData);
        runFlowExample(example, true);
        return example;
    }

    /**
     * @param example 需要执行的流程实例
     * @param isNext  表示 是否需要获取下一个节点进行执行（用于重试）
     */
    public void runFlowExample(FlowExample example, Boolean isNext) {
        if (isNext) {
            example.nextElement();
        }
        if (Objects.isNull(example.getFlowElement())) {
            return;
        }

        FlowElement flowElement = example.getFlowElement();
        // 说明流程结束
        if (flowElement.getElementType() == BpmnTypeEnum.END_EVENT) {
            Object result = ResultUtil.buildObject(example.getStoryBus());
            EndFlowExampleEventData endFlowExampleEventData = EndFlowExampleEventData.builder()
                    .status(FlowExampleStatus.SUSS)
                    .requestId(example.getExampleId())
                    .result(JsonObject.mapFrom(result))
                    .build();
            // step1: 判断时间存在事务-存在就提交事务
            if (!example.getTransactionGroupMap().isEmpty()) {
                transactionGroupOperate(example);
                example.getTransactionFuture().onSuccess(suss -> {
                    // step2: 流程实例执行完成
                    example.getPromise().complete();
                    // 移除
                    this.leaderFlowExampleMap.remove(example.getExampleId());
                }).onFailure(fail -> {
                    // 记录-》写入ES
                    example.getPromise().fail(fail);
                });
                eventManager.sendMessage(EventType.END_FLOW_EXAMPLE,endFlowExampleEventData);
                return;
            }
            example.getPromise().complete();
            this.leaderFlowExampleMap.remove(example.getExampleId());
            // 发送该流程实例结束的数据
            eventManager.sendMessage(EventType.END_FLOW_EXAMPLE,endFlowExampleEventData);
            return;
        }
        if (flowElement.getElementType() == BpmnTypeEnum.SERVICE_TASK) {
            // 通知，执行结束
            StartElementExampleData elementExampleData = StartElementExampleData.builder()
                    .flowElementId(flowElement.getId())
                    .flowElementName(flowElement.getName())
                    .functionName(example.getFunctionName())
                    .functionId(example.getFunctionId())
                    .build();


            ServiceTask serviceTask = (ServiceTask) flowElement;
            String transactionGroupId = serviceTask.queryTransactionGroup();
            // 当组的事务id,不为空的情况下，需要先注册事务信息
            if (!StringUtils.isEmpty(transactionGroupId)) {
                Future<JsonObject> transaction = registerTransaction(serviceTask, example);
                transaction.onSuccess(suss -> {
                    JsonObject transactionJson = suss;
                    RegisterTransactionResponse response = transactionJson.mapTo(RegisterTransactionResponse.class);
                    example.getTransactionGroupMap().put(transactionGroupId, response.getGroupId());
                    serviceTask.setXid(response.getGroupId());
                    serviceTask.setBranchId(response.getBranchId());
                    runPlan(example);
                    // 设置 groupId
                    elementExampleData.setTransactionGroupId(response.getGroupId());
                    // 设置该实例的 事务id
                    elementExampleData.setBranchId(response.getBranchId());
                    eventManager.sendMessage(EventType.ELEMENT_START,elementExampleData);
                }).onFailure(fail -> {
                    // 获取事务事务信息失败-（直接）
                    System.out.println("获取事务信息失败" + ExceptionMessage.getStackTrace(fail));
                    example.getPromise().fail(fail);
                });
                return;
            }
            eventManager.sendMessage(EventType.ELEMENT_START,elementExampleData);
            runPlan(example);
        }
    }

    /**
     * 操作- 实例执行完后的事务
     *
     * @param example
     */
    private void transactionGroupOperate(FlowExample example) {
        // 获取事务组map
        Map<String, String> transactionGroupMap = example.getTransactionGroupMap();
        // 校验事务是否结束

        for (String taskGroupId : transactionGroupMap.keySet()) {
            if (example.getTransactionGroupSussIds().contains(taskGroupId) || example.getTransactionGroupFailIds().contains(taskGroupId)) {
                continue;
            }
            String groupId = transactionGroupMap.get(taskGroupId);
            Future<JsonObject> transactionFuture = transaction(example, groupId, taskGroupId);
            transactionFuture.onSuccess(suss -> {
                example.addFailTransactionGroupSuss(taskGroupId);
                checkExampleTransactionIsFinish(example);
                // 校验是否 事务完毕
            }).onFailure(fail -> {
                // 因为已经重试了- 10次，不需要再重试了
                example.addFailTransactionGroupFailId(taskGroupId);
                example.getTransactionPromise().fail(fail);
            });
            break;
        }
    }

    private void checkExampleTransactionIsFinish(FlowExample example) {
        Map<String, String> transactionMap = example.getTransactionGroupMap();
        Boolean isFinish = true;
        for (String taskGroupId : transactionMap.keySet()) {
            if (example.getTransactionGroupFailIds().contains(taskGroupId) || example.getTransactionGroupSussIds().contains(taskGroupId)) {
                continue;
            }
            isFinish = false;
            break;
        }
        if (isFinish) {
            example.getTransactionPromise().complete();
        }
    }

    /**
     * 处理事务组
     *
     * @param example
     * @param groupId
     * @param taskGroupId
     */
    private Future<JsonObject> transaction(FlowExample example, String groupId, String taskGroupId) {
        // 防止- 事务接口为空
        init();
        if (example.getTransactionGroupFailIds().contains(taskGroupId)) {
            Future<JsonObject> future = transactionInterface.rollBack(new JsonObject().put("groupId", groupId));
            return future;
        }
        // 进行提交
        Future<JsonObject> future = transactionInterface.commit(new JsonObject().put("groupId", groupId));
        return future;
    }

    /**
     * 执行具体的节点
     *
     * @param example
     */
    private void runPlan(FlowExample example) {

        example.runExample().onSuccess(suss -> {
            Object result = suss;
            // 使用变量把参数引入到该 区域内
            FlowRegister flowRegisterAsync = example.getFlowRegister();
            StoryBus storyBusAsync = example.getStoryBus();
            FlowElement flowElementAsync = example.getFlowElement();
            if (flowElementAsync.getElementType() == BpmnTypeEnum.SERVICE_TASK) {
                ServiceTask serviceTask = (ServiceTask) flowElementAsync;
                // 获取 TaskServiceDef
                Optional<TaskServiceDef> taskServiceDefOptional = this.storyEngineModule.getTaskContainer().getTaskServiceDef(serviceTask.getTaskComponent(), serviceTask.getTaskService(), example.getRole());
                TaskServiceDef taskServiceDef = taskServiceDefOptional.orElseThrow(() ->
                        ExceptionUtil.buildException(null, ExceptionEnum.TASK_SERVICE_MATCH_ERROR, ExceptionEnum.TASK_SERVICE_MATCH_ERROR.getDesc()
                                + GlobalUtil.format(" service task identity: {}", serviceTask.identity())));
                // 校验返回结果是否属于Mono--- 当前默认不支持
                storyBusAsync.noticeResult(serviceTask, result, taskServiceDef);
                // 通知监控
                flowRegisterAsync.getMonitorTracking().finishTaskTracking(flowElementAsync, null);
            }
            // 执行下一个节点
            try {
                example.removeFailFlowElement(flowElementAsync.getId());
                flowRegisterAsync.predictNextElementNew(example.getCsd(), flowElementAsync);
                runFlowExample(example, true);
            } catch (Exception e) {
                // 存入es-异常信息
                System.out.println("执行失败fail----" + ExceptionMessage.getStackTrace(e));
                example.getPromise().fail(e);
            }
        }).onFailure(fail -> {
            System.out.println("执行失败fail" + ExceptionMessage.getStackTrace(fail));
            FlowElement flowElementAsync = example.getFlowElement();
            // // 后续改造重试-- 当重（）; 改造子流程
            example.addFailFlowElement(flowElementAsync.getId());
            ServiceTask serviceTask = (ServiceTask) flowElementAsync;
            String retryCountString = serviceTask.getRetryCount();
            Integer retryCount = StringUtils.isEmpty(retryCountString) ? 0 : Integer.parseInt(retryCountString);
            if (example.queryFailCount(flowElementAsync.getId()) >= retryCount) {
                // 整体通知失败
                example.getPromise().fail(fail);
                return;
            }
            runFlowExample(example, false);
        });

    }


    public Future<JsonObject> registerTransaction(ServiceTask serviceTask, FlowExample example) {
        // 防止- 事务接口为空
        init();
        RegisterTransactionRequest request = new RegisterTransactionRequest();
        request.setRequestId(example.getRequestId());
        request.setTaskId(serviceTask.getId());
        request.setTaskGroupId(serviceTask.queryTransactionGroup());
        request.setGroupId(example.getTransactionGroupMap().get(serviceTask.queryTransactionGroup()));
        request.setWorkerName(schedulerManager.queryWorkerName(serviceTask.getTaskComponent()));
        return transactionInterface.registerTransaction(JsonObject.mapFrom(request));
    }


    /**
     * 后续改造成雪花算法
     *
     * @return
     */

    private Long buildExampleId() {
        if (Objects.isNull(this.redisEnv)) {
            this.redisEnv = SpringUtil.getBean(RedisTemplate.class);
        }
        SnowIdDto snowIdDto = IdWorker.calculateDataIdAndWorkId2(this.redisEnv, FLOW_EXAMPLE_PREFIX);
        SnowFlake snowFlake = new SnowFlake(snowIdDto.getWorkerId(), snowIdDto.getDataCenterId(), snowIdDto.getTimestamp());
        return snowFlake.nextId();
    }

    private <T> FlowRegister getFlowRegister(StoryRequest<T> storyRequest, ScopeDataQuery scopeDataQuery) {
        String startId = storyRequest.getStartId();
        AssertUtil.notBlank(startId, ExceptionEnum.PARAMS_ERROR, "StartId is not allowed to be empty!");
        Optional<StartEvent> startEventOptional = storyEngineModule.getStartEventContainer().getStartEventById(scopeDataQuery);
        StartEvent startEvent = startEventOptional.orElseThrow(() -> ExceptionUtil
                .buildException(null, ExceptionEnum.PARAMS_ERROR, GlobalUtil.format("StartId did not match a valid StartEvent! startId: {}", startId)));
        return new FlowRegister(startEvent, storyRequest);
    }

    private <T> BasicStoryBus getStoryBus(StoryRequest<T> storyRequest, FlowRegister flowRegister, Role role) {
        String businessId = storyRequest.getBusinessId();
        ScopeData varScopeData = storyRequest.getVarScopeData();
        ScopeData staScopeData = storyRequest.getStaScopeData();
        MonitorTracking monitorTracking = flowRegister.getMonitorTracking();
        return new BasicStoryBus(storyRequest.getTimeout(), storyRequest.getStoryExecutor(),
                storyRequest.getRequestId(), storyRequest.getStartId(), businessId, role, monitorTracking, storyRequest.getRequest(), varScopeData, staScopeData);
    }


    private ScopeDataQuery getScopeDataQuery(StoryRequest<?> storyRequest) {

        return new ScopeDataQuery() {

            @Override
            public <T> T getReqScope() {
                return (T) storyRequest.getRequest();
            }

            @Override
            public <T extends ScopeData> T getStaScope() {
                return (T) storyRequest.getStaScopeData();
            }

            @Override
            public <T extends ScopeData> T getVarScope() {
                return (T) storyRequest.getVarScopeData();
            }

            @Override
            public <T> Optional<T> getResult() {
                throw new BusinessException(ExceptionEnum.BUSINESS_INVOKE_ERROR.getExceptionCode(), "Method is not allowed to be called!");
            }

            @Override
            public String getRequestId() {
                return storyRequest.getRequestId();
            }

            @Override
            public String getStartId() {
                return storyRequest.getStartId();
            }

            @Override
            public Optional<String> getBusinessId() {
                return Optional.ofNullable(storyRequest.getBusinessId()).filter(StringUtils::isNotBlank);
            }

            @Override
            public <T> Optional<T> getReqData(String name) {
                T reqScope = getReqScope();
                if (reqScope == null) {
                    return Optional.empty();
                }
                return PropertyUtil.getProperty(reqScope, name).map(obj -> (T) obj);
            }

            @Override
            public <T> Optional<T> getStaData(String name) {
                T staScope = getStaScope();
                if (staScope == null) {
                    return Optional.empty();
                }
                return PropertyUtil.getProperty(staScope, name).map(obj -> (T) obj);
            }

            @Override
            public <T> Optional<T> getVarData(String name) {
                T varScope = getVarScope();
                if (varScope == null) {
                    return Optional.empty();
                }
                return PropertyUtil.getProperty(varScope, name).map(obj -> (T) obj);
            }

            @Override
            public <T> Optional<T> getData(String expression) {
                if (!ElementParserUtil.isValidDataExpression(expression)) {
                    return Optional.empty();
                }

                String[] expArr = expression.split("\\.", 2);
                Optional<ScopeTypeEnum> ScopeTypeOptional = ScopeTypeEnum.of(expArr[0]);
                if (ScopeTypeOptional.orElse(null) == ScopeTypeEnum.RESULT) {
                    return getResult();
                }

                String key = (expArr.length == 2) ? expArr[1] : null;
                if (StringUtils.isBlank(key)) {
                    return Optional.empty();
                }
                return ScopeTypeOptional.flatMap(scope -> {
                    if (scope == ScopeTypeEnum.REQUEST) {
                        return getReqData(key);
                    } else if (scope == ScopeTypeEnum.STABLE) {
                        return getStaData(key);
                    } else if (scope == ScopeTypeEnum.VARIABLE) {
                        return getVarData(key);
                    }
                    return Optional.empty();
                });
            }

            @Override
            public Optional<String> getTaskProperty() {
                throw new BusinessException(ExceptionEnum.BUSINESS_INVOKE_ERROR.getExceptionCode(), "Method is not allowed to be called!");
            }

            @Override
            public <T> Optional<T> iterDataItem() {
                throw new BusinessException(ExceptionEnum.BUSINESS_INVOKE_ERROR.getExceptionCode(), "Method is not allowed to be called!");
            }

            @Override
            public ReentrantReadWriteLock.ReadLock readLock() {
                throw new BusinessException(ExceptionEnum.BUSINESS_INVOKE_ERROR.getExceptionCode(), "Method is not allowed to be called!");
            }
        };
    }
}
