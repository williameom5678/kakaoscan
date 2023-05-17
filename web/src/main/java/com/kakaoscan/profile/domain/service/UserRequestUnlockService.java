package com.kakaoscan.profile.domain.service;

import com.kakaoscan.profile.domain.entity.UserRequestUnlock;
import com.kakaoscan.profile.domain.enums.KafkaEventType;
import com.kakaoscan.profile.domain.kafka.service.KafkaProducerService;
import com.kakaoscan.profile.domain.repository.UserRequestUnlockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class UserRequestUnlockService {

    private final UserRequestUnlockRepository userRequestUnlockRepository;

    private final KafkaProducerService producerService;

    @Transactional
    public void updateUnlockMessage(String email, String message) {
        userRequestUnlockRepository.save(UserRequestUnlock.builder()
                 .email(email)
                 .message(message)
                 .build());

        Map<String, Object> map = new HashMap<>();
        map.put("message", String.format("[email]\n%s\n[message]\n%s", email, message));
        producerService.send(KafkaEventType.MESSAGE_BOT_EVENT, map);
    }

    @Transactional(readOnly = true)
    public UserRequestUnlock findByEmail(String email) {
        return userRequestUnlockRepository.findById(email).orElse(null);
    }
}
