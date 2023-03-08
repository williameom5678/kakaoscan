package com.kakaoscan.profile.domain.controller;

import com.kakaoscan.profile.domain.dto.UserDTO;
import com.kakaoscan.profile.domain.dto.UserRequestUnlockDTO;
import com.kakaoscan.profile.domain.entity.UserRequestUnlock;
import com.kakaoscan.profile.domain.model.UseCount;
import com.kakaoscan.profile.domain.service.AccessLimitService;
import com.kakaoscan.profile.domain.service.UserRequestUnlockService;
import com.kakaoscan.profile.domain.service.UserService;
import com.kakaoscan.profile.global.oauth.OAuthAttributes;
import com.kakaoscan.profile.global.oauth.annotation.UserAttributes;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import java.util.List;
import java.util.stream.Collectors;

/**
 * view
 */
@RestController
@RequiredArgsConstructor
public class ViewController {

    @Value("${kakaoscan.all.date.maxcount}")
    private long allLimitCount;

    @Value("${kakaoscan.server.count}")
    private long serverCount;

    private final AccessLimitService accessLimitService;

    private final UserRequestUnlockService userRequestUnlockService;

    private final UserService userService;

    @GetMapping("/")
    public ModelAndView index(@RequestParam(required = false, defaultValue = "") String phoneNumber) {
        ModelAndView mv = new ModelAndView("index");

        UseCount useCount = accessLimitService.getUseCount();
        long remainingCount = (allLimitCount * serverCount) - useCount.getTotalCount();
        if (remainingCount < 0) {
            remainingCount = 0;
        }

        mv.addObject("todayRemainingCount", String.format("%d/%d", remainingCount, allLimitCount * serverCount));
        mv.addObject("phoneNumber", phoneNumber);

        return mv;
    }

    @GetMapping("/req-unlock")
    @PreAuthorize("hasRole('ROLE_GUEST')")
    public ModelAndView unlock(@UserAttributes OAuthAttributes attributes) {
        ModelAndView mv = new ModelAndView("unlock");

        UserRequestUnlock userRequestUnlock = userRequestUnlockService.findByEmail(attributes.getEmail());
        if (userRequestUnlock != null) {
            UserRequestUnlockDTO userRequestUnlockDTO = UserRequestUnlockDTO.toDTO(userRequestUnlock);
            mv.addObject("lastRequestUnlock", userRequestUnlockDTO);
        }

        return mv;
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ModelAndView admin(@UserAttributes OAuthAttributes attributes) {
        ModelAndView mv = new ModelAndView("admin");

        List<UserDTO> users = userService.findByAll().stream()
                .map(UserDTO::toDTO)
                .collect(Collectors.toList());

        mv.addObject("users", users);

        return mv;
    }

    @GetMapping("/test")
    @PreAuthorize("hasRole('ROLE_ADMIN') or (hasRole('ROLE_USER') and !hasRole('ROLE_ADMIN'))")
    public String test() {
        return "test admin";
    }


}
