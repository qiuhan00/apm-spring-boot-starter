# apm-spring-boot-starter
基于 opentelemetry 手动埋点上报调用链路信息

## 1. Quick Start
### 1.1 add maven dependency
```
        <dependency>
            <groupId>com.open</groupId>
            <artifactId>apm-spring-boot-starter</artifactId>
            <version>1.0.0</version>
        </dependency>
```

### 1.2 yml配置开关
```
arms:
  endpoint: http://xxxxxxxxxx.aliyuncs.com:8090/ #接入点配置，未配置默认 http://xxxxxxxx.aliyuncs.com:8090/
  key: xxxxxxxx #鉴权token，未配置默认 xxxxxx
  trace:
    enabled: true # 是否开启链路监控
  aop:
    enabled: true # 是否启用 aop
  pointExpression: # 切点配置，仅在 arms.aop.enabled=true 时生效
    - com.cfang.payment.service.*.*(..)
    - com.cfang.payment.controller.*.*(..)
```
以上配置信息中，切点信息由具体应用决定

## 2. 自定义信息上报
### 2.1 注入 tracer
```
    @Autowired
    Tracer tracer;
```
### 2.2 上报信息
```
    Span span = tracer.spanBuilder("test_span_name")
            .setParent(Context.current().with(Span.current()))
            .startSpan();
    try (Scope scope = span.makeCurrent()){
        // do your logic
        // ...
    }catch (Exception e){
        span.setStatus(StatusCode.ERROR, e.getMessage());
        throw e;
    }finally {
        span.end();
    }
```

## 3. Supported
目前1.0.0版本，仅支持：
- 使用tomcat容器的spring-boot-web项目
- feign的okhttp远程调用
- mybatis的sql执行上传

## 4. Feature
- undertow容器支持
- httpclient、httpurlconnection支持
- hibernate、jpa支持