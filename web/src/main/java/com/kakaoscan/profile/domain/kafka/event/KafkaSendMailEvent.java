package com.kakaoscan.profile.domain.kafka.event;

import lombok.Getter;

import java.util.Map;

@Getter
public class KafkaSendMailEvent extends KafkaEvent {
    public KafkaSendMailEvent() {
        super(new Object());
    }

    public KafkaSendMailEvent(Map<String, Object> source) {
        super(new Object());
        super.email = ((String) source.get("email"));
    }
}