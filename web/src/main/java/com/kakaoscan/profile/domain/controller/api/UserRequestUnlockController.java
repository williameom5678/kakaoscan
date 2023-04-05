package com.kakaoscan.profile.domain.controller.api;

import com.kakaoscan.profile.domain.dto.UserDTO;
import com.kakaoscan.profile.domain.dto.UserLogDTO;
import com.kakaoscan.profile.domain.dto.UserRequestUnlockDTO;
import com.kakaoscan.profile.domain.entity.UserRequestUnlock;
import com.kakaoscan.profile.domain.enums.KafkaEventType;
import com.kakaoscan.profile.domain.enums.LogType;
import com.kakaoscan.profile.domain.kafka.service.KafkaProducerService;
import com.kakaoscan.profile.domain.service.UserRequestUnlockService;
import com.kakaoscan.profile.global.oauth.annotation.UserAttributes;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static com.kakaoscan.profile.utils.HttpRequestUtils.getRemoteAddress;

@RequiredArgsConstructor
@RestController
@Log4j2
public class UserRequestUnlockController extends ApiBaseController {

    private final UserRequestUnlockService userRequestUnlockService;

    private final KafkaProducerService producerService;

    @PostMapping("/unlock")
    @PreAuthorize("hasRole('ROLE_GUEST')")
    public ResponseEntity<?> updateUnlockMessage(@Valid @RequestBody UserRequestUnlockDTO userRequestUnlockDTO,
                                                 @UserAttributes UserDTO attributes, HttpServletRequest request) {

        userRequestUnlockService.updateUnlockMessage(attributes.getEmail(), userRequestUnlockDTO.getMessage());

        Map<String, Object> map = new HashMap<>();
        map.put("email", attributes.getEmail());
        map.put("json", UserLogDTO.builder()
                .type(LogType.UNLOCK.name())
                .message(userRequestUnlockDTO.getMessage())
                .remoteAddress(getRemoteAddress(request))
                .build());
        producerService.send(KafkaEventType.RECORD_LOG_EVENT, map);

        return ResponseEntity.ok().build();
    }

    @GetMapping("/unlock")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<?> getUnlockMessage(@RequestParam String email) {

        Optional<UserRequestUnlock> userRequestUnlockOptional = Optional.ofNullable(userRequestUnlockService.findByEmail(email));

        UserRequestUnlockDTO userRequestUnlockDTO = userRequestUnlockOptional
                .map(UserRequestUnlockDTO::toDTO)
                .orElseGet(UserRequestUnlockDTO::new);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(userRequestUnlockDTO);
    }
}
