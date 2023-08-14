package com.open.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author cfang
 * @date 2023/08/11 09:53
 * @desc
 */
@Data
@Component
@ConfigurationProperties(prefix = "arms")
public class AopProperties {

    private boolean httpEnabled;
    private boolean serviceEnabled;
    private boolean controllerEnabled;
    private boolean sqlEnabled;
    private List<String> pointExpression;
    private String endpoint;
    private String token;
}
