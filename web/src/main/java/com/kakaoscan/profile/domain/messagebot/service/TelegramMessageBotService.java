package com.kakaoscan.profile.domain.messagebot.service;

import com.kakaoscan.profile.domain.model.MessageBotKey;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.SendMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TelegramMessageBotService implements MessageBotService {

    private final MessageBotKey messageBotKey;

    @Override
    public void send(String message) {
        TelegramBot bot = new TelegramBot(messageBotKey.getTelegramToken());

        SendMessage request = new SendMessage(messageBotKey.getTelegramChatId(), message);

        bot.execute(request);
    }
}
