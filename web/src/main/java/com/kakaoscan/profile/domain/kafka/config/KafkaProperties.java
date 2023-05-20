package com.kakaoscan.profile.domain.kafka.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Getter
@Component
public class KafkaProperties {
    public static final String TOPIC_EVENT = "kakaoscan";
    public static final String GROUP_EVENT = "group.kakaoscan";

    public static final String TOPIC_SERVER_EVENT = "kakaoscan.server-app";
    public static final String GROUP_SERVER_EVENT = "group.kakaoscan.server-app";

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;
}