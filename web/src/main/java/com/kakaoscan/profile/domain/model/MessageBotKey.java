package com.kakaoscan.profile.domain.model;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MessageBotKey {
    public String telegramToken;
    public String telegramChatId;
}
