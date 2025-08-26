package com.rui.common.core.domain;

import com.alibaba.fastjson2.JSON;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 统一响应结果类
 *
 * @param <T> 数据类型
 * @author rui
 */
@Data
@NoArgsConstructor
public class R<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 成功状态码
     */
    public static final int SUCCESS = 200;

    /**
     * 失败状态码
     */
    public static final int FAIL = 500;

    /**
     * 状态码
     */
    private int code;

    /**
     * 返回消息
     */
    private String msg;

    /**
     * 返回数据
     */
    private T data;

    /**
     * 时间戳
     */
    private long timestamp;

    public R(int code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * 返回成功消息
     *
     * @return 成功消息
     */
    public static <T> R<T> ok() {
        return restResult(null, SUCCESS, "操作成功");
    }

    /**
     * 返回成功数据
     *
     * @param data 返回对象
     * @return 成功消息
     */
    public static <T> R<T> ok(T data) {
        return restResult(data, SUCCESS, "操作成功");
    }

    /**
     * 返回成功消息
     *
     * @param msg 返回消息
     * @return 成功消息
     */
    public static <T> R<T> ok(String msg) {
        return restResult(null, SUCCESS, msg);
    }

    /**
     * 返回成功消息
     *
     * @param msg  返回消息
     * @param data 数据对象
     * @return 成功消息
     */
    public static <T> R<T> ok(String msg, T data) {
        return restResult(data, SUCCESS, msg);
    }

    /**
     * 返回错误消息
     *
     * @return 错误消息
     */
    public static <T> R<T> fail() {
        return restResult(null, FAIL, "操作失败");
    }

    /**
     * 返回错误消息
     *
     * @param msg 返回消息
     * @return 错误消息
     */
    public static <T> R<T> fail(String msg) {
        return restResult(null, FAIL, msg);
    }

    /**
     * 返回错误消息
     *
     * @param code 状态码
     * @param msg  返回消息
     * @return 错误消息
     */
    public static <T> R<T> fail(int code, String msg) {
        return restResult(null, code, msg);
    }

    /**
     * 返回错误消息
     *
     * @param msg  返回消息
     * @param data 数据对象
     * @return 错误消息
     */
    public static <T> R<T> fail(String msg, T data) {
        return restResult(data, FAIL, msg);
    }

    /**
     * 返回警告消息
     *
     * @param msg 返回消息
     * @return 警告消息
     */
    public static <T> R<T> warn(String msg) {
        return restResult(null, 601, msg);
    }

    /**
     * 返回警告消息
     *
     * @param msg  返回消息
     * @param data 数据对象
     * @return 警告消息
     */
    public static <T> R<T> warn(String msg, T data) {
        return restResult(data, 601, msg);
    }

    private static <T> R<T> restResult(T data, int code, String msg) {
        return new R<>(code, msg, data);
    }

    public static <T> Boolean isError(R<T> ret) {
        return !isSuccess(ret);
    }

    public static <T> Boolean isSuccess(R<T> ret) {
        return R.SUCCESS == ret.getCode();
    }

    @Override
    public String toString() {
        return JSON.toJSONString(this);
    }
}