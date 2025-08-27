package com.rui.common.config.event;

import org.springframework.context.ApplicationEvent;

/**
 * 配置变更事件
 *
 * @author rui
 * @since 1.0.0
 */
public class ConfigChangeEvent extends ApplicationEvent {

    /**
     * 配置键
     */
    private final String configKey;

    /**
     * 旧值
     */
    private final Object oldValue;

    /**
     * 新值
     */
    private final Object newValue;

    /**
     * 变更类型
     */
    private final ChangeType changeType;

    /**
     * 变更时间戳
     */
    private final long changeTimestamp;

    public ConfigChangeEvent(Object source, String configKey, Object oldValue, Object newValue, ChangeType changeType) {
        super(source);
        this.configKey = configKey;
        this.oldValue = oldValue;
        this.newValue = newValue;
        this.changeType = changeType;
        this.changeTimestamp = System.currentTimeMillis();
    }

    public String getConfigKey() {
        return configKey;
    }

    public Object getOldValue() {
        return oldValue;
    }

    public Object getNewValue() {
        return newValue;
    }

    public ChangeType getChangeType() {
        return changeType;
    }

    public long getChangeTimestamp() {
        return changeTimestamp;
    }

    /**
     * 变更类型枚举
     */
    public enum ChangeType {
        /**
         * 新增
         */
        ADD,
        /**
         * 更新
         */
        UPDATE,
        /**
         * 删除
         */
        DELETE
    }

    @Override
    public String toString() {
        return "ConfigChangeEvent{" +
                "configKey='" + configKey + '\'' +
                ", oldValue=" + oldValue +
                ", newValue=" + newValue +
                ", changeType=" + changeType +
                ", changeTimestamp=" + changeTimestamp +
                '}';
    }
}