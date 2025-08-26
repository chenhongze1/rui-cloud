package com.rui.common.mongo.service.impl;

import com.rui.common.core.domain.PageResult;
import com.rui.common.mongo.entity.BaseEntity;
import com.rui.common.mongo.repository.BaseRepository;
import com.rui.common.mongo.service.BaseService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * MongoDB基础Service实现类
 * 
 * @param <T> 实体类型
 * @param <R> Repository类型
 * @author rui
 */
@Slf4j
public abstract class BaseServiceImpl<T extends BaseEntity, R extends BaseRepository<T>> implements BaseService<T> {
    
    @Autowired
    protected R repository;
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public T save(T entity) {
        try {
            return repository.save(entity);
        } catch (Exception e) {
            log.error("保存实体失败: {}", e.getMessage(), e);
            throw new RuntimeException("保存实体失败", e);
        }
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<T> saveAll(List<T> entities) {
        try {
            return repository.saveAll(entities);
        } catch (Exception e) {
            log.error("批量保存实体失败: {}", e.getMessage(), e);
            throw new RuntimeException("批量保存实体失败", e);
        }
    }
    
    @Override
    public Optional<T> findById(String id) {
        try {
            return repository.findById(id);
        } catch (Exception e) {
            log.error("根据ID查询实体失败: {}", e.getMessage(), e);
            throw new RuntimeException("查询实体失败", e);
        }
    }
    
    @Override
    public Optional<T> findByIdAndNotDeleted(String id) {
        try {
            return repository.findByIdAndNotDeleted(id);
        } catch (Exception e) {
            log.error("根据ID查询未删除实体失败: {}", e.getMessage(), e);
            throw new RuntimeException("查询实体失败", e);
        }
    }
    
    @Override
    public List<T> findAll() {
        try {
            return repository.findAll();
        } catch (Exception e) {
            log.error("查询所有实体失败: {}", e.getMessage(), e);
            throw new RuntimeException("查询实体失败", e);
        }
    }
    
    @Override
    public List<T> findAllNotDeleted() {
        try {
            return repository.findAllNotDeleted();
        } catch (Exception e) {
            log.error("查询所有未删除实体失败: {}", e.getMessage(), e);
            throw new RuntimeException("查询实体失败", e);
        }
    }
    
    @Override
    public PageResult<T> findPage(Pageable pageable) {
        try {
            Page<T> page = repository.findAll(pageable);
            return PageResult.of(page.getContent(), page.getTotalElements(), 
                               page.getNumber() + 1, page.getSize());
        } catch (Exception e) {
            log.error("分页查询实体失败: {}", e.getMessage(), e);
            throw new RuntimeException("分页查询失败", e);
        }
    }
    
    @Override
    public PageResult<T> findPageNotDeleted(Pageable pageable) {
        try {
            Page<T> page = repository.findAllNotDeleted(pageable);
            return PageResult.of(page.getContent(), page.getTotalElements(), 
                               page.getNumber() + 1, page.getSize());
        } catch (Exception e) {
            log.error("分页查询未删除实体失败: {}", e.getMessage(), e);
            throw new RuntimeException("分页查询失败", e);
        }
    }
    
    @Override
    public List<T> findByTenantId(String tenantId) {
        try {
            return repository.findByTenantIdAndNotDeleted(tenantId);
        } catch (Exception e) {
            log.error("根据租户ID查询实体失败: {}", e.getMessage(), e);
            throw new RuntimeException("查询实体失败", e);
        }
    }
    
    @Override
    public List<T> findByTenantIdAndNotDeleted(String tenantId) {
        try {
            return repository.findByTenantIdAndNotDeleted(tenantId);
        } catch (Exception e) {
            log.error("根据租户ID查询未删除实体失败: {}", e.getMessage(), e);
            throw new RuntimeException("查询实体失败", e);
        }
    }
    
    @Override
    public PageResult<T> findPageByTenantId(String tenantId, Pageable pageable) {
        try {
            Page<T> page = repository.findByTenantIdAndNotDeleted(tenantId, pageable);
            return PageResult.of(page.getContent(), page.getTotalElements(), 
                               page.getNumber() + 1, page.getSize());
        } catch (Exception e) {
            log.error("根据租户ID分页查询实体失败: {}", e.getMessage(), e);
            throw new RuntimeException("分页查询失败", e);
        }
    }
    
    @Override
    public PageResult<T> findPageByTenantIdAndNotDeleted(String tenantId, Pageable pageable) {
        try {
            Page<T> page = repository.findByTenantIdAndNotDeleted(tenantId, pageable);
            return PageResult.of(page.getContent(), page.getTotalElements(), 
                               page.getNumber() + 1, page.getSize());
        } catch (Exception e) {
            log.error("根据租户ID分页查询未删除实体失败: {}", e.getMessage(), e);
            throw new RuntimeException("分页查询失败", e);
        }
    }
    
    @Override
    public List<T> findByCreateBy(String createBy) {
        try {
            return repository.findByCreateByAndNotDeleted(createBy);
        } catch (Exception e) {
            log.error("根据创建人查询实体失败: {}", e.getMessage(), e);
            throw new RuntimeException("查询实体失败", e);
        }
    }
    
    @Override
    public List<T> findByCreateByAndNotDeleted(String createBy) {
        try {
            return repository.findByCreateByAndNotDeleted(createBy);
        } catch (Exception e) {
            log.error("根据创建人查询未删除实体失败: {}", e.getMessage(), e);
            throw new RuntimeException("查询实体失败", e);
        }
    }
    
    @Override
    public long count() {
        try {
            return repository.count();
        } catch (Exception e) {
            log.error("统计实体数量失败: {}", e.getMessage(), e);
            throw new RuntimeException("统计失败", e);
        }
    }
    
    @Override
    public long countNotDeleted() {
        try {
            return repository.countNotDeleted();
        } catch (Exception e) {
            log.error("统计未删除实体数量失败: {}", e.getMessage(), e);
            throw new RuntimeException("统计失败", e);
        }
    }
    
    @Override
    public long countByTenantId(String tenantId) {
        try {
            return repository.countByTenantIdAndNotDeleted(tenantId);
        } catch (Exception e) {
            log.error("根据租户ID统计实体数量失败: {}", e.getMessage(), e);
            throw new RuntimeException("统计失败", e);
        }
    }
    
    @Override
    public long countByTenantIdAndNotDeleted(String tenantId) {
        try {
            return repository.countByTenantIdAndNotDeleted(tenantId);
        } catch (Exception e) {
            log.error("根据租户ID统计未删除实体数量失败: {}", e.getMessage(), e);
            throw new RuntimeException("统计失败", e);
        }
    }
    
    @Override
    public boolean existsById(String id) {
        try {
            return repository.existsById(id);
        } catch (Exception e) {
            log.error("判断实体是否存在失败: {}", e.getMessage(), e);
            throw new RuntimeException("判断失败", e);
        }
    }
    
    @Override
    public boolean existsByIdAndNotDeleted(String id) {
        try {
            return repository.findByIdAndNotDeleted(id).isPresent();
        } catch (Exception e) {
            log.error("判断未删除实体是否存在失败: {}", e.getMessage(), e);
            throw new RuntimeException("判断失败", e);
        }
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteById(String id) {
        try {
            repository.deleteById(id);
        } catch (Exception e) {
            log.error("删除实体失败: {}", e.getMessage(), e);
            throw new RuntimeException("删除实体失败", e);
        }
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteByIds(List<String> ids) {
        try {
            repository.deleteAllById(ids);
        } catch (Exception e) {
            log.error("批量删除实体失败: {}", e.getMessage(), e);
            throw new RuntimeException("批量删除实体失败", e);
        }
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void logicalDelete(String id) {
        try {
            repository.logicalDelete(id);
        } catch (Exception e) {
            log.error("逻辑删除实体失败: {}", e.getMessage(), e);
            throw new RuntimeException("逻辑删除实体失败", e);
        }
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void logicalDeleteBatch(List<String> ids) {
        try {
            repository.logicalDeleteBatch(ids);
        } catch (Exception e) {
            log.error("批量逻辑删除实体失败: {}", e.getMessage(), e);
            throw new RuntimeException("批量逻辑删除实体失败", e);
        }
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void restore(String id) {
        try {
            repository.restore(id);
        } catch (Exception e) {
            log.error("恢复实体失败: {}", e.getMessage(), e);
            throw new RuntimeException("恢复实体失败", e);
        }
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void restoreBatch(List<String> ids) {
        try {
            ids.forEach(repository::restore);
        } catch (Exception e) {
            log.error("批量恢复实体失败: {}", e.getMessage(), e);
            throw new RuntimeException("批量恢复实体失败", e);
        }
    }
}