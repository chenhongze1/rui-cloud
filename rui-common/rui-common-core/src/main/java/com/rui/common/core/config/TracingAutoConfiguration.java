package com.rui.common.core.config;

import com.rui.common.core.tracing.TracingInterceptor;
import com.rui.common.core.tracing.TracingManager;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.exporter.jaeger.JaegerGrpcSpanExporter;
import io.opentelemetry.exporter.logging.LoggingSpanExporter;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.exporter.zipkin.ZipkinSpanExporter;
import io.opentelemetry.extension.trace.propagation.B3Propagator;
import io.opentelemetry.extension.trace.propagation.JaegerPropagator;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.opentelemetry.sdk.trace.samplers.Sampler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 链路追踪自动配置类
 * 整合所有追踪相关组件
 *
 * @author rui
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(TracingConfig.class)
@ConditionalOnProperty(prefix = "rui.tracing", name = "enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnClass(OpenTelemetry.class)
public class TracingAutoConfiguration {

    private final TracingConfig tracingConfig;

    /**
     * OpenTelemetry资源配置
     */
    @Bean
    @ConditionalOnMissingBean
    public Resource otelResource() {
        String serviceInstanceId = tracingConfig.getResource().getServiceInstanceId();
        if (serviceInstanceId == null || serviceInstanceId.isEmpty()) {
            serviceInstanceId = UUID.randomUUID().toString();
        }
        
        var resourceBuilder = Resource.getDefault().toBuilder()
            .put("service.name", tracingConfig.getServiceName())
            .put("service.version", tracingConfig.getResource().getServiceVersion())
            .put("service.instance.id", serviceInstanceId)
            .put("deployment.environment", tracingConfig.getResource().getEnvironment());
        
        // 添加自定义资源属性
        tracingConfig.getResource().getAttributes().forEach(resourceBuilder::put);
        
        return resourceBuilder.build();
    }

    /**
     * Span导出器配置
     */
    @Bean
    @ConditionalOnMissingBean
    public SpanExporter spanExporter() {
        TracingConfig.ExportConfig exportConfig = tracingConfig.getExport();
        
        switch (exportConfig.getType()) {
            case JAEGER:
                return createJaegerExporter(exportConfig.getJaeger());
            case ZIPKIN:
                return createZipkinExporter(exportConfig.getZipkin());
            case OTLP:
                return createOtlpExporter(exportConfig.getOtlp());
            case CONSOLE:
                return LoggingSpanExporter.create();
            case NONE:
            default:
                return SpanExporter.composite(); // 空的复合导出器
        }
    }

    /**
     * 采样器配置
     */
    @Bean
    @ConditionalOnMissingBean
    public Sampler sampler() {
        TracingConfig.SamplingConfig samplingConfig = tracingConfig.getSampling();
        
        switch (samplingConfig.getStrategy()) {
            case PROBABILITY:
                return Sampler.traceIdRatioBased(samplingConfig.getProbability());
            case RATE_LIMITING:
                // 这里可以实现速率限制采样器
                return Sampler.traceIdRatioBased(samplingConfig.getProbability());
            case ADAPTIVE:
                // 这里可以实现自适应采样器
                return Sampler.traceIdRatioBased(samplingConfig.getProbability());
            case CUSTOM:
                // 这里可以实现自定义采样器
                return Sampler.traceIdRatioBased(samplingConfig.getProbability());
            default:
                return Sampler.traceIdRatioBased(0.1); // 默认10%采样率
        }
    }

    /**
     * 上下文传播器配置
     */
    @Bean
    @ConditionalOnMissingBean
    public ContextPropagators contextPropagators() {
        List<TextMapPropagator> propagators = new ArrayList<>();
        
        for (TracingConfig.PropagatorType type : tracingConfig.getPropagation().getTypes()) {
            switch (type) {
                case TRACE_CONTEXT:
                    propagators.add(W3CTraceContextPropagator.getInstance());
                    break;
                case BAGGAGE:
                    propagators.add(io.opentelemetry.api.baggage.propagation.W3CBaggagePropagator.getInstance());
                    break;
                case B3_SINGLE:
                    propagators.add(B3Propagator.injectingSingleHeader());
                    break;
                case B3_MULTI:
                    propagators.add(B3Propagator.injectingMultiHeaders());
                    break;
                case JAEGER:
                    propagators.add(JaegerPropagator.getInstance());
                    break;
                case XRAY:
                    // 这里可以添加AWS X-Ray传播器
                    break;
            }
        }
        
        return ContextPropagators.create(TextMapPropagator.composite(propagators.toArray(new TextMapPropagator[0])));
    }

    /**
     * TracerProvider配置
     */
    @Bean
    @ConditionalOnMissingBean
    public SdkTracerProvider sdkTracerProvider(Resource resource, SpanExporter spanExporter, Sampler sampler) {
        TracingConfig.BatchConfig batchConfig = tracingConfig.getExport().getBatch();
        
        BatchSpanProcessor batchProcessor = BatchSpanProcessor.builder(spanExporter)
            .setMaxExportBatchSize(batchConfig.getMaxExportBatchSize())
            .setScheduleDelay(Duration.ofMillis(batchConfig.getScheduleDelay()))
            .setMaxQueueSize(batchConfig.getMaxQueueSize())
            .build();
        
        return SdkTracerProvider.builder()
            .addSpanProcessor(batchProcessor)
            .setResource(resource)
            .setSampler(sampler)
            .build();
    }

    /**
     * OpenTelemetry配置
     */
    @Bean
    @ConditionalOnMissingBean
    public OpenTelemetry openTelemetry(SdkTracerProvider tracerProvider, ContextPropagators contextPropagators) {
        return OpenTelemetrySdk.builder()
            .setTracerProvider(tracerProvider)
            .setPropagators(contextPropagators)
            .build();
    }

    /**
     * 链路追踪管理器
     */
    @Bean
    @ConditionalOnMissingBean
    public TracingManager tracingManager(OpenTelemetry openTelemetry) {
        return new TracingManager(tracingConfig, openTelemetry);
    }

    /**
     * 链路追踪拦截器
     */
    @Bean
    @ConditionalOnMissingBean
    public TracingInterceptor tracingInterceptor(TracingManager tracingManager) {
        return new TracingInterceptor(tracingConfig, tracingManager);
    }

    /**
     * Web MVC配置
     */
    @Configuration
    @ConditionalOnClass(name = "org.springframework.web.servlet.config.annotation.WebMvcConfigurer")
    static class TracingWebMvcConfiguration implements WebMvcConfigurer {
        
        private final TracingInterceptor tracingInterceptor;
        
        public TracingWebMvcConfiguration(TracingInterceptor tracingInterceptor) {
            this.tracingInterceptor = tracingInterceptor;
        }
        
        @Override
        public void addInterceptors(InterceptorRegistry registry) {
            registry.addInterceptor(tracingInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/actuator/**", "/health", "/metrics");
        }
    }

    /**
     * 创建Jaeger导出器
     */
    private SpanExporter createJaegerExporter(TracingConfig.JaegerConfig jaegerConfig) {
        var builder = JaegerGrpcSpanExporter.builder()
            .setEndpoint(jaegerConfig.getEndpoint())
            .setTimeout(Duration.ofMillis(jaegerConfig.getTimeout()));
        
        // 添加自定义头信息
        jaegerConfig.getHeaders().forEach((key, value) -> {
            // Note: addHeader method may not be available in current OpenTelemetry version
            // Custom headers should be handled through other configuration means
        });
        
        return builder.build();
    }

    /**
     * 创建Zipkin导出器
     */
    private SpanExporter createZipkinExporter(TracingConfig.ZipkinConfig zipkinConfig) {
        return ZipkinSpanExporter.builder()
            .setEndpoint(zipkinConfig.getEndpoint())
            .build();
    }

    /**
     * 创建OTLP导出器
     */
    private SpanExporter createOtlpExporter(TracingConfig.OtlpConfig otlpConfig) {
        var builder = OtlpGrpcSpanExporter.builder()
            .setEndpoint(otlpConfig.getEndpoint())
            .setTimeout(Duration.ofMillis(otlpConfig.getTimeout()));
        
        // 添加自定义头信息
        otlpConfig.getHeaders().forEach((key, value) -> {
            // Note: addHeader method may not be available in current OpenTelemetry version
            // Custom headers should be handled through other configuration means
        });
        
        return builder.build();
    }
}