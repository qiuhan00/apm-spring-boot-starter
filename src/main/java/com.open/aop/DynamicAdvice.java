package com.open.aop;

import lombok.extern.slf4j.Slf4j;
import org.aopalliance.aop.Advice;
import org.aopalliance.intercept.MethodInterceptor;
import org.springframework.aop.Advisor;
import org.springframework.aop.AfterReturningAdvice;
import org.springframework.aop.MethodBeforeAdvice;
import org.springframework.aop.framework.adapter.AdvisorAdapter;

import java.lang.reflect.Method;

/**
 * @author cfang
 * @date 2023/08/11 10:15
 * @desc
 */
@Slf4j
public class DynamicAdvice extends DynamicInterceptor implements MethodBeforeAdvice, AfterReturningAdvice, AdvisorAdapter {


    @Override
    public void afterReturning(Object returnValue, Method method, Object[] args, Object target) throws Throwable {
    }

    @Override
    public void before(Method method, Object[] args, Object target) throws Throwable {
    }

    @Override
    public boolean supportsAdvice(Advice advice) {
        return true;
    }

    @Override
    public MethodInterceptor getInterceptor(Advisor advisor) {
        return null;
    }
}
