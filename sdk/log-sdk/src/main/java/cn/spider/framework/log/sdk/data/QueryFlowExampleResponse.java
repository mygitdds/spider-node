package cn.spider.framework.log.sdk.data;

import java.util.ArrayList;
import java.util.List;

/**
 * @BelongsProject: spider-node
 * @BelongsPackage: cn.spider.framework.log.sdk.data
 * @Author: dengdongsheng
 * @CreateTime: 2023-04-18  16:50
 * @Description: TODO
 * @Version: 1.0
 */
public class QueryFlowExampleResponse {

    private List<FlowExample> flowExampleList;

    /**
     * 总页数
     */
    private int total;

    public List<FlowExample> getFlowExampleList() {
        return flowExampleList;
    }

    public void setFlowExampleList(List<FlowExample> flowExampleList) {
        this.flowExampleList = flowExampleList;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public void init(){
        this.total = 0;
        this.flowExampleList = new ArrayList<>();
    }
}
