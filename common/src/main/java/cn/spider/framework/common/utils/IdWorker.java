package cn.spider.framework.common.utils;

import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.support.atomic.RedisAtomicLong;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @BelongsProject: spider-node
 * @BelongsPackage: cn.spider.framework.common.utils
 * @Author: dengdongsheng
 * @CreateTime: 2023-04-07  12:53
 * @Description: TODO
 * @Version: 1.0
 */
public class IdWorker {
    private static final Integer DATA_SIZE = 32;
    private static final String[] RADIX_STR = new String[]{"0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n", "o", "p", "q", "r", "s", "t", "u", "v"};
    private static Map<String, Integer> RADIX_MAP = new LinkedHashMap<>();

    private final static String SNOW = "SPIDER_SNOW";

    static {
        for (int i = 0; i < DATA_SIZE; i++) {
            RADIX_MAP.put(RADIX_STR[i], i);
        }
    }

    /**
     * 计算雪花算法参数的新算法
     *
     * @param redisTemplate
     * @param appName
     * @return
     */
    public static SnowIdDto calculateDataIdAndWorkId2(RedisTemplate redisTemplate, String appName) {
        String key = SNOW + appName;
        RedisAtomicLong redisAtomicLong = new RedisAtomicLong(key, redisTemplate.getConnectionFactory());
        long andIncrement = redisAtomicLong.getAndIncrement();
        // result在0~1023之间
        long result = andIncrement % (DATA_SIZE * DATA_SIZE);
        //将result转化为32进制数，个位为worlId，十位为dataId
        String strResult = Integer.toString(Math.toIntExact(result), DATA_SIZE);
        String strResult2 = StringUtils.leftPad(strResult, 2, "0");
        String substring1 = strResult2.substring(0, 1);
        String substring2 = strResult2.substring(1, 2);
        Integer dataId = RADIX_MAP.get(substring1);
        Integer workId = RADIX_MAP.get(substring2);
        SnowIdDto snowIdDto = new SnowIdDto(System.currentTimeMillis(), dataId, workId);
        return snowIdDto;
    }
}
