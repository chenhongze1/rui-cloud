package com.rui.common.tracing.config;

import com.rui.common.tracing.aspect.TracingAspect;
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
import io.opentelemetry.extension.trace.propagation.B3Propagator;
import io.opentelemetry.extension.trace.propagation.JaegerPropagator;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes;
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
        return new TracingManager(tracingProperties, openTelemetry);
    }

    /**
     * 配置TracingAspect
     */
    @Bean
    @ConditionalOnClass(name = "org.aspectj.lang.annotation.Aspect")
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
        Resource.Builder resourceBuilder = Resource.getDefault().toBuilder()
                .put(ResourceAttributes.SERVICE_NAME, tracingProperties.getFullServiceName())
                .put(ResourceAttributes.SERVICE_VERSION, tracingProperties.getResource().getServiceVersion())
                .put(ResourceAttributes.DEPLOYMENT_ENVIRONMENT, tracingProperties.getResource().getEnvironment());

        // 添加服务实例ID
        if (tracingProperties.getResource().getServiceInstanceId() != null) {
            resourceBuilder.put(ResourceAttributes.SERVICE_INSTANCE_ID, 
                    tracingProperties.getResource().getServiceInstanceId());
        }

        // 添加自定义资源属性
        tracingProperties.getResource().getAttributes().forEach(resourceBuilder::put);

        // 添加自定义标签
        tracingProperties.getTags().forEach(resourceBuilder::put);

        return resourceBuilder.build();
    }

    /**
     * 构建TracerProvider
     */
    private SdkTracerProvider buildTracerProvider(Resource resource) {
        SdkTracerProvider.Builder tracerProviderBuilder = SdkTracerProvider.builder()
                .setResource(resource)
                .setSampler(buildSampler());

        // 添加Span处理器
        SpanExporter spanExporter = buildSpanExporter();
        if (spanExporter != null) {
            BatchSpanProcessor batchProcessor = BatchSpanProcessor.builder(spanExporter)
                    .setMaxExportBatchSize(tracingProperties.getBatch().getMaxExportBatchSize())
                    .setExportTimeout(Duration.ofMillis(tracingProperties.getBatch().getExportTimeout()))
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
                return Sampler.create(1.0);
            case "always_off":
                return Sampler.create(0.0);
            case "trace_id_ratio":
            default:
                return Sampler.create(tracingProperties.getEffectiveSamplingRatio());
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
        JaegerGrpcSpanExporter.Builder builder = JaegerGrpcSpanExporter.builder()
                .setEndpoint(export.getJaeger().getEndpoint())
                .setTimeout(export.getJaeger().getTimeout(), TimeUnit.MILLISECONDS);

        export.getHeaders().forEach(builder::addHeader);
        
        return builder.build();
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
        OtlpGrpcSpanExporter.Builder builder = OtlpGrpcSpanExporter.builder()
                .setEndpoint(export.getOtlp().getEndpoint())
                .setTimeout(Duration.ofMillis(export.getOtlp().getTimeout()));

        export.getOtlp().getHeaders().forEach(builder::addHeader);
        
        return builder.build();
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
                    if (tracingProperties.getPropagation().getB3().isSingleHeader()) {
                        propagators.add(B3Propagator.injectingSingleHeader());
                    } else {
                        propagators.add(B3Propagator.injectingMultiHeaders());
                    }
                    break;
                case "jaeger":
                    propagators.add(JaegerPropagator.getInstance());
                    break;
                default:
                    log.warn("Unknown propagator type: {}", type);
            }
        }
        
        return ContextPropagators.create(
                TextMapPropagator.composite(propagators.toArray(new TextMapPropagator[0]))
        );
    }
}