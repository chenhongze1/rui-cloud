package com.rui.common.mybatis.utils;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.rui.common.core.domain.PageQuery;
import com.rui.common.core.page.TableDataInfo;
import cn.hutool.core.util.StrUtil;

import java.util.List;

/**
 * 分页工具类
 *
 * @author rui
 */
public class PageUtils {

    /**
     * 设置请求分页数据
     */
    public static void startPage() {
        PageQuery pageQuery = getPageQuery();
        Integer pageNum = pageQuery.getPageNum();
        Integer pageSize = pageQuery.getPageSize();
        String orderBy = pageQuery.getOrderBy();
        Boolean reasonable = pageQuery.getReasonable();
        PageHelper.startPage(pageNum, pageSize, orderBy).setReasonable(reasonable);
    }

    /**
     * 清理分页的线程变量
     */
    public static void clearPage() {
        PageHelper.clearPage();
    }

    /**
     * 响应请求分页数据
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static TableDataInfo getDataTable(List<?> list) {
        TableDataInfo rspData = new TableDataInfo();
        rspData.setCode(200);
        rspData.setMsg("查询成功");
        rspData.setRows(list);
        rspData.setTotal(new PageInfo(list).getTotal());
        return rspData;
    }

    /**
     * MyBatis Plus分页对象转换
     */
    public static <T> TableDataInfo<T> getDataTable(IPage<T> page) {
        TableDataInfo<T> rspData = new TableDataInfo<>();
        rspData.setCode(200);
        rspData.setMsg("查询成功");
        rspData.setRows(page.getRecords());
        rspData.setTotal(page.getTotal());
        return rspData;
    }

    /**
     * 构建MyBatis Plus分页对象
     */
    public static <T> Page<T> buildPage() {
        PageQuery pageQuery = getPageQuery();
        Integer pageNum = pageQuery.getPageNum();
        Integer pageSize = pageQuery.getPageSize();
        String orderBy = pageQuery.getOrderBy();
        
        Page<T> page = new Page<>(pageNum, pageSize);
        
        if (StrUtil.isNotBlank(orderBy)) {
            page.addOrder(parseOrderBy(orderBy));
        }
        
        return page;
    }

    /**
     * 解析排序字符串
     */
    private static com.baomidou.mybatisplus.core.metadata.OrderItem parseOrderBy(String orderBy) {
        if (StrUtil.isBlank(orderBy)) {
            return null;
        }
        
        String[] orderByArray = orderBy.split(" ");
        String column = orderByArray[0];
        boolean isAsc = orderByArray.length > 1 && "asc".equalsIgnoreCase(orderByArray[1]);
        
        return isAsc ? 
            com.baomidou.mybatisplus.core.metadata.OrderItem.asc(column) : 
            com.baomidou.mybatisplus.core.metadata.OrderItem.desc(column);
    }

    /**
     * 获取分页查询对象
     * TODO: 从ThreadLocal或Request中获取分页参数
     */
    private static PageQuery getPageQuery() {
        // 这里应该从ThreadLocal或Request中获取分页参数
        // 暂时返回默认值
        PageQuery pageQuery = new PageQuery();
        pageQuery.setPageNum(1);
        pageQuery.setPageSize(10);
        return pageQuery;
    }
}