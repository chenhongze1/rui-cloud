package com.rui.common.core.domain;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

/**
 * 分页结果封装类
 * 
 * @param <T> 数据类型
 * @author rui
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageResult<T> {
    
    /**
     * 数据列表
     */
    private List<T> records;
    
    /**
     * 总记录数
     */
    private long total;
    
    /**
     * 当前页码（从1开始）
     */
    private int current;
    
    /**
     * 每页大小
     */
    private int size;
    
    /**
     * 总页数
     */
    private int pages;
    
    /**
     * 是否有上一页
     */
    private boolean hasPrevious;
    
    /**
     * 是否有下一页
     */
    private boolean hasNext;
    
    /**
     * 创建分页结果
     * 
     * @param records 数据列表
     * @param total 总记录数
     * @param current 当前页码
     * @param size 每页大小
     * @param <T> 数据类型
     * @return 分页结果
     */
    public static <T> PageResult<T> of(List<T> records, long total, int current, int size) {
        PageResult<T> result = new PageResult<>();
        result.setRecords(records);
        result.setTotal(total);
        result.setCurrent(current);
        result.setSize(size);
        
        // 计算总页数
        int pages = size > 0 ? (int) Math.ceil((double) total / size) : 0;
        result.setPages(pages);
        
        // 计算是否有上一页和下一页
        result.setHasPrevious(current > 1);
        result.setHasNext(current < pages);
        
        return result;
    }
    
    /**
     * 创建空的分页结果
     * 
     * @param <T> 数据类型
     * @return 空的分页结果
     */
    public static <T> PageResult<T> empty() {
        return new PageResult<>(List.of(), 0L, 1, 10, 0, false, false);
    }
    
    /**
     * 创建空的分页结果
     * 
     * @param current 当前页码
     * @param size 每页大小
     * @param <T> 数据类型
     * @return 空的分页结果
     */
    public static <T> PageResult<T> empty(int current, int size) {
        return new PageResult<>(List.of(), 0L, current, size, 0, false, false);
    }
}