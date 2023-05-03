package com.kakaoscan.profile.global.security.handler;

import com.kakaoscan.profile.domain.dto.UserDTO;
import com.kakaoscan.profile.domain.dto.UserLogDTO;
import com.kakaoscan.profile.domain.enums.KafkaEventType;
import com.kakaoscan.profile.domain.enums.LogType;
import com.kakaoscan.profile.domain.enums.Role;
import com.kakaoscan.profile.domain.kafka.service.KafkaProducerService;
import com.kakaoscan.profile.domain.service.UserRequestService;
import com.kakaoscan.profile.global.oauth.OAuthAttributes;
import com.kakaoscan.profile.global.session.instance.SessionManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static com.kakaoscan.profile.global.session.instance.SessionManager.SESSION_FORMAT;
import static com.kakaoscan.profile.utils.GenerateUtils.StrToMD5;
import static com.kakaoscan.profile.utils.HttpRequestUtils.getRemoteAddress;

@Log4j2
@RequiredArgsConstructor
@Component
public class CustomAuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final RedirectStrategy redirectStrategy = new DefaultRedirectStrategy();

    private final SessionManager sessionManager;

    private final UserRequestService userRequestService;

    private final KafkaProducerService producerService;

    @Override
    public void onAuthenticationSuccess(final HttpServletRequest request, final HttpServletResponse response, final Authentication authentication) throws IOException {
        try {
            DefaultOAuth2User oauth2User = (DefaultOAuth2User) authentication.getPrincipal();
            OAuthAttributes oa = OAuthAttributes.of(null, null, oauth2User.getAttributes());

            Collection<? extends GrantedAuthority> authorities = oauth2User.getAuthorities();
            String authority = authorities.stream()
                    .findFirst()
                    .map(GrantedAuthority::getAuthority)
                    .orElse(null);

            if (authority == null) {
                return;
            }

            Role role = Arrays.stream(Role.values())
                    .filter(v -> v.getKey().equals(authority))
                    .findFirst()
                    .orElse(null);
            oa.setRole(role);

            sessionManager.add(String.format(SESSION_FORMAT, request.getSession().getId()), UserDTO.toDTO(oa));

            String remoteAddress = getRemoteAddress(request);
            // 오늘 사용 데이터가 없으면 0으로 초기화
            if (userRequestService.getTodayUseCount(oa.getEmail()) == -1) {
                userRequestService.initUseCount(oa.getEmail(), StrToMD5(remoteAddress, ""));
            }

            Map<String, Object> map = new HashMap<>();
            map.put("email", oa.getEmail());
            map.put("json", UserLogDTO.builder()
                    .type(LogType.LOGIN.name())
                    .remoteAddress(remoteAddress)
                    .build());
            producerService.send(KafkaEventType.RECORD_LOG_EVENT, map);

        } finally {
            setDefaultTargetUrl("/");
            redirectStrategy.sendRedirect(request, response, getDefaultTargetUrl());
        }
    }
}

