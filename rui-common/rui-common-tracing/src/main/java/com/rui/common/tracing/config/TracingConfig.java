package com.rui.common.tracing.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 链路追踪配置
 *
 * @author rui
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "rui.tracing")
public class TracingConfig {

    /**
     * 是否启用链路追踪
     */
    private boolean enabled = true;

    /**
     * 服务名称
     */
    private String serviceName = "rui-cloud";

    /**
     * 服务版本
     */
    private String serviceVersion = "1.0.0";

    /**
     * 采样率 (0.0 - 1.0)
     */
    private double samplingRate = 1.0;

    /**
     * 导出器配置
     */
    private Exporter exporter = new Exporter();

    /**
     * 资源配置
     */
    private Resource resource = new Resource();

    /**
     * 批处理配置
     */
    private Batch batch = new Batch();

    @Data
    public static class Exporter {
        /**
         * 导出器类型 (jaeger, zipkin, otlp)
         */
        private String type = "zipkin";

        /**
         * 导出端点
         */
        private String endpoint = "http://localhost:9411/api/v2/spans";

        /**
         * 连接超时时间(毫秒)
         */
        private int connectTimeout = 10000;

        /**
         * 读取超时时间(毫秒)
         */
        private int readTimeout = 10000;
    }

    @Data
    public static class Resource {
        /**
         * 服务命名空间
         */
        private String serviceNamespace = "rui";

        /**
         * 服务实例ID
         */
        private String serviceInstanceId;

        /**
         * 部署环境
         */
        private String deploymentEnvironment = "development";
    }

    @Data
    public static class Batch {
        /**
         * 批处理大小
         */
        private int maxExportBatchSize = 512;

        /**
         * 导出超时时间(毫秒)
         */
        private int exportTimeout = 30000;

        /**
         * 调度延迟(毫秒)
         */
        private int scheduleDelay = 5000;

        /**
         * 最大队列大小
         */
        private int maxQueueSize = 2048;
    }
}