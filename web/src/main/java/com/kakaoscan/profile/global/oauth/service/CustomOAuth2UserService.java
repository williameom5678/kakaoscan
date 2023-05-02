package com.kakaoscan.profile.global.oauth.service;

import com.kakaoscan.profile.domain.dto.UserLogDTO;
import com.kakaoscan.profile.domain.entity.User;
import com.kakaoscan.profile.domain.enums.KafkaEventType;
import com.kakaoscan.profile.domain.enums.LogType;
import com.kakaoscan.profile.domain.enums.Role;
import com.kakaoscan.profile.domain.kafka.service.KafkaProducerService;
import com.kakaoscan.profile.domain.repository.UserRepository;
import com.kakaoscan.profile.global.oauth.OAuthAttributes;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static com.kakaoscan.profile.utils.HttpRequestUtils.getRemoteAddress;

@RequiredArgsConstructor
@Service
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    @Value("${md5.encryption.salt-key}")
    private String saltKey;

    private final HttpServletRequest request;

    private final UserRepository userRepository;

    private final KafkaProducerService producerService;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate = new DefaultOAuth2UserService();
        OAuth2User oAuth2User = delegate.loadUser(userRequest);

        // 현재 로그인 진행 중인 서비스를 구분하는 코드
        String registrationId = userRequest
                .getClientRegistration()
                .getRegistrationId();

        // oauth2 로그인 진행 시 키가 되는 필드값
        String userNameAttributeName = userRequest.getClientRegistration()
                .getProviderDetails()
                .getUserInfoEndpoint()
                .getUserNameAttributeName();

        OAuthAttributes attributes = OAuthAttributes
                .of(registrationId, userNameAttributeName, oAuth2User.getAttributes());

        Optional<User> userOptional = userRepository.findById(attributes.getEmail());
        User user = userOptional.orElse(User.builder()
                .email(attributes.getEmail())
                .role(Role.GUEST)
                .build());
        if (userOptional.isEmpty()) {
            userRepository.save(user);

            Map<String, Object> map = new HashMap<>();
            map.put("email", attributes.getEmail());
            map.put("json", UserLogDTO.builder()
                    .type(LogType.REGISTER.name())
                    .remoteAddress(getRemoteAddress(request))
                    .build());
            producerService.send(KafkaEventType.RECORD_LOG_EVENT, map);
        }

//        attributes.setRole(user.getRole());

        return new DefaultOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority(
                        user.getRole().getKey())),
                        attributes.getAttributes(),
                        attributes.getNameAttributeKey()
                );
    }
}