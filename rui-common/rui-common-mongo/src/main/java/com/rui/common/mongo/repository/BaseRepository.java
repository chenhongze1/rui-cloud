package com.rui.common.mongo.repository;

import com.rui.common.mongo.entity.BaseEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.List;
import java.util.Optional;

/**
 * MongoDB基础Repository接口
 * 
 * @param <T> 实体类型
 * @author rui
 */
@NoRepositoryBean
public interface BaseRepository<T extends BaseEntity> extends MongoRepository<T, String> {
    
    /**
     * 根据ID查询未删除的实体
     * 
     * @param id 主键ID
     * @return 实体
     */
    @Query("{'_id': ?0, 'deleted': 0}")
    Optional<T> findByIdAndNotDeleted(String id);
    
    /**
     * 查询所有未删除的实体
     * 
     * @return 实体列表
     */
    @Query("{'deleted': 0}")
    List<T> findAllNotDeleted();
    
    /**
     * 分页查询未删除的实体
     * 
     * @param pageable 分页参数
     * @return 分页结果
     */
    @Query("{'deleted': 0}")
    Page<T> findAllNotDeleted(Pageable pageable);
    
    /**
     * 根据租户ID查询未删除的实体
     * 
     * @param tenantId 租户ID
     * @return 实体列表
     */
    @Query("{'tenant_id': ?0, 'deleted': 0}")
    List<T> findByTenantIdAndNotDeleted(String tenantId);
    
    /**
     * 根据租户ID分页查询未删除的实体
     * 
     * @param tenantId 租户ID
     * @param pageable 分页参数
     * @return 分页结果
     */
    @Query("{'tenant_id': ?0, 'deleted': 0}")
    Page<T> findByTenantIdAndNotDeleted(String tenantId, Pageable pageable);
    
    /**
     * 根据创建人查询未删除的实体
     * 
     * @param createBy 创建人
     * @return 实体列表
     */
    @Query("{'create_by': ?0, 'deleted': 0}")
    List<T> findByCreateByAndNotDeleted(String createBy);
    
    /**
     * 根据创建人分页查询未删除的实体
     * 
     * @param createBy 创建人
     * @param pageable 分页参数
     * @return 分页结果
     */
    @Query("{'create_by': ?0, 'deleted': 0}")
    Page<T> findByCreateByAndNotDeleted(String createBy, Pageable pageable);
    
    /**
     * 统计未删除的实体数量
     * 
     * @return 数量
     */
    @Query(value = "{'deleted': 0}", count = true)
    long countNotDeleted();
    
    /**
     * 根据租户ID统计未删除的实体数量
     * 
     * @param tenantId 租户ID
     * @return 数量
     */
    @Query(value = "{'tenant_id': ?0, 'deleted': 0}", count = true)
    long countByTenantIdAndNotDeleted(String tenantId);
    
    /**
     * 逻辑删除实体
     * 
     * @param id 主键ID
     */
    default void logicalDelete(String id) {
        findById(id).ifPresent(entity -> {
            entity.setDeleted(1);
            save(entity);
        });
    }
    
    /**
     * 批量逻辑删除实体
     * 
     * @param ids 主键ID列表
     */
    default void logicalDeleteBatch(List<String> ids) {
        List<T> entities = findAllById(ids);
        entities.forEach(entity -> entity.setDeleted(1));
        saveAll(entities);
    }
    
    /**
     * 恢复逻辑删除的实体
     * 
     * @param id 主键ID
     */
    default void restore(String id) {
        findById(id).ifPresent(entity -> {
            entity.setDeleted(0);
            save(entity);
        });
    }
}