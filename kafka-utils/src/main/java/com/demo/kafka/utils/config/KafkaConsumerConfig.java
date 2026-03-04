package com.demo.kafka.utils.config;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.converter.StringJsonMessageConverter;
import org.springframework.util.backoff.FixedBackOff;

import java.util.Map;

@Configuration
@ConditionalOnBean(ConsumerFactory.class)
public class KafkaConsumerConfig {

    private static final Logger logger = LoggerFactory.getLogger(KafkaConsumerConfig.class);

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    /**
     * Dedicated KafkaTemplate using ByteArraySerializer for the DeadLetterPublishingRecoverer.
     * The recoverer forwards the original raw bytes to the .dlq topic — using JsonSerializer
     * here would re-wrap the bytes as JSON and inject a spurious __TypeId__:[B header.
     */
    @Bean
    public KafkaTemplate<byte[], byte[]> dltKafkaTemplate() {
        var props = Map.<String, Object>of(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers,
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class,
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class
        );
        return new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(props));
    }

    @Bean
    public CommonErrorHandler kafkaErrorHandler(KafkaTemplate<byte[], byte[]> dltKafkaTemplate) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(dltKafkaTemplate,
                (ConsumerRecord<?, ?> record, Exception ex) -> {
                    logger.error("Sending to DLQ: topic={}, error={}", record.topic(), ex.getMessage(), ex);
                    return new org.apache.kafka.common.TopicPartition(record.topic() + ".dlq", record.partition());
                });
        return new DefaultErrorHandler(recoverer, new FixedBackOff(1000L, 2));
    }

    /**
     * Custom container factory using StringJsonMessageConverter.
     * Infers the target type from the @KafkaListener method parameter — no __TypeId__
     * header required. This decouples the consumer from the producer's serializer config.
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory(
            ConsumerFactory<String, String> consumerFactory,
            CommonErrorHandler kafkaErrorHandler) {
        var factory = new ConcurrentKafkaListenerContainerFactory<String, String>();
        factory.setConsumerFactory(consumerFactory);
        factory.setCommonErrorHandler(kafkaErrorHandler);
        factory.setRecordMessageConverter(new StringJsonMessageConverter());
        return factory;
    }
}
