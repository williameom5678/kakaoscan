package com.kakaoscan.profile.domain.kafka.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kakaoscan.profile.domain.kafka.event.KafkaEvent;

import java.io.IOException;
import java.util.Map;

public class KafkaMessageDeserializer {
    private static final ObjectMapper mapper = new ObjectMapper();

    public static <T extends KafkaEvent> T deserialize(Map<String, Object> map, Class<T> clazz) throws IOException {
        return mapper.convertValue(map, clazz);
    }
}
