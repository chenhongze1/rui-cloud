package com.rui.common.config.listener;

import com.rui.common.config.event.ConfigChangeEvent;
import org.springframework.context.ApplicationListener;

/**
 * 配置变更监听器接口
 *
 * @author rui
 * @since 1.0.0
 */
public interface ConfigChangeListener extends ApplicationListener<ConfigChangeEvent> {

    /**
     * 处理配置变更事件
     *
     * @param event 配置变更事件
     */
    @Override
    void onApplicationEvent(ConfigChangeEvent event);

    /**
     * 获取监听器名称
     *
     * @return 监听器名称
     */
    default String getListenerName() {
        return this.getClass().getSimpleName();
    }

    /**
     * 获取监听器优先级
     * 数值越小优先级越高
     *
     * @return 优先级
     */
    default int getPriority() {
        return 0;
    }

    /**
     * 是否支持异步处理
     *
     * @return true-支持异步，false-同步处理
     */
    default boolean isAsync() {
        return false;
    }
}