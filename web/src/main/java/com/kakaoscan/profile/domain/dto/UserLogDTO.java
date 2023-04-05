package com.kakaoscan.profile.domain.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserLogDTO {
    private String type;
    private String search;
    private String remoteAddress;
    private String url;
    private String view;
    private String message;

    public static String serializer(UserLogDTO logDTO) {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        try {
            return objectMapper.writeValueAsString(logDTO);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
