package com.demo.kafka.utils.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurationPackage;

/**
 * Registers the com.demo.kafka.utils package into Spring Boot's AutoConfigurationPackages,
 * so that JPA entity scanning and repository scanning automatically include
 * ProcessedEvent and ProcessedEventRepository without requiring explicit
 * @EntityScan / @EnableJpaRepositories on the application class.
 */
@AutoConfiguration
@AutoConfigurationPackage(basePackages = "com.demo.kafka.utils")
public class KafkaUtilsJpaAutoConfiguration {
}
