package com.kakaoscan.profile.domain.kafka.event;

import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@Getter
public class KafkaMessageBotEvent extends KafkaEvent{
    private String message;

    public KafkaMessageBotEvent() {
        super(new Object());
    }

    public KafkaMessageBotEvent(Map<String, Object> source) {
        super(new Object());
        super.email = ((String) source.get("email"));
        this.message = ((String) source.get("message"));
    }

    @Builder
    public KafkaMessageBotEvent(String email, String message) {
        super(new Object());
        super.email = email;
        this.message = message;
    }
}
