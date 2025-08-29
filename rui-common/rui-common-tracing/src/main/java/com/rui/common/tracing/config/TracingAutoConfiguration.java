package com.rui.common.tracing.config;

import com.rui.common.tracing.aspect.TracingAspect;
import com.rui.common.tracing.context.TracingContext;
import com.rui.common.tracing.interceptor.TracingInterceptor;
import com.rui.common.tracing.manager.TracingManager;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.exporter.jaeger.JaegerGrpcSpanExporter;
import io.opentelemetry.exporter.logging.LoggingSpanExporter;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.exporter.zipkin.ZipkinSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 链路追踪自动配置
 *
 * @author rui
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(TracingProperties.class)
@ConditionalOnProperty(prefix = "rui.tracing", name = "enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnClass(OpenTelemetry.class)
public class TracingAutoConfiguration implements WebMvcConfigurer {

    private final TracingProperties tracingProperties;

    /**
     * 配置OpenTelemetry
     */
    @Bean
    public OpenTelemetry openTelemetry() {
        Resource resource = buildResource();
        SdkTracerProvider tracerProvider = buildTracerProvider(resource);
        ContextPropagators contextPropagators = buildContextPropagators();

        OpenTelemetry openTelemetry = OpenTelemetrySdk.builder()
                .setTracerProvider(tracerProvider)
                .setPropagators(contextPropagators)
                .build();

        log.info("OpenTelemetry initialized with service: {}", tracingProperties.getFullServiceName());
        return openTelemetry;
    }

    /**
     * 配置TracingManager
     */
    @Bean
    public TracingManager tracingManager(OpenTelemetry openTelemetry) {
        return new TracingManager(openTelemetry, tracingProperties, new TracingContext());
    }

    /**
     * 配置TracingAspect
     */
    @Bean
    @ConditionalOnClass(name = "org.aspectj.lang.annotation.Aspect")
    @ConditionalOnProperty(prefix = "rui.tracing.aspect", name = "enabled", havingValue = "false")  // 暂时禁用以隔离问题
    public TracingAspect tracingAspect(TracingManager tracingManager) {
        return new TracingAspect(tracingManager);
    }

    /**
     * 配置TracingInterceptor
     */
    @Bean
    public TracingInterceptor tracingInterceptor(TracingManager tracingManager) {
        return new TracingInterceptor(tracingProperties, tracingManager);
    }

    /**
     * 注册拦截器
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(tracingInterceptor(null))
                .addPathPatterns("/**")
                .excludePathPatterns("/health", "/metrics", "/actuator/**");
    }

    /**
     * 构建Resource
     */
    private Resource buildResource() {
        return Resource.getDefault().toBuilder()
                .put("service.name", tracingProperties.getFullServiceName())
                .put("service.version", tracingProperties.getResource().getServiceVersion())
                .put("deployment.environment", tracingProperties.getResource().getEnvironment())
                .put("service.instance.id", 
                    tracingProperties.getResource().getServiceInstanceId() != null ? 
                    tracingProperties.getResource().getServiceInstanceId() : "unknown")
                .build();
    }

    /**
     * 构建TracerProvider
     */
    private SdkTracerProvider buildTracerProvider(Resource resource) {
        var tracerProviderBuilder = SdkTracerProvider.builder()
                .setResource(resource)
                .setSampler(buildSampler());

        // 添加Span处理器
        SpanExporter spanExporter = buildSpanExporter();
        if (spanExporter != null) {
            BatchSpanProcessor batchProcessor = BatchSpanProcessor.builder(spanExporter)
                    .setMaxExportBatchSize(tracingProperties.getBatch().getMaxExportBatchSize())
                    .setScheduleDelay(Duration.ofMillis(tracingProperties.getBatch().getScheduleDelay()))
                    .setMaxQueueSize(tracingProperties.getBatch().getMaxQueueSize())
                    .build();
            tracerProviderBuilder.addSpanProcessor(batchProcessor);
        }

        return tracerProviderBuilder.build();
    }

    /**
     * 构建采样器
     */
    private Sampler buildSampler() {
        TracingProperties.SamplingConfig sampling = tracingProperties.getSampling();
        
        switch (sampling.getType().toLowerCase()) {
            case "always_on":
                return Sampler.alwaysOn();
            case "always_off":
                return Sampler.alwaysOff();
            case "trace_id_ratio":
            default:
                return Sampler.traceIdRatioBased(tracingProperties.getEffectiveSamplingRatio());
        }
    }

    /**
     * 构建Span导出器
     */
    private SpanExporter buildSpanExporter() {
        TracingProperties.ExportConfig export = tracingProperties.getExport();
        
        switch (export.getType().toLowerCase()) {
            case "jaeger":
                return buildJaegerExporter(export);
            case "zipkin":
                return buildZipkinExporter(export);
            case "otlp":
                return buildOtlpExporter(export);
            case "logging":
            default:
                return LoggingSpanExporter.create();
        }
    }

    /**
     * 构建Jaeger导出器
     */
    private SpanExporter buildJaegerExporter(TracingProperties.ExportConfig export) {
        return JaegerGrpcSpanExporter.builder()
                .setEndpoint(export.getJaeger().getEndpoint())
                .setTimeout(Duration.ofMillis(export.getJaeger().getTimeout()))
                .build();
    }

    /**
     * 构建Zipkin导出器
     */
    private SpanExporter buildZipkinExporter(TracingProperties.ExportConfig export) {
        return ZipkinSpanExporter.builder()
                .setEndpoint(export.getZipkin().getEndpoint())
                .build();
    }

    /**
     * 构建OTLP导出器
     */
    private SpanExporter buildOtlpExporter(TracingProperties.ExportConfig export) {
        return OtlpGrpcSpanExporter.builder()
                .setEndpoint(export.getOtlp().getEndpoint())
                .setTimeout(Duration.ofMillis(export.getOtlp().getTimeout()))
                .build();
    }

    /**
     * 构建上下文传播器
     */
    private ContextPropagators buildContextPropagators() {
        List<TextMapPropagator> propagators = new ArrayList<>();
        
        for (String type : tracingProperties.getPropagation().getTypes()) {
            switch (type.toLowerCase()) {
                case "tracecontext":
                    propagators.add(W3CTraceContextPropagator.getInstance());
                    break;
                case "b3":
                case "jaeger":
                    log.warn("Propagator type {} is not available, using W3C TraceContext instead", type);
                    propagators.add(W3CTraceContextPropagator.getInstance());
                    break;
                default:
                    log.warn("Unknown propagator type: {}, using W3C TraceContext", type);
                    propagators.add(W3CTraceContextPropagator.getInstance());
            }
        }
        
        // 如果没有配置任何propagator，使用默认的W3C TraceContext
        if (propagators.isEmpty()) {
            propagators.add(W3CTraceContextPropagator.getInstance());
        }
        
        return ContextPropagators.create(
                TextMapPropagator.composite(propagators.toArray(new TextMapPropagator[0]))
        );
    }
}