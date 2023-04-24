package cn.spider.framework.db.util;

import io.vertx.core.Vertx;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.rocksdb.*;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * @BelongsProject: spider-node
 * @BelongsPackage: cn.spider.framework.db.map
 * @Author: dengdongsheng
 * @CreateTime: 2023-04-06  04:55
 * @Description: 继承hashmap
 * @Version: 1.0
 */
@Slf4j
public class RocksdbUtil {

    private static RocksDB rocksDB;
    public static Map<String, ColumnFamilyHandle> columnFamilyHandleMap = new HashMap<>(); //数据库列族(表)集合
    public static int GET_KEYS_BATCH_SIZE = 10000;

    private static RocksdbUtil rocksdbUtil;

    public static RocksdbUtil getInstance() {
        if(Objects.isNull(rocksdbUtil)){
            rocksdbUtil = new RocksdbUtil();
        }
        return rocksdbUtil;
    }

    private RocksdbUtil() {
        try {
            String osName = System.getProperty("os.name");
            log.info("osName:{}", osName);
            String rocksDBPath = null;
            if (osName.toLowerCase().contains("windows")) {
                rocksDBPath = "D:\\RocksDB\\"; // 指定windows系统下RocksDB文件目录
            }else {
                // linux下的路径
                rocksDBPath = "/usr/local/rocksdb/";
            }
            RocksDB.loadLibrary();
            Options options = new Options();
            options.setCreateIfMissing(true); //如果数据库不存在则创建
            List<byte[]> cfArr = RocksDB.listColumnFamilies(options, rocksDBPath); // 初始化所有已存在列族
            List<ColumnFamilyDescriptor> columnFamilyDescriptors = new ArrayList<>(); //ColumnFamilyDescriptor集合
            if (!ObjectUtils.isEmpty(cfArr)) {
                for (byte[] cf : cfArr) {
                    columnFamilyDescriptors.add(new ColumnFamilyDescriptor(cf, new ColumnFamilyOptions()));
                }
            } else {
                columnFamilyDescriptors.add(new ColumnFamilyDescriptor(RocksDB.DEFAULT_COLUMN_FAMILY, new ColumnFamilyOptions()));
            }
            DBOptions dbOptions = new DBOptions();
            dbOptions.setCreateIfMissing(true);
            List<ColumnFamilyHandle> columnFamilyHandles = new ArrayList<>(); //ColumnFamilyHandle集合
            rocksDB = RocksDB.open(dbOptions, rocksDBPath, columnFamilyDescriptors, columnFamilyHandles);
            for (int i = 0; i < columnFamilyDescriptors.size(); i++) {
                ColumnFamilyHandle columnFamilyHandle = columnFamilyHandles.get(i);
                String cfName = new String(columnFamilyDescriptors.get(i).getName(), StandardCharsets.UTF_8);
                columnFamilyHandleMap.put(cfName, columnFamilyHandle);
            }
            log.info("RocksDB init success!! path:{}", rocksDBPath);
            log.info("cfNames:{}", columnFamilyHandleMap.keySet());
        } catch (Exception e) {
            log.error("RocksDB init failure!! error:{}", e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 列族，创建（如果不存在）
     */
    public ColumnFamilyHandle cfAddIfNotExist(String cfName) throws RocksDBException {
        ColumnFamilyHandle columnFamilyHandle;
        if (!columnFamilyHandleMap.containsKey(cfName)) {
            columnFamilyHandle = rocksDB.createColumnFamily(new ColumnFamilyDescriptor(cfName.getBytes(), new ColumnFamilyOptions()));
            columnFamilyHandleMap.put(cfName, columnFamilyHandle);
            log.info("cfAddIfNotExist success!! cfName:{}", cfName);
        } else {
            columnFamilyHandle = columnFamilyHandleMap.get(cfName);
        }
        return columnFamilyHandle;
    }

    /**
     * 列族，删除（如果存在）
     */
    public void cfDeleteIfExist(String cfName) throws RocksDBException {
        if (columnFamilyHandleMap.containsKey(cfName)) {
            rocksDB.dropColumnFamily(columnFamilyHandleMap.get(cfName));
            columnFamilyHandleMap.remove(cfName);
            log.info("cfDeleteIfExist success!! cfName:{}", cfName);
        } else {
            log.warn("cfDeleteIfExist containsKey!! cfName:{}", cfName);
        }
    }

    /**
     * 增
     */
    public void put(String cfName, String key, String value) throws RocksDBException {
        ColumnFamilyHandle columnFamilyHandle = cfAddIfNotExist(cfName); //获取列族Handle
        rocksDB.put(columnFamilyHandle, key.getBytes(), value.getBytes());
    }

    /**
     * 增（批量）
     */
    public void batchPut(String cfName, Map<String, String> map) throws RocksDBException {
        ColumnFamilyHandle columnFamilyHandle = cfAddIfNotExist(cfName); //获取列族Handle
        WriteOptions writeOptions = new WriteOptions();
        WriteBatch writeBatch = new WriteBatch();
        for (Map.Entry<String, String> entry : map.entrySet()) {
            writeBatch.put(columnFamilyHandle, entry.getKey().getBytes(), entry.getValue().getBytes());
        }
        rocksDB.write(writeOptions, writeBatch);
    }

    /**
     * 删
     */
    public void delete(String cfName, String key) throws RocksDBException {
        ColumnFamilyHandle columnFamilyHandle = cfAddIfNotExist(cfName); //获取列族Handle
        rocksDB.delete(columnFamilyHandle, key.getBytes());
    }

    /**
     * 查
     */
    public String get(String cfName, String key) throws RocksDBException {
        String value = null;
        ColumnFamilyHandle columnFamilyHandle = cfAddIfNotExist(cfName); //获取列族Handle
        byte[] bytes = rocksDB.get(columnFamilyHandle, key.getBytes());
        if (!ObjectUtils.isEmpty(bytes)) {
            value = new String(bytes, StandardCharsets.UTF_8);
        }
        return value;
    }

    /**
     * 查（多个键值对）
     */
    public Map<String, String> multiGetAsMap(String cfName, List<String> keys) throws RocksDBException {
        Map<String, String> map = new HashMap<>(keys.size());
        ColumnFamilyHandle columnFamilyHandle = cfAddIfNotExist(cfName); //获取列族Handle
        List<ColumnFamilyHandle> columnFamilyHandles;
        List<byte[]> keyBytes = keys.stream().map(String::getBytes).collect(Collectors.toList());
        columnFamilyHandles = IntStream.range(0, keys.size()).mapToObj(i -> columnFamilyHandle).collect(Collectors.toList());
        List<byte[]> bytes = rocksDB.multiGetAsList(columnFamilyHandles, keyBytes);
        for (int i = 0; i < bytes.size(); i++) {
            byte[] valueBytes = bytes.get(i);
            String value = "";
            if (!ObjectUtils.isEmpty(valueBytes)) {
                value = new String(valueBytes, StandardCharsets.UTF_8);
            }
            map.put(keys.get(i), value);
        }
        return map;
    }

    /**
     * 查（多个值）
     */
    public List<String> multiGetValueAsList(String cfName, List<String> keys) throws RocksDBException {
        List<String> values = new ArrayList<>(keys.size());
        ColumnFamilyHandle columnFamilyHandle = cfAddIfNotExist(cfName); //获取列族Handle
        List<ColumnFamilyHandle> columnFamilyHandles = new ArrayList<>();
        List<byte[]> keyBytes = keys.stream().map(String::getBytes).collect(Collectors.toList());
        for (int i = 0; i < keys.size(); i++) {
            columnFamilyHandles.add(columnFamilyHandle);
        }
        List<byte[]> bytes = rocksDB.multiGetAsList(columnFamilyHandles, keyBytes);
        for (byte[] valueBytes : bytes) {
            String value = "";
            if (!ObjectUtils.isEmpty(valueBytes)) {
                value = new String(valueBytes, StandardCharsets.UTF_8);
            }
            values.add(value);
        }
        return values;
    }

    /**
     * 查（所有键）
     */
    public List<String> getAllKey(String cfName) throws RocksDBException {
        List<String> list = new ArrayList<>();
        ColumnFamilyHandle columnFamilyHandle = cfAddIfNotExist(cfName); //获取列族Handle
        try (RocksIterator rocksIterator = rocksDB.newIterator(columnFamilyHandle)) {
            for (rocksIterator.seekToFirst(); rocksIterator.isValid(); rocksIterator.next()) {
                list.add(new String(rocksIterator.key(), StandardCharsets.UTF_8));
            }
        }
        return list;
    }

    /**
     * 分片查（键）
     */
    public List<String> getKeysFrom(String cfName, String lastKey) throws RocksDBException {
        List<String> list = new ArrayList<>(GET_KEYS_BATCH_SIZE);
        // 获取列族Handle
        ColumnFamilyHandle columnFamilyHandle = cfAddIfNotExist(cfName);
        try (RocksIterator rocksIterator = rocksDB.newIterator(columnFamilyHandle)) {
            if (lastKey != null) {
                rocksIterator.seek(lastKey.getBytes(StandardCharsets.UTF_8));
                rocksIterator.next();
            } else {
                rocksIterator.seekToFirst();
            }
            // 一批次最多 GET_KEYS_BATCH_SIZE 个 key
            while (rocksIterator.isValid() && list.size() < GET_KEYS_BATCH_SIZE) {
                list.add(new String(rocksIterator.key(), StandardCharsets.UTF_8));
                rocksIterator.next();
            }
        }
        return list;
    }

    /**
     * 查（所有键值）
     */
    public Map<String, String> getAll(String cfName) throws RocksDBException {
        Map<String, String> map = new HashMap<>();
        ColumnFamilyHandle columnFamilyHandle = cfAddIfNotExist(cfName); //获取列族Handle
        try (RocksIterator rocksIterator = rocksDB.newIterator(columnFamilyHandle)) {
            for (rocksIterator.seekToFirst(); rocksIterator.isValid(); rocksIterator.next()) {
                map.put(new String(rocksIterator.key(), StandardCharsets.UTF_8), new String(rocksIterator.value(), StandardCharsets.UTF_8));
            }
        }
        return map;
    }

    /**
     * 查总条数
     */
    public int getCount(String cfName) throws RocksDBException {
        int count = 0;
        ColumnFamilyHandle columnFamilyHandle = cfAddIfNotExist(cfName); //获取列族Handle
        try (RocksIterator rocksIterator = rocksDB.newIterator(columnFamilyHandle)) {
            for (rocksIterator.seekToFirst(); rocksIterator.isValid(); rocksIterator.next()) {
                count++;
            }
        }
        return count;
    }
}
