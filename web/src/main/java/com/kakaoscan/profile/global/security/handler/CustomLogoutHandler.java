package com.kakaoscan.profile.global.security.handler;

import com.kakaoscan.profile.domain.dto.UserLogDTO;
import com.kakaoscan.profile.domain.enums.KafkaEventType;
import com.kakaoscan.profile.domain.enums.LogType;
import com.kakaoscan.profile.domain.kafka.service.KafkaProducerService;
import com.kakaoscan.profile.global.session.instance.SessionManager;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.stereotype.Component;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

import static com.kakaoscan.profile.global.session.instance.SessionManager.SESSION_FORMAT;
import static com.kakaoscan.profile.global.session.instance.SessionManager.SESSION_KEY;
import static com.kakaoscan.profile.utils.HttpRequestUtils.getCookie;
import static com.kakaoscan.profile.utils.HttpRequestUtils.getRemoteAddress;

@Component
@RequiredArgsConstructor
public class CustomLogoutHandler implements LogoutHandler {

    private final SessionManager sessionManager;

    private final KafkaProducerService producerService;

    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        getCookie(request)
                .ifPresent(cookie -> sessionManager.deleteValue(String.format(SESSION_FORMAT, cookie.getValue())));

        Cookie cookie = new Cookie(SESSION_KEY, "");
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);

        DefaultOAuth2User oauth2User = (DefaultOAuth2User) authentication.getPrincipal();
        if (oauth2User == null) {
            return;
        }

        String email = oauth2User.getAttribute("email");
        String remoteAddress = getRemoteAddress(request);

        Map<String, Object> map = new HashMap<>();
        map.put("email", email);
        map.put("json", UserLogDTO.builder()
                .type(LogType.LOGOUT.name())
                .remoteAddress(remoteAddress)
                .build());
        producerService.send(KafkaEventType.RECORD_LOG_EVENT, map);
    }
}
