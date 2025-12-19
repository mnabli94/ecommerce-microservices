package com.demo.security.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import java.util.List;

@ConfigurationProperties(prefix = "security")
public record AuthSecurityProperties(List<String> permitAll, String authIssuer, String authAudience) {
}
