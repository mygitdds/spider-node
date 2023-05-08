package cn.spider.framework.container.sdk.data;

import java.util.List;

/**
 * @BelongsProject: spider-node
 * @BelongsPackage: cn.spider.framework.container.sdk.data
 * @Author: dengdongsheng
 * @CreateTime: 2023-05-08  22:30
 * @Description: TODO
 * @Version: 1.0
 */
public class SelectFunctionResponse {
    private List<Object> functions;

    public List<Object> getFunctions() {
        return functions;
    }

    public void setFunctions(List<Object> functions) {
        this.functions = functions;
    }
}
