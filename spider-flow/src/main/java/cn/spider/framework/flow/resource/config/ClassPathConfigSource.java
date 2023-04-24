/*
 *
 *  * Copyright (c) 2020-2023, Lykan (jiashuomeng@gmail.com).
 *  * <p>
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  * <p>
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  * <p>
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */
package cn.spider.framework.flow.resource.config;

import java.util.ArrayList;
import java.util.List;

import cn.spider.framework.common.utils.SpringUtil;
import cn.spider.framework.db.list.RedisList;
import cn.spider.framework.flow.util.ExceptionUtil;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import cn.spider.framework.flow.exception.ExceptionEnum;
import cn.spider.framework.flow.util.AssertUtil;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * 类路径配置来源
 *
 * @author lykan
 */
public abstract class ClassPathConfigSource implements ConfigSource {

    /**
     * 配置路径- 配置的url前缀
     */
    private final String configName;

    public ClassPathConfigSource(String configName) {
        AssertUtil.notBlank(configName);
        this.configName = configName;
    }

    protected List<Resource> getResourceList() {
        ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        List<Resource> resourceList = new ArrayList<>();
        try {
            // 从redis中获取
            RedisList bpmnFillNames = new RedisList(SpringUtil.getBean(RedisTemplate.class), "bpmn");
            List<String> bpmnFillNameList = bpmnFillNames.queueData();
            for (String bpmnFillName : bpmnFillNameList) {
                Resource[] resources = resolver.getResources(configName + bpmnFillName);
                for (Resource resource : resources) {
                    resourceList.add(resource);
                }
            }
        } catch (Throwable e) {
            throw ExceptionUtil.buildException(e, ExceptionEnum.CONFIGURATION_PARSE_FAILURE, null);
        }
        return resourceList;
    }

    protected List<Resource> getResourceList(String fillName) {
        ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        List<Resource> resourceList = new ArrayList<>();
        try {
            Resource[] resources = resolver.getResources(configName + fillName);
            for (Resource resource : resources) {
                resourceList.add(resource);
            }
        } catch (Throwable e) {
            throw ExceptionUtil.buildException(e, ExceptionEnum.CONFIGURATION_PARSE_FAILURE, null);
        }
        return resourceList;
    }
}
