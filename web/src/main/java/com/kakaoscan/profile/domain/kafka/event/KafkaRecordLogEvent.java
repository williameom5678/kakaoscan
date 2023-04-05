package com.kakaoscan.profile.domain.kafka.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kakaoscan.profile.domain.dto.UserLogDTO;
import lombok.Getter;

import java.util.Map;

@Getter
public class KafkaRecordLogEvent extends KafkaEvent {
    private static final ObjectMapper mapper = new ObjectMapper();

    private String email;
    private String json;

    public KafkaRecordLogEvent() {
        super(new Object());
    }

    public KafkaRecordLogEvent(Map<String, Object> source) {
        super(new Object());
        this.email = (String) source.get("email");
        this.json = UserLogDTO.serializer(mapper.convertValue(source.get("json"), UserLogDTO.class));
    }
}