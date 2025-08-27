package com.rui.common.core.domain;

import lombok.Data;

/**
 * 分页查询参数
 *
 * @author rui
 */
@Data
public class PageQuery {

    /**
     * 页码
     */
    private Integer pageNum = 1;

    /**
     * 页面大小
     */
    private Integer pageSize = 10;

    /**
     * 排序字段
     */
    private String orderBy;

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
     * 最大页面大小限制
     */
    private static final Integer MAX_PAGE_SIZE = 500;

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
}