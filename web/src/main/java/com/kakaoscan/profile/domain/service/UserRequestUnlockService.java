package com.kakaoscan.profile.domain.service;

import com.kakaoscan.profile.domain.entity.UserRequestUnlock;
import com.kakaoscan.profile.domain.messagebot.service.MessageBotService;
import com.kakaoscan.profile.domain.repository.UserRequestUnlockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserRequestUnlockService {

    private final UserRequestUnlockRepository userRequestUnlockRepository;

    private final MessageBotService messageBotService;

    @Transactional
    public void updateUnlockMessage(String email, String message) {
         userRequestUnlockRepository.save(UserRequestUnlock.builder()
                 .email(email)
                 .message(message)
                 .build());

        messageBotService.send(String.format("[email]\n%s\n[message]\n%s", email, message));
    }

    @Transactional(readOnly = true)
    public UserRequestUnlock findByEmail(String email) {
        return userRequestUnlockRepository.findById(email).orElse(null);
    }
}
