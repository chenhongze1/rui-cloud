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
     * 分页大小
     */
    @NotNull(message = "分页大小不能为空")
    @Min(value = 1, message = "分页大小不能小于1")
    @Max(value = 100, message = "分页大小不能大于100")
    private Integer pageSize = 10;

    /**
     * 当前页数
     */
    @NotNull(message = "页码不能为空")
    @Min(value = 1, message = "页码不能小于1")
    private Integer pageNum = 1;

    /**
     * 排序字段
     */
    private String orderByColumn;

    /**
     * 排序方向 desc 或者 asc
     */
    private String isAsc = "asc";

    /**
     * 分页参数合理化
     */
    private Boolean reasonable = true;

    /**
     * 获取排序字符串
     */
    public String getOrderBy() {
        if (orderByColumn == null || orderByColumn.trim().isEmpty()) {
            return null;
        }
        return orderByColumn + " " + isAsc;
    }
}