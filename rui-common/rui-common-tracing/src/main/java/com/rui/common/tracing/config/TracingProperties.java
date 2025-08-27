package com.rui.common.tracing.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 链路追踪配置属性
 *
 * @author rui
 */
@Data
@Component
@ConfigurationProperties(prefix = "rui.tracing")
public class TracingProperties {

    /**
     * 是否启用链路追踪
     */
    private boolean enabled = true;

    /**
     * 服务名称
     */
    private String serviceName = "rui-service";

    /**
     * 采样配置
     */
    private SamplingConfig sampling = new SamplingConfig();

    /**
     * 导出配置
     */
    private ExportConfig export = new ExportConfig();

    /**
     * 资源配置
     */
    private ResourceConfig resource = new ResourceConfig();

    /**
     * 传播配置
     */
    private PropagationConfig propagation = new PropagationConfig();

    /**
     * 自定义标签
     */
    private Map<String, String> tags = new HashMap<>();

    /**
     * 忽略的URL模式
     */
    private List<String> ignorePatterns = new ArrayList<>();

    /**
     * 批处理配置
     */
    private BatchConfig batch = new BatchConfig();

    /**
     * 采样配置
     */
    @Data
    public static class SamplingConfig {
        /**
         * 采样类型：always_on, always_off, trace_id_ratio
         */
        private String type = "trace_id_ratio";

        /**
         * 采样率 (0.0 - 1.0)
         */
        private double ratio = 1.0;

        /**
         * 父级采样决策
         */
        private boolean respectParentDecision = true;
    }

    /**
     * 导出配置
     */
    @Data
    public static class ExportConfig {
        /**
         * 导出器类型：jaeger, zipkin, otlp, logging
         */
        private String type = "logging";

        /**
         * 导出端点
         */
        private String endpoint;

        /**
         * 连接超时时间（毫秒）
         */
        private int timeout = 10000;

        /**
         * 压缩类型：none, gzip
         */
        private String compression = "none";

        /**
         * 请求头
         */
        private Map<String, String> headers = new HashMap<>();

        /**
         * Jaeger配置
         */
        private JaegerConfig jaeger = new JaegerConfig();

        /**
         * Zipkin配置
         */
        private ZipkinConfig zipkin = new ZipkinConfig();

        /**
         * OTLP配置
         */
        private OtlpConfig otlp = new OtlpConfig();
    }

    /**
     * Jaeger配置
     */
    @Data
    public static class JaegerConfig {
        private String endpoint = "http://localhost:14268/api/traces";
        private int timeout = 10000;
    }

    /**
     * Zipkin配置
     */
    @Data
    public static class ZipkinConfig {
        private String endpoint = "http://localhost:9411/api/v2/spans";
        private int timeout = 10000;
    }

    /**
     * OTLP配置
     */
    @Data
    public static class OtlpConfig {
        private String endpoint = "http://localhost:4317";
        private int timeout = 10000;
        private Map<String, String> headers = new HashMap<>();
    }

    /**
     * 资源配置
     */
    @Data
    public static class ResourceConfig {
        /**
         * 服务版本
         */
        private String serviceVersion = "1.0.0";

        /**
         * 服务实例ID
         */
        private String serviceInstanceId;

        /**
         * 服务命名空间
         */
        private String serviceNamespace = "rui";

        /**
         * 部署环境
         */
        private String environment = "development";

        /**
         * 自定义资源属性
         */
        private Map<String, String> attributes = new HashMap<>();
    }

    /**
     * 传播配置
     */
    @Data
    public static class PropagationConfig {
        /**
         * 传播器类型：tracecontext, baggage, b3, jaeger, ottrace
         */
        private List<String> types = List.of("tracecontext", "baggage");

        /**
         * B3传播配置
         */
        private B3Config b3 = new B3Config();
    }

    /**
     * B3传播配置
     */
    @Data
    public static class B3Config {
        /**
         * 是否使用单一头部
         */
        private boolean singleHeader = false;
    }

    /**
     * 批处理配置
     */
    @Data
    public static class BatchConfig {
        /**
         * 最大导出批次大小
         */
        private int maxExportBatchSize = 512;

        /**
         * 导出超时时间（毫秒）
         */
        private int exportTimeout = 30000;

        /**
         * 调度延迟（毫秒）
         */
        private int scheduleDelay = 5000;

        /**
         * 最大队列大小
         */
        private int maxQueueSize = 2048;
    }

    /**
     * 获取完整的服务名称
     */
    public String getFullServiceName() {
        if (resource.serviceNamespace != null && !resource.serviceNamespace.isEmpty()) {
            return resource.serviceNamespace + "." + serviceName;
        }
        return serviceName;
    }

    /**
     * 检查是否启用了特定的导出器
     */
    public boolean isExporterEnabled(String exporterType) {
        return enabled && exporterType.equalsIgnoreCase(export.type);
    }

    /**
     * 获取有效的采样率
     */
    public double getEffectiveSamplingRatio() {
        return Math.max(0.0, Math.min(1.0, sampling.ratio));
    }

    /**
     * 检查URL是否应该被忽略
     */
    public boolean shouldIgnoreUrl(String url) {
        return ignorePatterns.stream().anyMatch(pattern -> 
            url.matches(pattern.replace("*", ".*")));
    }
}