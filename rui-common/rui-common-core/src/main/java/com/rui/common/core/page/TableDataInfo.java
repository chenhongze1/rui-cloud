package com.rui.common.core.page;

import com.rui.common.core.domain.PageResult;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 表格分页数据对象
 * 基于PageResult扩展，增加API响应格式支持
 *
 * @author rui
 */
@Data
@NoArgsConstructor
public class TableDataInfo<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 总记录数
     */
    private long total;

    /**
     * 列表数据
     */
    private List<T> rows;

    /**
     * 消息状态码
     */
    private int code = 200;

    /**
     * 消息内容
     */
    private String msg = "查询成功";

    /**
     * 分页信息（兼容PageResult）
     */
    private PageResult<T> pageInfo;

    /**
     * 分页
     *
     * @param list  列表数据
     * @param total 总记录数
     */
    public TableDataInfo(List<T> list, long total) {
        this.rows = list;
        this.total = total;
    }

    /**
     * 基于PageResult构造
     *
     * @param pageResult 分页结果
     */
    public TableDataInfo(PageResult<T> pageResult) {
        this.rows = pageResult.getRecords();
        this.total = pageResult.getTotal();
        this.pageInfo = pageResult;
    }

    public static <T> TableDataInfo<T> build(List<T> list) {
        TableDataInfo<T> rspData = new TableDataInfo<>();
        rspData.setRows(list);
        rspData.setTotal(list.size());
        return rspData;
    }

    public static <T> TableDataInfo<T> build(List<T> list, long total) {
        TableDataInfo<T> rspData = new TableDataInfo<>();
        rspData.setRows(list);
        rspData.setTotal(total);
        return rspData;
    }

    public static <T> TableDataInfo<T> build() {
        TableDataInfo<T> rspData = new TableDataInfo<>();
        return rspData;
    }

    /**
     * 基于PageResult构建TableDataInfo
     *
     * @param pageResult 分页结果
     * @param <T> 数据类型
     * @return TableDataInfo
     */
    public static <T> TableDataInfo<T> build(PageResult<T> pageResult) {
        TableDataInfo<T> rspData = new TableDataInfo<>(pageResult);
        return rspData;
    }

    /**
     * 获取当前页码（兼容PageResult）
     */
    public int getCurrent() {
        return pageInfo != null ? pageInfo.getCurrent() : 1;
    }

    /**
     * 获取每页大小（兼容PageResult）
     */
    public int getSize() {
        return pageInfo != null ? pageInfo.getSize() : (int) total;
    }

    /**
     * 获取总页数（兼容PageResult）
     */
    public int getPages() {
        return pageInfo != null ? pageInfo.getPages() : 1;
    }
}