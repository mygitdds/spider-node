package cn.spider.framework.spider.log.es.dao;

import cn.spider.framework.spider.log.es.domain.SpiderFlowElementExampleLog;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

/**
 * @BelongsProject: spider-node
 * @BelongsPackage: cn.spider.framework.spider.log.es.dao
 * @Author: dengdongsheng
 * @CreateTime: 2023-04-18  18:20
 * @Description:
 * @Version: 1.0
 */
@Repository
public interface SpiderFlowElementExampleLogDao extends ElasticsearchRepository<SpiderFlowElementExampleLog,String> {
}
