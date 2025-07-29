package com.flightmanagement.flightarchiveservice.config;

import com.flightmanagement.flightarchiveservice.event.FlightEvent;
import com.flightmanagement.flightarchiveservice.event.ReferenceEvent;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id}")
    private String groupId;

    // Flight Events Consumer Factory
    @Bean
    public ConsumerFactory<String, FlightEvent> flightEventConsumerFactory() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configs.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        configs.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        configs.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);

        // Key deserializer
        configs.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

        // Value deserializer with error handling
        configs.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        configs.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class.getName());

        // JSON Deserializer settings
        configs.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        configs.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
        configs.put(JsonDeserializer.VALUE_DEFAULT_TYPE, FlightEvent.class.getName());

        return new DefaultKafkaConsumerFactory<>(configs);
    }

    // Reference Events Consumer Factory
    @Bean
    public ConsumerFactory<String, ReferenceEvent> referenceEventConsumerFactory() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configs.put(ConsumerConfig.GROUP_ID_CONFIG, groupId + "-reference");
        configs.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        configs.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);

        configs.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        configs.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        configs.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class.getName());

        configs.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        configs.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
        configs.put(JsonDeserializer.VALUE_DEFAULT_TYPE, ReferenceEvent.class.getName());

        return new DefaultKafkaConsumerFactory<>(configs);
    }

    // Flight Events Listener Container Factory
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, FlightEvent> flightEventKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, FlightEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(flightEventConsumerFactory());
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);
        factory.setCommonErrorHandler(new org.springframework.kafka.listener.DefaultErrorHandler());
        return factory;
    }

    // Reference Events Listener Container Factory
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, ReferenceEvent> referenceEventKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, ReferenceEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(referenceEventConsumerFactory());
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);
        factory.setCommonErrorHandler(new org.springframework.kafka.listener.DefaultErrorHandler());
        return factory;
    }
}