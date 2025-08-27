package com.rui.common.web.controller;

import com.rui.common.core.domain.R;
import com.rui.common.core.domain.PageResult;
import com.rui.common.core.page.PageQuery;
import com.rui.common.core.page.TableDataInfo;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;

import java.beans.PropertyEditorSupport;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * web层通用数据处理
 *
 * @author rui
 */
@Slf4j
public class BaseController {

    /**
     * 将前台传递过来的日期格式的字符串，自动转化为Date类型
     */
    @InitBinder
    public void initBinder(WebDataBinder binder) {
        // Date 类型转换
        binder.registerCustomEditor(LocalDate.class, new PropertyEditorSupport() {
            @Override
            public void setAsText(String text) {
                setValue(LocalDate.parse(text, DateTimeFormatter.ofPattern("yyyy-MM-dd")));
            }
        });
        binder.registerCustomEditor(LocalDateTime.class, new PropertyEditorSupport() {
            @Override
            public void setAsText(String text) {
                setValue(LocalDateTime.parse(text, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            }
        });
        binder.registerCustomEditor(LocalTime.class, new PropertyEditorSupport() {
            @Override
            public void setAsText(String text) {
                setValue(LocalTime.parse(text, DateTimeFormatter.ofPattern("HH:mm:ss")));
            }
        });
    }

    /**
     * 设置请求分页数据
     */
    protected void startPage(PageQuery pageQuery) {
        Integer pageNum = pageQuery.getPageNum();
        Integer pageSize = pageQuery.getPageSize();
        String orderBy = pageQuery.getOrderBy();
        Boolean reasonable = pageQuery.getReasonable();
        PageHelper.startPage(pageNum, pageSize, orderBy).setReasonable(reasonable);
    }

    /**
     * 清理分页的线程变量
     */
    protected void clearPage() {
        PageHelper.clearPage();
    }

    /**
     * 响应请求分页数据
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    protected <T> TableDataInfo<T> getDataTable(List<T> list) {
        TableDataInfo<T> rspData = new TableDataInfo<>();
        rspData.setRows(list);
        rspData.setTotal(new PageInfo(list).getTotal());
        return rspData;
    }

    /**
     * 基于PageResult构建TableDataInfo
     *
     * @param pageResult 分页结果
     * @return 分页数据对象
     */
    protected <T> TableDataInfo<T> getDataTable(PageResult<T> pageResult) {
        return TableDataInfo.build(pageResult);
    }

    /**
     * 构建PageResult对象
     *
     * @param list 数据列表
     * @param total 总记录数
     * @param current 当前页码
     * @param size 每页大小
     * @return PageResult对象
     */
    protected <T> PageResult<T> getPageResult(List<T> list, long total, int current, int size) {
        return PageResult.of(list, total, current, size);
    }

    /**
     * 返回成功
     */
    public R<Void> success() {
        return R.ok();
    }

    /**
     * 返回成功消息
     */
    public R<Void> success(String message) {
        return R.ok(message);
    }

    /**
     * 返回成功消息
     */
    public <T> R<T> success(T data) {
        return R.ok(data);
    }

    /**
     * 返回成功消息
     */
    public <T> R<T> success(String message, T data) {
        return R.ok(message, data);
    }

    /**
     * 返回错误消息
     */
    public R<Void> error() {
        return R.fail();
    }

    /**
     * 返回错误消息
     */
    public R<Void> error(String message) {
        return R.fail(message);
    }

    /**
     * 返回警告消息
     */
    public R<Void> warn(String message) {
        return R.warn(message);
    }

    /**
     * 响应返回结果
     *
     * @param rows 影响行数
     * @return 操作结果
     */
    protected R<Void> toAjax(int rows) {
        return rows > 0 ? R.ok() : R.fail();
    }

    /**
     * 响应返回结果
     *
     * @param result 结果
     * @return 操作结果
     */
    protected R<Void> toAjax(boolean result) {
        return result ? success() : error();
    }
}