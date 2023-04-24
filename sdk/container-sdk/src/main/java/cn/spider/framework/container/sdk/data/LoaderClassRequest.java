package cn.spider.framework.container.sdk.data;

/**
 * @BelongsProject: spider-node
 * @BelongsPackage: cn.spider.framework.container.sdk.data
 * @Author: dengdongsheng
 * @CreateTime: 2023-03-28  01:12
 * @Description: 加载 jar包功能的请求参数类
 * @Version: 1.0
 */

public class LoaderClassRequest {
    private String jarName;

    private String classPath;

    public String getJarName() {
        return jarName;
    }

    public void setJarName(String jarName) {
        this.jarName = jarName;
    }

    public String getClassPath() {
        return classPath;
    }

    public void setClassPath(String classPath) {
        this.classPath = classPath;
    }
}