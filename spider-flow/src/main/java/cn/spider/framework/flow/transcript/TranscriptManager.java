package cn.spider.framework.flow.transcript;

import cn.spider.framework.common.config.Constant;
import cn.spider.framework.common.event.EventManager;
import cn.spider.framework.common.event.EventType;
import cn.spider.framework.common.event.data.LeaderReplaceData;
import cn.spider.framework.common.utils.BrokerInfoUtil;
import cn.spider.framework.common.utils.ExceptionMessage;
import cn.spider.framework.flow.timer.SpiderTimer;
import cn.spider.framework.transaction.sdk.data.NotifyTranscriptsChange;
import cn.spider.framework.transaction.sdk.interfaces.TransactionInterface;
import com.alibaba.fastjson.JSON;
import com.google.common.collect.Sets;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import java.util.Objects;
import java.util.Set;

/**
 * @BelongsProject: spider-node
 * @BelongsPackage: cn.spider.framework.flow.transcript
 * @Author: dengdongsheng
 * @CreateTime: 2023-04-25  14:14
 * @Description: 副本管理
 * @Version: 1.0
 */
@Slf4j
public class TranscriptManager {

    private Set<String> transcripts;

    private RedisTemplate redisTemplate;

    private EventManager eventManager;

    private String thisBrokerName;

    private Vertx vertx;

    private TransactionInterface transactionInterface;

    private SpiderTimer timer;

    public TranscriptManager(RedisTemplate redisTemplate,
                             EventManager eventManager,
                             Vertx vertx,
                             TransactionInterface transactionInterface,
                             SpiderTimer timer) {
        this.transcripts = Sets.newHashSet();
        this.redisTemplate = redisTemplate;
        this.eventManager = eventManager;
        this.vertx = vertx;
        this.thisBrokerName = BrokerInfoUtil.queryBrokerName(this.vertx);
        this.transactionInterface = transactionInterface;
        this.timer = timer;
    }

    public Boolean checkIsTranscript(String brokerName) {
        return transcripts.contains(brokerName);
    }

    public void replace(Set<String> transcripts) {
        this.transcripts = Objects.isNull(transcripts) ? Sets.newHashSet() : transcripts;
        NotifyTranscriptsChange change = new NotifyTranscriptsChange(this.transcripts);
        Future<Void> future = this.transactionInterface.replaceTranscripts(JsonObject.mapFrom(change));
        future.onFailure(fail->{
            log.error("brokerName {} 同步副本信息 {} 失败 {}",this.thisBrokerName, JSON.toJSONString(this.transcripts), ExceptionMessage.getStackTrace(fail));
        });
    }

    /**
     * 选举某个副本的leader
     * @param brokerName
     */
    public void election(String brokerName) {

        if(!checkIsTranscript(brokerName)){
            return;
        }
        // step1: 竞争锁
        String key = Constant.TRANSCRIPT_PREFIX + brokerName;

        boolean result = this.redisTemplate.opsForValue()
                .setIfAbsent(key, brokerName);

        if (!result) {
            return;
        }
        // step2: 告知本节点已经替代了某节点
        LeaderReplaceData replaceData = LeaderReplaceData.builder()
                .newLeaderTransaction(thisBrokerName)
                .oldLeaderTransaction(brokerName)
                .build();
        // 发事件通知 本节点已经替代了，这个leader
        eventManager.sendMessage(EventType.LEADER_REPLACE_CHANGE, replaceData);
        // step3: 把该节点中的副本转正-注册延迟，一秒后执行
        this.timer.becomeTimer(brokerName);
        // step4 1分钟后删除对应的数据
        this.timer.deleteElectionKey(key);
    }

}
