package com.open.config;

import com.open.aop.ArmsConfig;
import com.open.http.OkHttpInterceptor;
import com.open.sql.SQLInterceptor;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import org.apache.ibatis.session.SqlSessionFactory;
import org.jetbrains.annotations.NotNull;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import java.net.InetAddress;

/**
 * @author cfang
 * @date 2023/08/11 16:01
 * @desc
 */
@Slf4j
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnProperty(prefix = "arms.trace", name = "enabled", havingValue = "true")
@Import({OkHttpInterceptor.class, ArmsConfig.class})
@EnableConfigurationProperties(AopProperties.class)
public class TraceAutoConfiguration {

    @Bean
    @ConditionalOnClass(OkHttpClient.class)
    public OkHttpClient.Builder builder(OkHttpInterceptor interceptor){
        return new OkHttpClient.Builder()
                .retryOnConnectionFailure(true)
                .addInterceptor(interceptor);
    }

    @Bean
    @ConditionalOnClass(SqlSessionFactory.class)

    public SQLInterceptor sqlInterceptor(){
        return new SQLInterceptor();
    }

    @Bean
    public OpenTelemetry openTelemetry(@NotNull ApplicationContext context){
        String env = context.getEnvironment().getActiveProfiles()[0];
        String serviceName = "default-java-service-name";
        String hostName = "default-java-host-name";
        try {
            hostName = InetAddress.getLocalHost().getHostName();
            /**
             * 获取配置文件中 spring.application.name 属性
             */
            serviceName = context.getId();
        }catch (Exception e){
            log.error("获取服务信息异常，msg:{}", e.getMessage());
        }
        Resource resource = Resource.getDefault()
                .merge(Resource.create(Attributes.of(
                        ResourceAttributes.SERVICE_NAME, serviceName + "_" + env,
                        ResourceAttributes.HOST_NAME, hostName
                )));

        SdkTracerProvider sdkTracerProvider = SdkTracerProvider.builder()
                .addSpanProcessor(BatchSpanProcessor.builder(OtlpGrpcSpanExporter.builder()
                        .setEndpoint(context.getEnvironment().getProperty("arms.endpoint", "http://tracing-analysis-dc-sh.aliyuncs.com:8090/"))
                        .addHeader("Authentication", context.getEnvironment().getProperty("arms.key", "fzy881oles@5fa24010b7d7269_fzy881oles@53df7ad2afe8301"))
                        .build()).build())
                .setResource(resource)
                .build();

        return OpenTelemetrySdk.builder()
                .setTracerProvider(sdkTracerProvider)
                .setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance()))
                .buildAndRegisterGlobal();
    }

    @Bean
    public Tracer tracer(OpenTelemetry openTelemetry){
        return openTelemetry.getTracer("default-trace", "1.0.0");
    }
}
