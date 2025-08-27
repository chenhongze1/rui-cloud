package com.rui.common.core.page;

import lombok.Data;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * 分页查询基础类
 *
 * @author rui
 */
@Data
public class PageQuery implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 最大页面大小限制
     */
    private static final Integer MAX_PAGE_SIZE = 500;

    /**
     * 页码
     */
    @NotNull(message = "页码不能为空")
    @Min(value = 1, message = "页码不能小于1")
    private Integer pageNum = 1;

    /**
     * 页面大小
     */
    @NotNull(message = "分页大小不能为空")
    @Min(value = 1, message = "分页大小不能小于1")
    @Max(value = 500, message = "分页大小不能大于500")
    private Integer pageSize = 10;

    /**
     * 排序字段
     */
    private String orderByColumn;

    /**
     * 排序方向 desc 或者 asc
     */
    private String isAsc = "asc";

    /**
     * 是否启用合理化，禁用 reasonable 后，如果 pageNum<1 或 pageNum>pages 会返回空数据
     */
    private Boolean reasonable = true;

    /**
     * 是否查询总数
     */
    private Boolean searchCount = true;

    /**
     * 优化COUNT SQL查询
     */
    private Boolean optimizeCountSql = true;

    /**
     * 设置页面大小，限制最大值
     */
    public void setPageSize(Integer pageSize) {
        if (pageSize != null && pageSize > MAX_PAGE_SIZE) {
            this.pageSize = MAX_PAGE_SIZE;
        } else {
            this.pageSize = pageSize;
        }
    }

    /**
     * 获取偏移量
     */
    public Integer getOffset() {
        return (pageNum - 1) * pageSize;
    }

    /**
     * 获取限制数量
     */
    public Integer getLimit() {
        return pageSize;
    }

    /**
     * 获取排序字符串
     */
    public String getOrderBy() {
        if (orderByColumn == null || orderByColumn.trim().isEmpty()) {
            return null;
        }
        return orderByColumn + " " + isAsc;
    }

    /**
     * 兼容旧版本的orderBy字段
     * @deprecated 请使用 orderByColumn 和 isAsc
     */
    @Deprecated
    public void setOrderBy(String orderBy) {
        if (orderBy != null && !orderBy.trim().isEmpty()) {
            String[] parts = orderBy.trim().split("\\s+");
            if (parts.length >= 1) {
                this.orderByColumn = parts[0];
                if (parts.length >= 2) {
                    this.isAsc = parts[1].toLowerCase();
                }
            }
        }
    }
}