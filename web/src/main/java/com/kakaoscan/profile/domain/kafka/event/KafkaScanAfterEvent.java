package com.kakaoscan.profile.domain.kafka.event;

import lombok.Getter;

import java.util.Map;

@Getter
public class KafkaScanAfterEvent extends KafkaEvent {
    private String phoneNumber;
    private String scanResultJson;

    public KafkaScanAfterEvent() {
        super(new Object());
    }

    public KafkaScanAfterEvent(Map<String, Object> source) {
        super(new Object());
        super.email = ((String) source.get("email"));
        this.phoneNumber = (String) source.get("phoneNumber");
        this.scanResultJson = (String) source.get("scanResultJson");
    }
}
