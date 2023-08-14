# zeus-infra-apm
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
  endpoint: http://tracing-analysis-dc-sh.aliyuncs.com:8090/ #接入点配置，未配置默认 http://tracing-analysis-dc-sh.aliyuncs.com:8090/
  key: fzy881oles@5fa24010b7d7269_fzy881oles@53df7ad2afe8301 #鉴权token，未配置默认 fzy881oles@5fa24010b7d7269_fzy881oles@53df7ad2afe8301
  trace:
    enabled: true # 是否开启链路监控
  aop:
    enabled: true # 是否启用 aop
  pointExpression: # 切点配置，仅在 arms.aop.enabled=true 时生效
    - com.shuinfo.decathlon.payment.service.*.*(..)
    - com.shuinfo.decathlon.payment.controller.*.*(..)
```
以上配置信息中，切点信息由具体应用决定

## 2. Supported
目前1.0.0版本，仅支持：
- 使用tomcat容器的spring-boot-web项目
- feign的okhttp远程调用
- mybatis的sql执行上传

## 3. Feature
- undertow容器支持
- httpclient、httpurlconnection支持
- hibernate、jpa支持