package com.rui.common.redis.optimization;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rui.common.redis.pool.RedisPoolConfig;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Redis性能优化器
 * 提供缓存预热、批量操作、压缩等性能优化功能
 *
 * @author rui
 */
@Slf4j
@Component
public class RedisPerformanceOptimizer {

    @Autowired
    private RedisPoolConfig poolConfig;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired(required = false)
    private MeterRegistry meterRegistry;

    @Autowired
    private ObjectMapper objectMapper;

    private ExecutorService batchExecutor;
    private ScheduledExecutorService scheduledExecutor;
    
    private final Map<String, Object> serializationCache = new ConcurrentHashMap<>();
    private final AtomicLong cacheHits = new AtomicLong(0);
    private final AtomicLong cacheMisses = new AtomicLong(0);
    private final AtomicLong compressionSavings = new AtomicLong(0);
    
    // Lua脚本缓存
    private final Map<String, DefaultRedisScript<Object>> scriptCache = new ConcurrentHashMap<>();
    
    // 批量操作队列
    private final BlockingQueue<BatchOperation> batchQueue = new LinkedBlockingQueue<>();
    
    /**
     * 初始化性能优化器
     */
    @PostConstruct
    public void initialize() {
        if (!poolConfig.isEnabled() || !poolConfig.getPerformance().isSerializationCache()) {
            log.info("Redis性能优化器已禁用");
            return;
        }

        log.info("初始化Redis性能优化器");
        
        // 初始化线程池
        initializeExecutors();
        
        // 启动批量处理
        if (poolConfig.getPerformance().isPipelining()) {
            startBatchProcessing();
        }
        
        // 预加载Lua脚本
        preloadLuaScripts();
        
        log.info("Redis性能优化器初始化完成");
    }

    /**
     * 初始化线程池
     */
    private void initializeExecutors() {
        batchExecutor = Executors.newFixedThreadPool(4, r -> {
            Thread t = new Thread(r, "redis-batch-executor");
            t.setDaemon(true);
            return t;
        });
        
        scheduledExecutor = Executors.newScheduledThreadPool(2, r -> {
            Thread t = new Thread(r, "redis-scheduled-executor");
            t.setDaemon(true);
            return t;
        });
    }

    /**
     * 启动批量处理
     */
    private void startBatchProcessing() {
        batchExecutor.submit(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    List<BatchOperation> operations = new ArrayList<>();
                    
                    // 收集批量操作
                    BatchOperation first = batchQueue.take();
                    operations.add(first);
                    
                    batchQueue.drainTo(operations, poolConfig.getPerformance().getPipelineBatchSize() - 1);
                    
                    // 执行批量操作
                    executeBatchOperations(operations);
                    
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    log.error("批量操作处理失败", e);
                }
            }
        });
    }

    /**
     * 预加载Lua脚本
     */
    private void preloadLuaScripts() {
        // 批量设置脚本
        String batchSetScript = 
            "for i = 1, #KEYS do " +
            "  redis.call('SET', KEYS[i], ARGV[i]) " +
            "end " +
            "return #KEYS";
        scriptCache.put("batchSet", new DefaultRedisScript<>(batchSetScript, Object.class));
        
        // 批量获取脚本
        String batchGetScript = 
            "local result = {} " +
            "for i = 1, #KEYS do " +
            "  result[i] = redis.call('GET', KEYS[i]) " +
            "end " +
            "return result";
        scriptCache.put("batchGet", new DefaultRedisScript<>(batchGetScript, Object.class));
        
        // 原子计数器脚本
        String atomicCounterScript = 
            "local current = redis.call('GET', KEYS[1]) " +
            "if current == false then " +
            "  current = 0 " +
            "end " +
            "local new_value = current + ARGV[1] " +
            "redis.call('SET', KEYS[1], new_value) " +
            "if ARGV[2] then " +
            "  redis.call('EXPIRE', KEYS[1], ARGV[2]) " +
            "end " +
            "return new_value";
        scriptCache.put("atomicCounter", new DefaultRedisScript<>(atomicCounterScript, Object.class));
        
        log.info("Lua脚本预加载完成，共{}个脚本", scriptCache.size());
    }

    /**
     * 压缩数据
     */
    public byte[] compress(Object data) {
        if (!poolConfig.getPerformance().isCompression()) {
            return serializeObject(data);
        }
        
        try {
            byte[] originalData = serializeObject(data);
            
            if (originalData.length < poolConfig.getPerformance().getCompressionThreshold()) {
                return originalData;
            }
            
            Timer.Sample sample = Timer.start();
            
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (GZIPOutputStream gzipOut = new GZIPOutputStream(baos)) {
                gzipOut.write(originalData);
            }
            
            byte[] compressedData = baos.toByteArray();
            
            if (meterRegistry != null) {
                sample.stop(Timer.builder("redis.compression.time")
                        .description("Redis compression time")
                        .register(meterRegistry));
            }
            
            long savings = originalData.length - compressedData.length;
            compressionSavings.addAndGet(savings);
            
            log.debug("数据压缩完成，原始大小: {}, 压缩后大小: {}, 节省: {}", 
                    originalData.length, compressedData.length, savings);
            
            return compressedData;
            
        } catch (IOException e) {
            log.error("数据压缩失败", e);
            return serializeObject(data);
        }
    }

    /**
     * 解压数据
     */
    public Object decompress(byte[] compressedData) {
        if (!poolConfig.getPerformance().isCompression()) {
            return deserializeObject(compressedData);
        }
        
        try {
            Timer.Sample sample = Timer.start();
            
            ByteArrayInputStream bais = new ByteArrayInputStream(compressedData);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            
            try (GZIPInputStream gzipIn = new GZIPInputStream(bais)) {
                byte[] buffer = new byte[1024];
                int len;
                while ((len = gzipIn.read(buffer)) != -1) {
                    baos.write(buffer, 0, len);
                }
            }
            
            if (meterRegistry != null) {
                sample.stop(Timer.builder("redis.decompression.time")
                        .description("Redis decompression time")
                        .register(meterRegistry));
            }
            
            return deserializeObject(baos.toByteArray());
            
        } catch (IOException e) {
            log.error("数据解压失败", e);
            return deserializeObject(compressedData);
        }
    }

    /**
     * 序列化对象（带缓存）
     */
    private byte[] serializeObject(Object obj) {
        if (!poolConfig.getPerformance().isSerializationCache()) {
            return serializeDirectly(obj);
        }
        
        String cacheKey = obj.getClass().getName() + ":" + obj.hashCode();
        
        byte[] cached = (byte[]) serializationCache.get(cacheKey);
        if (cached != null) {
            cacheHits.incrementAndGet();
            return cached;
        }
        
        cacheMisses.incrementAndGet();
        byte[] serialized = serializeDirectly(obj);
        
        if (serializationCache.size() < poolConfig.getPerformance().getSerializationCacheSize()) {
            serializationCache.put(cacheKey, serialized);
        }
        
        return serialized;
    }

    /**
     * 直接序列化
     */
    private byte[] serializeDirectly(Object obj) {
        try {
            return objectMapper.writeValueAsBytes(obj);
        } catch (Exception e) {
            log.error("对象序列化失败", e);
            throw new RuntimeException("序列化失败", e);
        }
    }

    /**
     * 反序列化对象
     */
    private Object deserializeObject(byte[] data) {
        try {
            return objectMapper.readValue(data, Object.class);
        } catch (Exception e) {
            log.error("对象反序列化失败", e);
            throw new RuntimeException("反序列化失败", e);
        }
    }

    /**
     * 批量设置
     */
    public void batchSet(Map<String, Object> keyValues, Duration expiration) {
        if (!poolConfig.getPerformance().isPipelining()) {
            // 普通批量设置
            keyValues.forEach((key, value) -> {
                if (expiration != null) {
                    redisTemplate.opsForValue().set(key, value, expiration);
                } else {
                    redisTemplate.opsForValue().set(key, value);
                }
            });
            return;
        }
        
        // 使用管道批量设置
        BatchOperation operation = new BatchOperation(BatchOperationType.SET, keyValues, expiration);
        try {
            batchQueue.offer(operation, 1, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("批量设置操作被中断");
        }
    }

    /**
     * 批量获取
     */
    public Map<String, Object> batchGet(Collection<String> keys) {
        if (!poolConfig.getPerformance().isPipelining()) {
            // 普通批量获取
            List<Object> values = redisTemplate.opsForValue().multiGet(keys);
            Map<String, Object> result = new HashMap<>();
            int index = 0;
            for (String key : keys) {
                if (index < values.size()) {
                    result.put(key, values.get(index));
                }
                index++;
            }
            return result;
        }
        
        // 使用Lua脚本批量获取
        List<String> keyList = new ArrayList<>(keys);
        DefaultRedisScript<Object> script = scriptCache.get("batchGet");
        
        @SuppressWarnings("unchecked")
        List<Object> values = (List<Object>) redisTemplate.execute(script, keyList);
        
        Map<String, Object> result = new HashMap<>();
        for (int i = 0; i < keyList.size() && i < values.size(); i++) {
            result.put(keyList.get(i), values.get(i));
        }
        
        return result;
    }

    /**
     * 原子计数器
     */
    public Long atomicIncrement(String key, long delta, Duration expiration) {
        DefaultRedisScript<Object> script = scriptCache.get("atomicCounter");
        
        List<String> keys = Collections.singletonList(key);
        List<Object> args = new ArrayList<>();
        args.add(String.valueOf(delta));
        if (expiration != null) {
            args.add(String.valueOf(expiration.getSeconds()));
        }
        
        Object result = redisTemplate.execute(script, keys, args.toArray());
        return result != null ? Long.valueOf(result.toString()) : 0L;
    }

    /**
     * 执行批量操作
     */
    private void executeBatchOperations(List<BatchOperation> operations) {
        if (operations.isEmpty()) {
            return;
        }
        
        Timer.Sample sample = Timer.start();
        
        try {
            redisTemplate.executePipelined((RedisConnection connection) -> {
                for (BatchOperation operation : operations) {
                    switch (operation.getType()) {
                        case SET:
                            operation.getKeyValues().forEach((key, value) -> {
                                byte[] keyBytes = key.getBytes();
                                byte[] valueBytes = compress(value);
                                
                                if (operation.getExpiration() != null) {
                                    connection.setEx(keyBytes, operation.getExpiration().getSeconds(), valueBytes);
                                } else {
                                    connection.set(keyBytes, valueBytes);
                                }
                            });
                            break;
                        case DELETE:
                            operation.getKeyValues().keySet().forEach(key -> {
                                connection.del(key.getBytes());
                            });
                            break;
                        default:
                            log.warn("不支持的批量操作类型: {}", operation.getType());
                    }
                }
                return null;
            });
            
            if (meterRegistry != null) {
                sample.stop(Timer.builder("redis.batch.operations")
                        .description("Redis batch operations time")
                        .register(meterRegistry));
                
                meterRegistry.counter("redis.batch.operations.count").increment(operations.size());
            }
            
            log.debug("批量操作执行完成，操作数: {}", operations.size());
            
        } catch (Exception e) {
            log.error("批量操作执行失败", e);
        }
    }

    /**
     * 获取性能统计
     */
    public PerformanceStats getPerformanceStats() {
        PerformanceStats stats = new PerformanceStats();
        stats.setCacheHits(cacheHits.get());
        stats.setCacheMisses(cacheMisses.get());
        stats.setCompressionSavings(compressionSavings.get());
        stats.setBatchQueueSize(batchQueue.size());
        stats.setSerializationCacheSize(serializationCache.size());
        stats.setTimestamp(LocalDateTime.now());
        
        long totalRequests = cacheHits.get() + cacheMisses.get();
        if (totalRequests > 0) {
            stats.setCacheHitRate((double) cacheHits.get() / totalRequests);
        }
        
        return stats;
    }

    /**
     * 清理缓存
     */
    public void clearCaches() {
        serializationCache.clear();
        log.info("序列化缓存已清理");
    }

    /**
     * 批量操作类型
     */
    public enum BatchOperationType {
        SET, GET, DELETE
    }

    /**
     * 批量操作
     */
    public static class BatchOperation {
        private final BatchOperationType type;
        private final Map<String, Object> keyValues;
        private final Duration expiration;

        public BatchOperation(BatchOperationType type, Map<String, Object> keyValues, Duration expiration) {
            this.type = type;
            this.keyValues = keyValues;
            this.expiration = expiration;
        }

        public BatchOperationType getType() { return type; }
        public Map<String, Object> getKeyValues() { return keyValues; }
        public Duration getExpiration() { return expiration; }
    }

    /**
     * 性能统计
     */
    public static class PerformanceStats {
        private long cacheHits;
        private long cacheMisses;
        private double cacheHitRate;
        private long compressionSavings;
        private int batchQueueSize;
        private int serializationCacheSize;
        private LocalDateTime timestamp;

        // Getters and Setters
        public long getCacheHits() { return cacheHits; }
        public void setCacheHits(long cacheHits) { this.cacheHits = cacheHits; }
        public long getCacheMisses() { return cacheMisses; }
        public void setCacheMisses(long cacheMisses) { this.cacheMisses = cacheMisses; }
        public double getCacheHitRate() { return cacheHitRate; }
        public void setCacheHitRate(double cacheHitRate) { this.cacheHitRate = cacheHitRate; }
        public long getCompressionSavings() { return compressionSavings; }
        public void setCompressionSavings(long compressionSavings) { this.compressionSavings = compressionSavings; }
        public int getBatchQueueSize() { return batchQueueSize; }
        public void setBatchQueueSize(int batchQueueSize) { this.batchQueueSize = batchQueueSize; }
        public int getSerializationCacheSize() { return serializationCacheSize; }
        public void setSerializationCacheSize(int serializationCacheSize) { this.serializationCacheSize = serializationCacheSize; }
        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    }
}