package cn.spider.framework.flow.engine.example.data;

import cn.spider.framework.common.utils.ExceptionMessage;
import cn.spider.framework.db.map.RocksDbMap;
import cn.spider.framework.db.util.RocksdbUtil;
import cn.spider.framework.flow.bpmn.EndEvent;
import cn.spider.framework.flow.bpmn.FlowElement;
import cn.spider.framework.flow.bpmn.ServiceTask;
import cn.spider.framework.flow.bpmn.StartEvent;
import cn.spider.framework.flow.bpmn.enums.BpmnTypeEnum;
import cn.spider.framework.flow.bus.BasicStoryBus;
import cn.spider.framework.flow.bus.ContextStoryBus;
import cn.spider.framework.flow.bus.StoryBus;
import cn.spider.framework.flow.container.component.TaskServiceDef;
import cn.spider.framework.flow.engine.FlowRegister;
import cn.spider.framework.flow.engine.StoryEngineModule;
import cn.spider.framework.flow.engine.example.enums.FlowExampleRole;
import cn.spider.framework.flow.engine.thread.EndTaskPedometer;
import cn.spider.framework.flow.engine.thread.FragmentTask;
import cn.spider.framework.flow.exception.ExceptionEnum;
import cn.spider.framework.flow.exception.KstryException;
import cn.spider.framework.flow.role.Role;
import cn.spider.framework.flow.util.ExceptionUtil;
import cn.spider.framework.flow.util.GlobalUtil;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * @BelongsProject: spider-node
 * @BelongsPackage: cn.spider.framework.flow.engine.example.data
 * @Author: dengdongsheng
 * @CreateTime: 2023-03-29  18:56
 * @Description: TODO
 * @Version: 1.0
 */
@Slf4j
@Builder
public class FlowExample {
    /**
     * 实例id
     */
    private String exampleId;

    /**
     * 流程注册对象
     */
    private FlowRegister flowRegister;

    private String functionName;

    private String functionId;
    /**
     * 参数存储对象
     */
    private BasicStoryBus storyBus;
    /**
     * 角色
     */
    private Role role;
    /**
     * 获取执行完成通知对象
     */
    private Future<Void> future;


    private Promise<Void> promise;

    /**
     * 用于子流程
     */
    private Promise<Void> parentPromise;

    // 当前执行
    private ContextStoryBus csd;

    // 当前执行的-flowElement
    private FlowElement flowElement;

    //当前的end节点
    protected EndTaskPedometer endTaskPedometer;

    /**
     * StoryEngine 组成模块
     */
    private StoryEngineModule storyEngineModule;

    /**
     * 事务的映射
     */
    private Map<String, String> transactionGroupMap;

    private Map<String, Integer> transactionFailCountMap;


    private Map<String, Integer> flowElementFailCountMap;

    private Promise<Void> transactionPromise;

    private Future<Void> transactionFuture;

    private FlowExampleRole flowExampleRole;

    private String gatewayAddr;


    public FlowExampleRole getFlowExampleRole() {
        return flowExampleRole;
    }

    public void setFlowExampleRole(FlowExampleRole flowExampleRole) {
        this.flowExampleRole = flowExampleRole;
    }

    public String getFunctionName() {
        return functionName;
    }

    public void setFunctionName(String functionName) {
        this.functionName = functionName;
    }

    public String getFunctionId() {
        return functionId;
    }

    public void setFunctionId(String functionId) {
        this.functionId = functionId;
    }

    public Set<String> getTransactionGroupFailIds() {
        return transactionGroupFailIds;
    }

    public void setTransactionGroupFailIds(Set<String> transactionGroupFailIds) {
        this.transactionGroupFailIds = transactionGroupFailIds;
    }

    public Promise<Void> getTransactionPromise() {
        return transactionPromise;
    }

    public void setTransactionPromise(Promise<Void> transactionPromise) {
        this.transactionPromise = transactionPromise;
    }

    public Future<Void> getTransactionFuture() {
        return transactionFuture;
    }

    public void setTransactionFuture(Future<Void> transactionFuture) {
        this.transactionFuture = transactionFuture;
    }

    /**
     * 事务-异常事务组id
     */
    private Set<String> transactionGroupFailIds;

    /**
     * 事务-异常事务组id
     */
    private Set<String> transactionGroupSussIds;

    private String requestId;

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public StoryEngineModule getStoryEngineModule() {
        return storyEngineModule;
    }

    public void setStoryEngineModule(StoryEngineModule storyEngineModule) {
        this.storyEngineModule = storyEngineModule;
    }

    public Set<String> getTransactionGroupSussIds() {
        return transactionGroupSussIds;
    }

    public void setTransactionGroupSussIds(Set<String> transactionGroupSussIds) {
        this.transactionGroupSussIds = transactionGroupSussIds;
    }

    public FlowElement getFlowElement() {
        return flowElement;
    }

    public void setFlowElement(FlowElement flowElement) {
        this.flowElement = flowElement;
    }

    public Map<String, String> getTransactionGroupMap() {
        return transactionGroupMap;
    }

    public void setTransactionGroupMap(Map<String, String> transactionGroupMap) {
        this.transactionGroupMap = transactionGroupMap;
    }

    public void init() {
        this.promise = Promise.promise();
        this.future = this.promise.future();
        StartEvent startEvent = GlobalUtil.transferNotEmpty(flowRegister.getStartElement(), StartEvent.class);
        EndEvent endEvent = GlobalUtil.notNull(startEvent.getEndEvent());
        this.endTaskPedometer = new EndTaskPedometer(startEvent.getId(), endEvent.comingList(), startEvent.getId());
        this.transactionGroupMap = new HashMap<>();
        this.transactionGroupFailIds = new HashSet<>();
        this.flowElementFailCountMap = new HashMap<>();
        this.transactionPromise = Promise.promise();
        this.transactionFuture = this.transactionPromise.future();
        this.transactionGroupSussIds = new HashSet<>();
        this.transactionFailCountMap = new HashMap<>();
    }


    public Future<Void> getFuture() {
        return future;
    }

    public void setFuture(Future<Void> future) {
        this.future = future;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public String getExampleId() {
        return exampleId;
    }

    public void setExampleId(String exampleId) {
        this.exampleId = exampleId;
    }

    public FlowRegister getFlowRegister() {
        return flowRegister;
    }

    public void setFlowRegister(FlowRegister flowRegister) {
        this.flowRegister = flowRegister;
    }

    public BasicStoryBus getStoryBus() {
        return storyBus;
    }

    public void setStoryBus(BasicStoryBus storyBus) {
        this.storyBus = storyBus;
    }

    public Promise<Void> getPromise() {
        return promise;
    }

    public void setPromise(Promise<Void> promise) {
        this.promise = promise;
    }

    public ContextStoryBus getCsd() {
        return csd;
    }

    public void setCsd(ContextStoryBus csd) {
        this.csd = csd;
    }

    public Future<Object> runExample() {
        // 判断当前节点，是end,如果是，直接结束
        FragmentTask fragmentTask = new FragmentTask(storyEngineModule, this.getFlowRegister(), this.getRole(), this.getStoryBus());
        fragmentTask.init(this.flowElement, this);
        return fragmentTask.runPlan();
    }

    public void nextElement() {
        this.csd = new ContextStoryBus(this.storyBus);
        this.csd.setEndTaskPedometer(this.endTaskPedometer);
        Optional<FlowElement> next = this.flowRegister.nextElement(this.csd);
        this.flowElement = next.isPresent() ? next.get() : null;
    }

    public void addFailTransactionGroupFailId(String transactionGroupId) {
        this.transactionGroupFailIds.add(transactionGroupId);
    }

    public void addTransactionFail(String taskGroupId) {
        Integer groupIdCount = this.transactionFailCountMap.containsKey(taskGroupId) ? this.transactionFailCountMap.get(taskGroupId) : 0;
        this.transactionFailCountMap.put(taskGroupId, groupIdCount + 1);
    }

    public Integer countTransactionFail(String taskGroupId) {
        return this.transactionFailCountMap.containsKey(taskGroupId) ? this.transactionFailCountMap.get(taskGroupId) : 0;
    }

    public void removeTransactionFail(String taskGroupId) {
        this.transactionFailCountMap.remove(taskGroupId);
    }

    public void addFailTransactionGroupSuss(String transactionGroupId) {
        this.transactionGroupSussIds.add(transactionGroupId);
    }

    public void addFailFlowElement(String id) {
        Integer failCount = this.flowElementFailCountMap.containsKey(id) ? flowElementFailCountMap.get(id) : 0;
        this.flowElementFailCountMap.put(id, failCount + 1);
    }

    public void removeFailFlowElement(String id) {
        this.flowElementFailCountMap.remove(id);
    }

    public Integer queryFailCount(String id) {
        return this.flowElementFailCountMap.containsKey(id) ? this.flowElementFailCountMap.get(id) : 0;
    }


}
