package com.demo.kafka.utils.config;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.converter.StringJsonMessageConverter;
import org.springframework.util.backoff.FixedBackOff;

import java.util.Map;

/**
 * Auto-configuration for Kafka consumer infrastructure shared across all services.
 *
 * Runs BEFORE Spring Boot's KafkaAutoConfiguration so our kafkaListenerContainerFactory
 * takes priority (Spring Boot's @ConditionalOnMissingBean then skips creating its own).
 *
 * Creates its own ConsumerFactory with StringDeserializer + earliest offset reset,
 * so services don't need spring.kafka.consumer.* properties to get a working listener.
 */
@Configuration
@AutoConfigureBefore(KafkaAutoConfiguration.class)
@ConditionalOnProperty("spring.kafka.bootstrap-servers")
public class KafkaConsumerConfig {

    private static final Logger logger = LoggerFactory.getLogger(KafkaConsumerConfig.class);

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    /**
     * Dedicated KafkaTemplate using ByteArraySerializer for the DeadLetterPublishingRecoverer.
     * Forwards original raw bytes to .dlq topics without re-wrapping as JSON.
     */
    @Bean
    @ConditionalOnMissingBean(name = "dltKafkaTemplate")
    public KafkaTemplate<byte[], byte[]> dltKafkaTemplate() {
        var props = Map.<String, Object>of(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers,
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class,
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class
        );
        return new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(props));
    }

    @Bean
    @ConditionalOnMissingBean(CommonErrorHandler.class)
    public CommonErrorHandler kafkaErrorHandler(KafkaTemplate<byte[], byte[]> dltKafkaTemplate) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(dltKafkaTemplate,
                (ConsumerRecord<?, ?> record, Exception ex) -> {
                    logger.error("Sending to DLQ: topic={}, error={}", record.topic(), ex.getMessage(), ex);
                    return new org.apache.kafka.common.TopicPartition(record.topic() + ".dlq", record.partition());
                });
        return new DefaultErrorHandler(recoverer, new FixedBackOff(1000L, 2));
    }

    /**
     * Container factory using StringJsonMessageConverter: infers target type from the
     * @KafkaListener method parameter, no __TypeId__ header required.
     *
     * Owns its ConsumerFactory directly (StringDeserializer + earliest) so services
     * don't need spring.kafka.consumer.value-deserializer / auto-offset-reset properties.
     */
    @Bean
    @ConditionalOnMissingBean(name = "kafkaListenerContainerFactory")
    public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory(
            CommonErrorHandler kafkaErrorHandler) {
        var consumerProps = Map.<String, Object>of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers,
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest"
        );
        var factory = new ConcurrentKafkaListenerContainerFactory<String, String>();
        factory.setConsumerFactory(new DefaultKafkaConsumerFactory<>(consumerProps));
        factory.setCommonErrorHandler(kafkaErrorHandler);
        factory.setRecordMessageConverter(new StringJsonMessageConverter());
        return factory;
    }
}
