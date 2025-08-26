package com.rui.common.mongo.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.data.annotation.*;
import org.springframework.data.mongodb.core.mapping.Field;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * MongoDB基础实体类
 * 
 * @author rui
 */
@Data
@Accessors(chain = true)
public abstract class BaseEntity implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 主键ID
     */
    @Id
    private String id;
    
    /**
     * 创建时间
     */
    @CreatedDate
    @Field("create_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    @LastModifiedDate
    @Field("update_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
    
    /**
     * 创建人
     */
    @CreatedBy
    @Field("create_by")
    private String createBy;
    
    /**
     * 更新人
     */
    @LastModifiedBy
    @Field("update_by")
    private String updateBy;
    
    /**
     * 版本号（乐观锁）
     */
    @Version
    @Field("version")
    private Long version;
    
    /**
     * 租户ID
     */
    @Field("tenant_id")
    private String tenantId;
    
    /**
     * 逻辑删除标识（0-未删除，1-已删除）
     */
    @Field("deleted")
    private Integer deleted = 0;
    
    /**
     * 备注
     */
    @Field("remark")
    private String remark;
}