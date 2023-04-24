package cn.spider.framework.linker.client.grpc;

import cn.spider.framework.common.utils.ExceptionMessage;
import cn.spider.framework.linker.client.config.RpcConst;
import cn.spider.framework.linker.client.util.IpUtil;
import cn.spider.framework.linker.sdk.data.LinkerServerRequest;
import cn.spider.framework.linker.sdk.data.TransactionalType;
import cn.spider.framework.proto.grpc.TransferRequest;
import cn.spider.framework.proto.grpc.TransferResponse;
import cn.spider.framework.proto.grpc.VertxTransferServerGrpc;
import com.alibaba.fastjson.JSON;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.grpc.VertxServer;
import io.vertx.grpc.VertxServerBuilder;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.util.ReflectionUtils;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.net.SocketException;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

/**
 * @program: spider-node
 * @description: 接受spider-服务端请求并且执行
 * @author: dds
 * @create: 2023-03-02 21:25
 */

public class TransferServerHandler {

    private Vertx vertx;

    private ApplicationContext applicationContext;

    private Executor taskPool;

    private String localhost;

    private static final ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

    private Map<String, Method> methodMap;

    private final String TRANSACTION_MANAGER = "spiderTransactionManager";

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(TransferServerHandler.class);

    private PlatformTransactionManager platformTransactionManager;

    private TransactionDefinition transactionDefinition;


    public void init(Vertx vertx, ApplicationContext applicationContext, Executor taskPool, PlatformTransactionManager platformTransactionManager, TransactionDefinition transactionDefinition) {
        this.vertx = vertx;
        this.applicationContext = applicationContext;
        this.taskPool = taskPool;
        this.platformTransactionManager = platformTransactionManager;
        this.transactionDefinition = transactionDefinition;
        try {
            this.localhost = IpUtil.buildLocalHost();
        } catch (SocketException e) {
            throw new RuntimeException(e);
        }
        this.methodMap = new ConcurrentHashMap<>();
        run();
    }

    // 跟客户端功能交互
    public void run() {
        VertxServer server = VertxServerBuilder
                .forAddress(vertx, this.localhost, RpcConst.PORT)
                // 添加服务的实现
                .addService(new VertxTransferServerGrpc.TransferServerVertxImplBase() {
                    @Override
                    public Future<TransferResponse> instruct(TransferRequest transferRequest) {
                        // 构造Promise对象
                        Promise<TransferResponse> transferResponsePromise = Promise.promise();
                        // 获取请求中的body
                        String body = transferRequest.getBody();
                        // 给到业务线程池->执行完成后给回调
                        taskPool.execute(() -> {
                            LinkerServerRequest request = JSON.parseObject(body, LinkerServerRequest.class);
                            // 返回的具体对象给
                            Object resultObject = null;
                            // 返回消息
                            String message = "";
                            TransferResponse response = null;
                            try {
                                // 判断执行的类型
                                switch (request.getExecutionType()) {
                                    case FUNCTION:
                                        // 功能执行
                                        resultObject = runFunction(request);
                                        break;
                                    case TRANSACTION:
                                        //事务执行
                                        resultObject = runTransaction(request);
                                        break;
                                }
                                message = "执行成功";
                                response = TransferResponse.newBuilder()
                                        .setCode(1001)
                                        .setMessage(message)
                                        .setData(Objects.nonNull(resultObject) ? JSON.toJSONString(resultObject) : "{}")
                                        .build();
                                log.info("执行的功能信息 {} 返回结果为 {}", body, JSON.toJSONString(response));
                            } catch (Exception e) {
                                System.out.println("异常信息为=" + ExceptionMessage.getStackTrace(e));
                                log.error("执行的功能信息 {} 异常信息为 {}", body, ExceptionMessage.getStackTrace(e));
                                // 异常信息给到返回值当中
                                message = ExceptionMessage.getStackTrace(e);
                                response = TransferResponse.newBuilder()
                                        .setCode(1002)
                                        .setMessage(message)
                                        .build();
                            }
                            // 返回-spider-server
                            transferResponsePromise.complete(response);
                        });
                        return transferResponsePromise.future();
                    }
                })
                .build();
        // start the server
        server.start(ar -> {
            if (ar.failed()) {
                System.out.println("执行失败");
            } else {
                // 发布成功
                System.out.println("发布成功");
            }
        });
    }

    /**
     * 功能执行
     *
     * @param request
     * @return object
     */
    public Object runFunction(LinkerServerRequest request) {

        Object target = applicationContext.getBean(request.getFunctionRequest().getComponentName());

        // 获取参数
        Map<String, Object> paramMap = request.getFunctionRequest().getParam();
        String methodKey = request.getFunctionRequest().getServiceName() + request.getFunctionRequest().getMethodName();
        Method methodNew = null;
        if (methodMap.containsKey(methodKey)) {
            methodNew = methodMap.get(methodKey);
        } else {
            // 方法
            Method[] methods = target.getClass().getMethods();
            // 获取要执行的方法
            for (Method method : methods) {
                if (StringUtils.equals(method.getName(), request.getFunctionRequest().getMethodName())) {
                    methodNew = method;
                    break;
                }
            }
            // 方法缓存
            methodMap.put(methodKey, methodNew);
        }
        // 获取执行参数
        Object[] params = buildParam(paramMap, methodNew);
        // 获取事务的xid
        String xid = request.getFunctionRequest().getXid();
        // 获取事务的 branchId
        String branchId = request.getFunctionRequest().getBranchId();
        /**
         * 当需要事务的情况下，使用编程事务
         */
        if (StringUtils.isNotEmpty(xid) && StringUtils.isNotEmpty(branchId)) {
            TransactionStatus transaction = platformTransactionManager.getTransaction(transactionDefinition);
            try {
                Object result = ReflectionUtils.invokeMethod(methodNew, target, params);
                platformTransactionManager.commit(transaction);
                return result;
            } catch (Exception e) {
                platformTransactionManager.rollback(transaction);
                throw new RuntimeException(e);
            }
        }
        // 具体执行
        return ReflectionUtils.invokeMethod(methodNew, target, params);

    }

    /**
     * 事务操作
     *
     * @param request
     * @return object
     * @throws NoSuchMethodException
     */
    public Object runTransaction(LinkerServerRequest request) {
        Object target = applicationContext.getBean(TRANSACTION_MANAGER);
        Method method = null;

        String methodName = request.getTransactionalRequest().getTransactionalType().equals(TransactionalType.ROLLBACK) ? "rollBack" : "commit";
        if (methodMap.containsKey(methodName)) {
            method = methodMap.get(methodName);
        } else {
            Method[] methods = target.getClass().getMethods();

            for (Method methodNew : methods) {
                if (StringUtils.equals(methodNew.getName(), methodName)) {
                    method = methodNew;
                    break;
                }
            }
            methodMap.put(methodName, method);
        }
        String transactionId = request.getTransactionalRequest().getTransactionId();
        String brushId = request.getTransactionalRequest().getBranchId();
        Object[] params = new Object[2];
        params[0] = transactionId;
        params[1] = brushId;
        return ReflectionUtils.invokeMethod(method, target, params);
    }

    public Object[] buildParam(Map<String, Object> paramMap, Method method) {
        Object[] params = new Object[paramMap.keySet().size()];
        Parameter[] parameters = method.getParameters();
        String[] parameterNames = parameterNameDiscoverer.getParameterNames(method);
        for (int i = 0; i < params.length; i++) {
            Parameter parameter = parameters[i];
            String parameterName = parameterNames[i];
            Object requestParam = paramMap.get(parameterName);
            log.info("获取到的参数名称为 {} 传入的值为 {}", parameterName, JSON.toJSONString(requestParam));
            Class paramType = parameter.getType();
            params[i] = JSON.parseObject(JSON.toJSONString(requestParam), paramType);
        }
        return params;
    }


}
