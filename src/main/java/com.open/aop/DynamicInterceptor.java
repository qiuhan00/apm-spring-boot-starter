package com.open.aop;

import com.open.utils.IPUtil;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;
import lombok.extern.slf4j.Slf4j;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.servlet.HandlerMapping;

import javax.servlet.http.HttpServletRequest;

/**
 * @author cfang
 * @date 2023/08/11 10:13
 * @desc
 */
@Slf4j
public class DynamicInterceptor implements MethodInterceptor {

    @Autowired
    Tracer tracer;

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        if(invocation.getThis().getClass().isAnnotationPresent(Controller.class) || invocation.getThis().getClass().isAnnotationPresent(RestController.class)){
            return processorController(invocation);
        }
        if(invocation.getThis().getClass().isAnnotationPresent(Service.class)){
            return processorService(invocation);
        }
        return invocation.proceed();
    }

    private Object processorController(MethodInvocation invocation) throws Throwable{
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = (HttpServletRequest) requestAttributes.resolveReference(RequestAttributes.REFERENCE_REQUEST);
        String mappingPath = (String) request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        String spanName = String.join(" ", request.getMethod(), mappingPath);
        Span span = tracer.spanBuilder(spanName).startSpan();
        try (Scope scope = span.makeCurrent()){
            span.setAttribute(SemanticAttributes.HTTP_TARGET, request.getRequestURI());
            span.setAttribute(SemanticAttributes.HTTP_CLIENT_IP, IPUtil.getIPAddress(request));
            span.setAttribute(SemanticAttributes.HTTP_SCHEME, request.getScheme());
            span.setAttribute(SemanticAttributes.HTTP_METHOD, request.getMethod());
            span.setAttribute(SemanticAttributes.NET_HOST_NAME, request.getServerName());
            span.setAttribute(SemanticAttributes.NET_HOST_PORT, request.getServerPort());
            span.setAttribute(SemanticAttributes.HTTP_ROUTE, request.getServletPath());
            span.setAttribute(SemanticAttributes.THREAD_NAME, Thread.currentThread().getName());
            span.setAttribute(SemanticAttributes.THREAD_ID, Thread.currentThread().getId());
            return invocation.proceed();
        }catch (Exception e){
            span.setStatus(StatusCode.ERROR, e.getMessage());
            throw e;
        }finally {
            span.end();
        }
    }

    private Object processorService(MethodInvocation invocation) throws Throwable{
        String clsName = invocation.getThis().getClass().getSimpleName();
        String methodName = invocation.getMethod().getName();
        Span span = tracer.spanBuilder(String.join(".", clsName, methodName))
                .setParent(Context.current().with(Span.current()))
                .startSpan();
        try (Scope scope = span.makeCurrent()){
            return invocation.proceed();
        }catch (Exception e){
            span.setStatus(StatusCode.ERROR, e.getMessage());
            throw e;
        }finally {
            span.end();
        }
    }

}
