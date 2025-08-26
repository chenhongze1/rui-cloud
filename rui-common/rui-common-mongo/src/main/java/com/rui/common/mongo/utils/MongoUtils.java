package com.rui.common.mongo.utils;

import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * MongoDB工具类
 * 
 * @author rui
 */
@Slf4j
@Component
public class MongoUtils {
    
    @Autowired
    private MongoTemplate mongoTemplate;
    
    /**
     * 保存文档
     * 
     * @param entity 实体对象
     * @param <T> 实体类型
     * @return 保存后的实体
     */
    public <T> T save(T entity) {
        try {
            return mongoTemplate.save(entity);
        } catch (Exception e) {
            log.error("保存文档失败: {}", e.getMessage(), e);
            throw new RuntimeException("保存文档失败", e);
        }
    }
    
    /**
     * 保存文档到指定集合
     * 
     * @param entity 实体对象
     * @param collectionName 集合名称
     * @param <T> 实体类型
     * @return 保存后的实体
     */
    public <T> T save(T entity, String collectionName) {
        try {
            return mongoTemplate.save(entity, collectionName);
        } catch (Exception e) {
            log.error("保存文档到集合{}失败: {}", collectionName, e.getMessage(), e);
            throw new RuntimeException("保存文档失败", e);
        }
    }
    
    /**
     * 批量保存文档
     * 
     * @param entities 实体列表
     * @param entityClass 实体类
     * @param <T> 实体类型
     * @return 保存后的实体列表
     */
    public <T> Collection<T> saveAll(Collection<T> entities, Class<T> entityClass) {
        try {
            return mongoTemplate.insertAll(entities);
        } catch (Exception e) {
            log.error("批量保存文档失败: {}", e.getMessage(), e);
            throw new RuntimeException("批量保存文档失败", e);
        }
    }
    
    /**
     * 根据ID查询文档
     * 
     * @param id 文档ID
     * @param entityClass 实体类
     * @param <T> 实体类型
     * @return 实体对象
     */
    public <T> T findById(Object id, Class<T> entityClass) {
        try {
            return mongoTemplate.findById(id, entityClass);
        } catch (Exception e) {
            log.error("根据ID查询文档失败: {}", e.getMessage(), e);
            throw new RuntimeException("查询文档失败", e);
        }
    }
    
    /**
     * 根据条件查询单个文档
     * 
     * @param query 查询条件
     * @param entityClass 实体类
     * @param <T> 实体类型
     * @return 实体对象
     */
    public <T> T findOne(Query query, Class<T> entityClass) {
        try {
            return mongoTemplate.findOne(query, entityClass);
        } catch (Exception e) {
            log.error("根据条件查询单个文档失败: {}", e.getMessage(), e);
            throw new RuntimeException("查询文档失败", e);
        }
    }
    
    /**
     * 根据条件查询文档列表
     * 
     * @param query 查询条件
     * @param entityClass 实体类
     * @param <T> 实体类型
     * @return 实体列表
     */
    public <T> List<T> find(Query query, Class<T> entityClass) {
        try {
            return mongoTemplate.find(query, entityClass);
        } catch (Exception e) {
            log.error("根据条件查询文档列表失败: {}", e.getMessage(), e);
            throw new RuntimeException("查询文档失败", e);
        }
    }
    
    /**
     * 查询所有文档
     * 
     * @param entityClass 实体类
     * @param <T> 实体类型
     * @return 实体列表
     */
    public <T> List<T> findAll(Class<T> entityClass) {
        try {
            return mongoTemplate.findAll(entityClass);
        } catch (Exception e) {
            log.error("查询所有文档失败: {}", e.getMessage(), e);
            throw new RuntimeException("查询文档失败", e);
        }
    }
    
    /**
     * 分页查询文档
     * 
     * @param query 查询条件
     * @param entityClass 实体类
     * @param page 页码（从0开始）
     * @param size 页大小
     * @param <T> 实体类型
     * @return 实体列表
     */
    public <T> List<T> findPage(Query query, Class<T> entityClass, int page, int size) {
        try {
            query.skip((long) page * size).limit(size);
            return mongoTemplate.find(query, entityClass);
        } catch (Exception e) {
            log.error("分页查询文档失败: {}", e.getMessage(), e);
            throw new RuntimeException("分页查询失败", e);
        }
    }
    
    /**
     * 统计文档数量
     * 
     * @param query 查询条件
     * @param entityClass 实体类
     * @return 文档数量
     */
    public long count(Query query, Class<?> entityClass) {
        try {
            return mongoTemplate.count(query, entityClass);
        } catch (Exception e) {
            log.error("统计文档数量失败: {}", e.getMessage(), e);
            throw new RuntimeException("统计失败", e);
        }
    }
    
    /**
     * 更新文档
     * 
     * @param query 查询条件
     * @param update 更新内容
     * @param entityClass 实体类
     * @return 更新结果
     */
    public UpdateResult updateFirst(Query query, Update update, Class<?> entityClass) {
        try {
            return mongoTemplate.updateFirst(query, update, entityClass);
        } catch (Exception e) {
            log.error("更新文档失败: {}", e.getMessage(), e);
            throw new RuntimeException("更新文档失败", e);
        }
    }
    
    /**
     * 批量更新文档
     * 
     * @param query 查询条件
     * @param update 更新内容
     * @param entityClass 实体类
     * @return 更新结果
     */
    public UpdateResult updateMulti(Query query, Update update, Class<?> entityClass) {
        try {
            return mongoTemplate.updateMulti(query, update, entityClass);
        } catch (Exception e) {
            log.error("批量更新文档失败: {}", e.getMessage(), e);
            throw new RuntimeException("批量更新文档失败", e);
        }
    }
    
    /**
     * 删除文档
     * 
     * @param query 查询条件
     * @param entityClass 实体类
     * @return 删除结果
     */
    public DeleteResult remove(Query query, Class<?> entityClass) {
        try {
            return mongoTemplate.remove(query, entityClass);
        } catch (Exception e) {
            log.error("删除文档失败: {}", e.getMessage(), e);
            throw new RuntimeException("删除文档失败", e);
        }
    }
    
    /**
     * 根据ID删除文档
     * 
     * @param id 文档ID
     * @param entityClass 实体类
     * @return 删除结果
     */
    public DeleteResult removeById(Object id, Class<?> entityClass) {
        try {
            Query query = new Query(Criteria.where("id").is(id));
            return mongoTemplate.remove(query, entityClass);
        } catch (Exception e) {
            log.error("根据ID删除文档失败: {}", e.getMessage(), e);
            throw new RuntimeException("删除文档失败", e);
        }
    }
    
    /**
     * 聚合查询
     * 
     * @param aggregation 聚合管道
     * @param collectionName 集合名称
     * @param outputType 输出类型
     * @param <T> 输出类型
     * @return 聚合结果
     */
    public <T> AggregationResults<T> aggregate(Aggregation aggregation, String collectionName, Class<T> outputType) {
        try {
            return mongoTemplate.aggregate(aggregation, collectionName, outputType);
        } catch (Exception e) {
            log.error("聚合查询失败: {}", e.getMessage(), e);
            throw new RuntimeException("聚合查询失败", e);
        }
    }
    
    /**
     * 构建等值查询条件
     * 
     * @param field 字段名
     * @param value 字段值
     * @return 查询对象
     */
    public static Query buildEqualQuery(String field, Object value) {
        return new Query(Criteria.where(field).is(value));
    }
    
    /**
     * 构建范围查询条件
     * 
     * @param field 字段名
     * @param start 开始值
     * @param end 结束值
     * @return 查询对象
     */
    public static Query buildRangeQuery(String field, Object start, Object end) {
        return new Query(Criteria.where(field).gte(start).lte(end));
    }
    
    /**
     * 构建模糊查询条件
     * 
     * @param field 字段名
     * @param value 字段值
     * @return 查询对象
     */
    public static Query buildLikeQuery(String field, String value) {
        Pattern pattern = Pattern.compile(".*" + value + ".*", Pattern.CASE_INSENSITIVE);
        return new Query(Criteria.where(field).regex(pattern));
    }
    
    /**
     * 构建IN查询条件
     * 
     * @param field 字段名
     * @param values 字段值列表
     * @return 查询对象
     */
    public static Query buildInQuery(String field, Collection<?> values) {
        return new Query(Criteria.where(field).in(values));
    }
    
    /**
     * 构建排序查询条件
     * 
     * @param field 字段名
     * @param direction 排序方向
     * @return 查询对象
     */
    public static Query buildSortQuery(String field, Sort.Direction direction) {
        return new Query().with(Sort.by(direction, field));
    }
    
    /**
     * 构建更新对象
     * 
     * @param updateMap 更新字段映射
     * @return 更新对象
     */
    public static Update buildUpdate(Map<String, Object> updateMap) {
        Update update = new Update();
        if (!CollectionUtils.isEmpty(updateMap)) {
            updateMap.forEach(update::set);
        }
        return update;
    }
    
    /**
     * 构建递增更新对象
     * 
     * @param field 字段名
     * @param value 递增值
     * @return 更新对象
     */
    public static Update buildIncUpdate(String field, Number value) {
        return new Update().inc(field, value);
    }
    
    /**
     * 获取MongoTemplate
     * 
     * @return MongoTemplate实例
     */
    public MongoTemplate getMongoTemplate() {
        return mongoTemplate;
    }
}