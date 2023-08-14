package com.open.http;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.TextMapSetter;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * @author cfang
 * @date 2023/08/03 14:47
 * @desc
 */
@Slf4j
public class OkHttpInterceptor implements Interceptor {

    @Autowired
    Tracer tracer;
    @Autowired
    OpenTelemetry openTelemetry;

    @NotNull
    @Override
    public Response intercept(@NotNull Chain chain) throws IOException {
        Request request = chain.request();
        Span span = tracer.spanBuilder(request.method())
                .setParent(Context.current().with(Span.current()))
                .startSpan();
        try(Scope scope = span.makeCurrent()) {
            TextMapSetter<Request.Builder> setter =
                new TextMapSetter<Request.Builder>() {
                    @Override
                    public void set(Request.Builder carrier, String key, String value) {
                        // Insert the context as Header
                        carrier.header(key, value);
                    }
                };
            Context context = Context.current().with(Span.current());
            Request.Builder builder = request.newBuilder();
            openTelemetry.getPropagators()
                    .getTextMapPropagator()
                    .inject(context, builder, setter);
            return chain.proceed(builder.build());
        }catch (Exception e){
            span.setStatus(StatusCode.ERROR, e.getMessage());
            throw e;
        }finally {
            span.end();
        }
    }
}
