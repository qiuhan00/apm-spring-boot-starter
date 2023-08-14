package com.open.aop;

import com.open.config.AopProperties;
import org.springframework.aop.Pointcut;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.Assert;

/**
 * @author cfang
 * @date 2023/08/11 10:17
 * @desc
 */
@Configuration
@ConditionalOnProperty(name = "arms.aop.enabled", havingValue = "true")
public class ArmsConfig {

    @Bean
    public Pointcut customPointCut(AopProperties properties) {
        boolean isNull = null == properties.getPointExpression() || 0 == properties.getPointExpression().size();
        Assert.isTrue(!isNull, "Field properties pointcut expression that not found, pls add pointExpression.");
        DynamicPointcut dynamicPointcut = new DynamicPointcut();
        StringBuilder expression = new StringBuilder();
        properties.getPointExpression().forEach(it -> {
            expression.append("execution(public * ").append(it).append(")").append("||");
        });
        dynamicPointcut.setExpression(expression.toString().substring(0, expression.toString().length()-2));
        return dynamicPointcut;
    }

    @Bean
    public DynamicAdvice getAdvice () {
        return new DynamicAdvice();
    }

    @Bean
    public DefaultPointcutAdvisor defaultPointcutAdvisor(Pointcut pointcut, DynamicAdvice dynamicAdvice) {
        DefaultPointcutAdvisor defaultPointcutAdvisor = new DefaultPointcutAdvisor();
        defaultPointcutAdvisor.setPointcut(pointcut);
        defaultPointcutAdvisor.setAdvice(dynamicAdvice);
        return defaultPointcutAdvisor;
    }
}
