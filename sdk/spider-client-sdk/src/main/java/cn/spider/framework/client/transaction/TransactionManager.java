package cn.spider.framework.client.transaction;
import cn.spider.framework.transaction.sdk.core.exception.TransactionException;
import cn.spider.framework.transaction.sdk.datasource.util.JdbcUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import javax.annotation.PostConstruct;
import javax.annotation.Resource;

/**
 * @program: spider-node
 * @description: 事务管理器
 * @author: dds
 * @create: 2023-03-06 13:55
 */
public class TransactionManager {

    // 获取 dataSource.url
    private String url;

    private SpiderTransactionOperation operation;

    private String resourceId;

    public TransactionManager(String url,SpiderTransactionOperation operation){
        this.url = url;
        this.operation = operation;
    }

    @PostConstruct
    public void init(){
        this.resourceId = JdbcUtils.buildResourceId(url);
    }

    /**
     * 提交事务
     * @param xid,brushId
     * @throws TransactionException
     */
    public void commit(String xid,String brushId) throws TransactionException {
        TransactionOperateModel operateModel = new TransactionOperateModel();
        operateModel.setXid(xid);
        operateModel.setBranchId(brushId);
        operateModel.setResourceId(resourceId);
        operation.commit(operateModel);
    }

    /**
     * 回滚事务
     * @param xid
     * @throws TransactionException
     */
    public void rollBack(String xid,String brushId) throws TransactionException {
        TransactionOperateModel operateModel = new TransactionOperateModel();
        operateModel.setXid(xid);
        operateModel.setBranchId(brushId);
        operateModel.setResourceId(resourceId);
        operation.rollBack(operateModel);
    }
}
