package com.rui.common.core.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 链路追踪配置
 *
 * @author rui
 */
@Data
@Component
@ConfigurationProperties(prefix = "rui.tracing")
public class TracingConfig {

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
     * 采样配置
     */
    @Data
    public static class SamplingConfig {
        /**
         * 采样率 (0.0 - 1.0)
         */
        private double probability = 0.1;

        /**
         * 每秒最大采样数
         */
        private int maxTracesPerSecond = 100;

        /**
         * 采样策略
         */
        private SamplingStrategy strategy = SamplingStrategy.PROBABILITY;

        /**
         * 自定义采样规则
         */
        private List<SamplingRule> rules = new ArrayList<>();n    }

    /**
     * 导出配置
     */
    @Data
    public static class ExportConfig {
        /**
         * 导出器类型
         */
        private ExporterType type = ExporterType.JAEGER;

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

        /**
         * 控制台配置
         */
        private ConsoleConfig console = new ConsoleConfig();

        /**
         * 批处理配置
         */
        private BatchConfig batch = new BatchConfig();
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
         * 传播器类型
         */
        private List<PropagatorType> types = List.of(PropagatorType.TRACE_CONTEXT, PropagatorType.BAGGAGE);

        /**
         * 自定义传播头
         */
        private Map<String, String> customHeaders = new HashMap<>();
    }

    /**
     * Jaeger配置
     */
    @Data
    public static class JaegerConfig {
        /**
         * Jaeger端点
         */
        private String endpoint = "http://localhost:14268/api/traces";

        /**
         * 连接超时时间(毫秒)
         */
        private int timeout = 10000;

        /**
         * 自定义头信息
         */
        private Map<String, String> headers = new HashMap<>();
    }

    /**
     * Zipkin配置
     */
    @Data
    public static class ZipkinConfig {
        /**
         * Zipkin端点
         */
        private String endpoint = "http://localhost:9411/api/v2/spans";

        /**
         * 连接超时时间(毫秒)
         */
        private int timeout = 10000;
    }

    /**
     * OTLP配置
     */
    @Data
    public static class OtlpConfig {
        /**
         * OTLP端点
         */
        private String endpoint = "http://localhost:4317";

        /**
         * 协议类型
         */
        private OtlpProtocol protocol = OtlpProtocol.GRPC;

        /**
         * 连接超时时间(毫秒)
         */
        private int timeout = 10000;

        /**
         * 自定义头信息
         */
        private Map<String, String> headers = new HashMap<>();
    }

    /**
     * 控制台配置
     */
    @Data
    public static class ConsoleConfig {
        /**
         * 是否启用控制台输出
         */
        private boolean enabled = false;

        /**
         * 输出格式
         */
        private ConsoleFormat format = ConsoleFormat.JSON;
    }

    /**
     * 批处理配置
     */
    @Data
    public static class BatchConfig {
        /**
         * 最大批处理大小
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

    /**
     * 采样规则
     */
    @Data
    public static class SamplingRule {
        /**
         * 规则名称
         */
        private String name;

        /**
         * 服务名称模式
         */
        private String serviceNamePattern;

        /**
         * 操作名称模式
         */
        private String operationNamePattern;

        /**
         * 采样率
         */
        private double probability;

        /**
         * 最大每秒采样数
         */
        private int maxTracesPerSecond;
    }

    /**
     * 采样策略枚举
     */
    public enum SamplingStrategy {
        /**
         * 概率采样
         */
        PROBABILITY,
        /**
         * 速率限制采样
         */
        RATE_LIMITING,
        /**
         * 自适应采样
         */
        ADAPTIVE,
        /**
         * 自定义采样
         */
        CUSTOM
    }

    /**
     * 导出器类型枚举
     */
    public enum ExporterType {
        /**
         * Jaeger
         */
        JAEGER,
        /**
         * Zipkin
         */
        ZIPKIN,
        /**
         * OTLP
         */
        OTLP,
        /**
         * 控制台
         */
        CONSOLE,
        /**
         * 无导出
         */
        NONE
    }

    /**
     * 传播器类型枚举
     */
    public enum PropagatorType {
        /**
         * W3C Trace Context
         */
        TRACE_CONTEXT,
        /**
         * W3C Baggage
         */
        BAGGAGE,
        /**
         * B3 Single
         */
        B3_SINGLE,
        /**
         * B3 Multi
         */
        B3_MULTI,
        /**
         * Jaeger
         */
        JAEGER,
        /**
         * AWS X-Ray
         */
        XRAY
    }

    /**
     * OTLP协议枚举
     */
    public enum OtlpProtocol {
        /**
         * gRPC
         */
        GRPC,
        /**
         * HTTP/protobuf
         */
        HTTP_PROTOBUF,
        /**
         * HTTP/JSON
         */
        HTTP_JSON
    }

    /**
     * 控制台格式枚举
     */
    public enum ConsoleFormat {
        /**
         * JSON格式
         */
        JSON,
        /**
         * 简单格式
         */
        SIMPLE
    }
}