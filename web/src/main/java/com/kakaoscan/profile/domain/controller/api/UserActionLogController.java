package com.kakaoscan.profile.domain.controller.api;

import com.kakaoscan.profile.domain.dto.UserDTO;
import com.kakaoscan.profile.domain.dto.UserLogDTO;
import com.kakaoscan.profile.domain.enums.ApiErrorCase;
import com.kakaoscan.profile.domain.enums.KafkaEventType;
import com.kakaoscan.profile.domain.enums.LogType;
import com.kakaoscan.profile.domain.exception.ApiException;
import com.kakaoscan.profile.domain.kafka.service.KafkaProducerService;
import com.kakaoscan.profile.global.oauth.annotation.UserAttributes;
import com.kakaoscan.profile.global.security.annotation.AnyRoleAccess;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

import static com.kakaoscan.profile.utils.HttpRequestUtils.getRemoteAddress;
import static org.apache.logging.log4j.util.Strings.isBlank;

@RequiredArgsConstructor
@RestController
public class UserActionLogController extends ApiBaseController {

    private final KafkaProducerService producerService;

    @PostMapping("/log")
    @AnyRoleAccess
    public void log(@RequestBody(required = false) UserLogDTO userLogDTO, @UserAttributes UserDTO attributes, HttpServletRequest request) {
        if (userLogDTO == null) {
            throw new ApiException(ApiErrorCase.INVALID_PARAMETER);
        }

        Map<String, Object> map = new HashMap<>();
        map.put("email", attributes.getEmail());
        map.put("json", UserLogDTO.builder()
                .type(isBlank(userLogDTO.getSearch()) ? LogType.ACTION.name() : LogType.SEARCH.name())
                .url(isBlank(userLogDTO.getUrl()) ? null : userLogDTO.getUrl())
                .search(isBlank(userLogDTO.getSearch()) ? null : userLogDTO.getSearch())
                .view(isBlank(userLogDTO.getView()) ? null : userLogDTO.getView())
                .remoteAddress(getRemoteAddress(request))
                .build());
        producerService.send(KafkaEventType.RECORD_LOG_EVENT, map);
    }
}
