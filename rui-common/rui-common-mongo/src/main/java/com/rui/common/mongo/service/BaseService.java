package com.rui.common.mongo.service;

import com.rui.common.core.domain.PageResult;
import com.rui.common.mongo.entity.BaseEntity;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

/**
 * MongoDB基础Service接口
 * 
 * @param <T> 实体类型
 * @author rui
 */
public interface BaseService<T extends BaseEntity> {
    
    /**
     * 保存实体
     * 
     * @param entity 实体
     * @return 保存后的实体
     */
    T save(T entity);
    
    /**
     * 批量保存实体
     * 
     * @param entities 实体列表
     * @return 保存后的实体列表
     */
    List<T> saveAll(List<T> entities);
    
    /**
     * 根据ID查询实体
     * 
     * @param id 主键ID
     * @return 实体
     */
    Optional<T> findById(String id);
    
    /**
     * 根据ID查询未删除的实体
     * 
     * @param id 主键ID
     * @return 实体
     */
    Optional<T> findByIdAndNotDeleted(String id);
    
    /**
     * 查询所有实体
     * 
     * @return 实体列表
     */
    List<T> findAll();
    
    /**
     * 查询所有未删除的实体
     * 
     * @return 实体列表
     */
    List<T> findAllNotDeleted();
    
    /**
     * 分页查询实体
     * 
     * @param pageable 分页参数
     * @return 分页结果
     */
    PageResult<T> findPage(Pageable pageable);
    
    /**
     * 分页查询未删除的实体
     * 
     * @param pageable 分页参数
     * @return 分页结果
     */
    PageResult<T> findPageNotDeleted(Pageable pageable);
    
    /**
     * 根据租户ID查询实体
     * 
     * @param tenantId 租户ID
     * @return 实体列表
     */
    List<T> findByTenantId(String tenantId);
    
    /**
     * 根据租户ID查询未删除的实体
     * 
     * @param tenantId 租户ID
     * @return 实体列表
     */
    List<T> findByTenantIdAndNotDeleted(String tenantId);
    
    /**
     * 根据租户ID分页查询实体
     * 
     * @param tenantId 租户ID
     * @param pageable 分页参数
     * @return 分页结果
     */
    PageResult<T> findPageByTenantId(String tenantId, Pageable pageable);
    
    /**
     * 根据租户ID分页查询未删除的实体
     * 
     * @param tenantId 租户ID
     * @param pageable 分页参数
     * @return 分页结果
     */
    PageResult<T> findPageByTenantIdAndNotDeleted(String tenantId, Pageable pageable);
    
    /**
     * 根据创建人查询实体
     * 
     * @param createBy 创建人
     * @return 实体列表
     */
    List<T> findByCreateBy(String createBy);
    
    /**
     * 根据创建人查询未删除的实体
     * 
     * @param createBy 创建人
     * @return 实体列表
     */
    List<T> findByCreateByAndNotDeleted(String createBy);
    
    /**
     * 统计实体数量
     * 
     * @return 数量
     */
    long count();
    
    /**
     * 统计未删除的实体数量
     * 
     * @return 数量
     */
    long countNotDeleted();
    
    /**
     * 根据租户ID统计实体数量
     * 
     * @param tenantId 租户ID
     * @return 数量
     */
    long countByTenantId(String tenantId);
    
    /**
     * 根据租户ID统计未删除的实体数量
     * 
     * @param tenantId 租户ID
     * @return 数量
     */
    long countByTenantIdAndNotDeleted(String tenantId);
    
    /**
     * 判断实体是否存在
     * 
     * @param id 主键ID
     * @return 是否存在
     */
    boolean existsById(String id);
    
    /**
     * 判断未删除的实体是否存在
     * 
     * @param id 主键ID
     * @return 是否存在
     */
    boolean existsByIdAndNotDeleted(String id);
    
    /**
     * 删除实体
     * 
     * @param id 主键ID
     */
    void deleteById(String id);
    
    /**
     * 批量删除实体
     * 
     * @param ids 主键ID列表
     */
    void deleteByIds(List<String> ids);
    
    /**
     * 逻辑删除实体
     * 
     * @param id 主键ID
     */
    void logicalDelete(String id);
    
    /**
     * 批量逻辑删除实体
     * 
     * @param ids 主键ID列表
     */
    void logicalDeleteBatch(List<String> ids);
    
    /**
     * 恢复逻辑删除的实体
     * 
     * @param id 主键ID
     */
    void restore(String id);
    
    /**
     * 批量恢复逻辑删除的实体
     * 
     * @param ids 主键ID列表
     */
    void restoreBatch(List<String> ids);
}