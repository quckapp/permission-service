package com.quckapp.permission.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
@ConditionalOnProperty(name = "spring.kafka.bootstrap-servers")
public class KafkaConfig {

    @Value("${app.kafka.topics.permission-events:quckapp.permissions.events}")
    private String permissionEventsTopic;

    @Bean
    public NewTopic permissionEventsTopic() {
        return TopicBuilder.name(permissionEventsTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
