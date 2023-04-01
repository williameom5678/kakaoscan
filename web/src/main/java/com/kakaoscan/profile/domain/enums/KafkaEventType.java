package com.kakaoscan.profile.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum KafkaEventType {
    SCAN_AFTER_EVENT("SCAN_AFTER_EVENT"),
    SEND_MAIL_EVENT("SEND_MAIL_EVENT");

    private final String value;
}
