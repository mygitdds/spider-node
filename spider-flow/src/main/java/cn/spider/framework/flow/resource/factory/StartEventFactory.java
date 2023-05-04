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
package cn.spider.framework.flow.resource.factory;

import cn.spider.framework.flow.bpmn.StartEvent;
import cn.spider.framework.flow.bpmn.SubProcess;
import cn.spider.framework.flow.bpmn.impl.SubProcessImpl;
import cn.spider.framework.flow.bus.ScopeDataQuery;
import cn.spider.framework.flow.component.bpmn.BpmnDiagramRegister;
import cn.spider.framework.flow.component.bpmn.builder.SubProcessLink;
import cn.spider.framework.flow.component.bpmn.link.ProcessLink;
import cn.spider.framework.flow.component.dynamic.ProcessDynamicComponent;
import cn.spider.framework.flow.container.element.BasicStartEventContainer;
import cn.spider.framework.flow.container.element.StartEventContainer;
import cn.spider.framework.flow.enums.ResourceTypeEnum;
import cn.spider.framework.flow.exception.ExceptionEnum;
import cn.spider.framework.flow.resource.config.BpmnConfigResource;
import cn.spider.framework.flow.resource.config.ConfigResource;
import cn.spider.framework.flow.util.*;
import com.alibaba.fastjson.JSON;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * StartEvent 资源创建工厂
 *
 * @author lykan
 */
@Slf4j
public class StartEventFactory extends BasicResourceFactory<StartEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StartEventFactory.class);

    private Map<String, SubProcess> allSubProcessMap;

    private List<StartEvent> resourceList;
    private final ProcessDynamicComponent processDynamicComponent;

    public StartEventFactory(ApplicationContext applicationContext, ProcessDynamicComponent processDynamicComponent) {
        super(applicationContext);
        this.processDynamicComponent = processDynamicComponent;
        this.allSubProcessMap = new HashMap<>();
        this.resourceList = new ArrayList<>();
    }

    /**
     * 类加载完了，就会执行该方法-加载
     */
    @PostConstruct
    public void initResourceList() {
        List<ConfigResource> configResourceList = getConfigResource(ResourceTypeEnum.BPMN);
        List<ConfigResource> diagramConfigResourceList = getConfigResource(ResourceTypeEnum.BPMN_DIAGRAM);
        if (CollectionUtils.isEmpty(configResourceList) && CollectionUtils.isEmpty(diagramConfigResourceList)) {
            return;
        }

        List<BpmnConfigResource> bpmnResourceList =
                configResourceList.stream().map(c -> GlobalUtil.transferNotEmpty(c, BpmnConfigResource.class)).collect(Collectors.toList());

        log.info("加载成功的 bpmnResourceList功能 {}", JSON.toJSONString(bpmnResourceList));
        List<BpmnDiagramRegister> bpmnDiagramResourceList =
                diagramConfigResourceList.stream().map(r -> GlobalUtil.transferNotEmpty(r, BpmnDiagramRegister.class)).collect(Collectors.toList());
        Map<String, SubProcess> aspMap = getAllSubProcessMap(bpmnResourceList, bpmnDiagramResourceList);
        List<StartEvent> list = getStartEvents(aspMap, bpmnResourceList, bpmnDiagramResourceList);
        notDuplicateCheck(list);
        this.resourceList.addAll(list);
        this.allSubProcessMap.putAll(aspMap);
        log.info("加载成功的 bpmn功能 {}", JSON.toJSONString(resourceList));
        log.info("加载成功的 allSubProcessMap功能 {}", JSON.toJSONString(allSubProcessMap));
    }

    /**
     * 动态加载bmpm文件到内核中来
     *
     * @param bpmnName 指定的bpmn名称
     */
    public void dynamicsLoaderBpmn(String bpmnName) {
        List<ConfigResource> configResourceList = getConfigResource(ResourceTypeEnum.APPOINT_BPMN, bpmnName);
        if (CollectionUtils.isEmpty(configResourceList)) {
            return;
        }

        List<BpmnConfigResource> bpmnResourceList =
                configResourceList.stream().map(c -> GlobalUtil.transferNotEmpty(c, BpmnConfigResource.class)).collect(Collectors.toList());

        if (CollectionUtils.isEmpty(bpmnResourceList)) {
            return;
        }

        Map<String, SubProcess> aspMap = getAllSubProcessMap(bpmnResourceList, null);

        List<StartEvent> startEventList = getStartEvents(aspMap, bpmnResourceList, null);
        log.info("动态加载的 StartEvent {}", JSON.toJSONString(startEventList));
        Set<String> startIds = startEventList.stream().map(StartEvent::getId).collect(Collectors.toSet());
        this.resourceList = this.resourceList.stream().filter(item -> !startIds.contains(item.getId())).collect(Collectors.toList());
        this.resourceList.addAll(startEventList);
        this.allSubProcessMap.putAll(aspMap);
        // 刷新bms相关数据
        StartEventContainer startEventContainer = SpringUtil.getBean(BasicStartEventContainer.class);
        startEventContainer.refreshStartEvent(startEventList);
    }


    @Override
    public List<StartEvent> getResourceList() {
        return resourceList;
    }

    public Optional<StartEvent> getDynamicStartEvent(ScopeDataQuery scopeDataQuery) {
        return processDynamicComponent.getStartEvent(allSubProcessMap, scopeDataQuery);
    }

    private static void notDuplicateCheck(List<StartEvent> list) {
        List<StartEvent> duplicateList = list.stream().collect(Collectors.toMap(e -> e, e -> 1, Integer::sum))
                .entrySet().stream().filter(e -> e.getValue() > 1).map(Map.Entry::getKey).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(duplicateList)) {
            return;
        }
        StartEvent es = duplicateList.get(0);
        String fileName = es.getConfig().map(ConfigResource::getConfigName).orElse(null);
        throw ExceptionUtil.buildException(null, ExceptionEnum.ELEMENT_DUPLICATION_ERROR,
                GlobalUtil.format("There are duplicate start event ids defined! identity: {}, fileName: {}", es.identity(), fileName));
    }

    private List<StartEvent> getStartEvents(Map<String, SubProcess> allSubProcess, List<BpmnConfigResource> bpmnResourceList, List<BpmnDiagramRegister> bpmnDiagramResourceList) {
        List<StartEvent> startEventList = bpmnResourceList.stream().flatMap(bpmnResource ->
                bpmnResource.getStartEventList().stream()).peek(startEvent -> ElementParserUtil.fillSubProcess(allSubProcess, startEvent)).collect(Collectors.toList());
        if (!CollectionUtils.isEmpty(bpmnDiagramResourceList)) {
            bpmnDiagramResourceList.forEach(bpmnDiagramRegister -> {
                List<ProcessLink> processLinkList = Lists.newArrayList();
                bpmnDiagramRegister.registerDiagram(processLinkList);
                if (CollectionUtils.isEmpty(processLinkList)) {
                    return;
                }
                LOGGER.info("Load bpmn code register. name: {}", bpmnDiagramRegister.getConfigName());
                List<StartEvent> sList = Lists.newArrayList();
                for (ProcessLink processLink : processLinkList) {
                    if (processLink == null) {
                        continue;
                    }
                    StartEvent startEvent = processLink.getElement();
                    startEvent.setConfig(bpmnDiagramRegister);
                    ElementParserUtil.fillSubProcess(allSubProcess, startEvent);
                    sList.add(startEvent);
                }
                startEventList.addAll(sList);
            });
        }
        return startEventList;
    }

    private Map<String, SubProcess> getAllSubProcessMap(List<BpmnConfigResource> bpmnResourceList, List<BpmnDiagramRegister> bpmnDiagramResourceList) {
        Map<String, SubProcess> allSubProcess = Maps.newHashMap();
        bpmnResourceList.forEach(bpmnResource -> {
            Map<String, SubProcessLink> sbMap = bpmnResource.getSubProcessMap();
            if (MapUtils.isEmpty(sbMap)) {
                return;
            }
            sbMap.forEach((k, v) -> {
                AssertUtil.notTrue(allSubProcess.containsKey(k),
                        ExceptionEnum.ELEMENT_DUPLICATION_ERROR, "There are duplicate SubProcess ids defined! id: {}", k);
                allSubProcess.put(k, v.getSubProcess());
            });
        });
        if (!CollectionUtils.isEmpty(bpmnDiagramResourceList)) {
            bpmnDiagramResourceList.forEach(diagramConfig -> {
                List<SubProcessLink> subLinkBuilderList = Lists.newArrayList();
                diagramConfig.registerSubDiagram(subLinkBuilderList);
                if (CollectionUtils.isEmpty(subLinkBuilderList)) {
                    return;
                }
                subLinkBuilderList.forEach(linkBuilder -> {
                    SubProcessImpl subProcess = linkBuilder.buildSubDiagramBpmnLink(diagramConfig).getElement();
                    AssertUtil.notTrue(allSubProcess.containsKey(subProcess.getId()),
                            ExceptionEnum.ELEMENT_DUPLICATION_ERROR, "There are duplicate SubProcess ids defined! identity: {}", subProcess.identity());
                    allSubProcess.put(subProcess.getId(), subProcess);
                });
            });
        }
        return allSubProcess;
    }
}
