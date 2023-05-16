package com.kakaoscan.profile.domain.messagebot.config;

import com.kakaoscan.profile.domain.model.MessageBotKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MessageBotKeyConfig {

    @Value("${telegram.token}")
    private String telegramToken;

    @Value("${telegram.chat-id}")
    private String telegramChatId;

    @Bean
    public MessageBotKey messageBotKey() {
        return MessageBotKey.builder()
                .telegramToken(telegramToken)
                .telegramChatId(telegramChatId)
                .build();
    }
}
