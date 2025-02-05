package com.kakaoscan.profile.domain.controller;

import com.kakaoscan.profile.domain.dto.UserDTO;
import com.kakaoscan.profile.domain.dto.UserHistoryDTO;
import com.kakaoscan.profile.domain.dto.UserRequestUnlockDTO;
import com.kakaoscan.profile.domain.entity.UserRequestUnlock;
import com.kakaoscan.profile.domain.enums.Role;
import com.kakaoscan.profile.domain.model.UseCount;
import com.kakaoscan.profile.domain.service.AccessLimitService;
import com.kakaoscan.profile.domain.service.UserHistoryService;
import com.kakaoscan.profile.domain.service.UserRequestUnlockService;
import com.kakaoscan.profile.domain.service.UserService;
import com.kakaoscan.profile.global.oauth.annotation.UserAttributes;
import com.kakaoscan.profile.global.security.annotation.AdminRoleAccess;
import com.kakaoscan.profile.global.security.annotation.AnyRoleAccess;
import com.kakaoscan.profile.global.security.annotation.GuestRoleAccess;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
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

    private final UserHistoryService userHistoryService;

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
    @GuestRoleAccess
    public ModelAndView unlock(@UserAttributes UserDTO attributes) {
        ModelAndView mv = new ModelAndView("unlock");

        UserRequestUnlock userRequestUnlock = userRequestUnlockService.findByEmail(attributes.getEmail());
        if (userRequestUnlock != null) {
            UserRequestUnlockDTO userRequestUnlockDTO = UserRequestUnlockDTO.toDTO(userRequestUnlock);
            mv.addObject("lastRequestUnlock", userRequestUnlockDTO);
        }

        return mv;
    }

    @GetMapping("/admin")
    @AdminRoleAccess
    public ModelAndView admin() {
        ModelAndView mv = new ModelAndView("admin");

        List<UserDTO> users = userService.findByAll().stream()
                .filter(user -> !Role.ADMIN.equals(user.getRole()))
                .map(user -> {
                    if (user.getRequestUnlock() != null) {
                        user.setEmail(user.getEmail() + " *");
                    }
                    return UserDTO.toDTO(user);
                })
                .collect(Collectors.toList());

        mv.addObject("users", users);

        return mv;
    }

    @GetMapping("/history")
    @AnyRoleAccess
    public ModelAndView history(@UserAttributes UserDTO attributes) {
        ModelAndView mv = new ModelAndView("history");

        List<UserHistoryDTO> historyDTOS = userHistoryService.getHistory(attributes.getEmail());

        mv.addObject("historyDTOS", historyDTOS);

        return mv;
    }

    @GetMapping("/alarm")
    @AnyRoleAccess
    public ModelAndView alarm(@UserAttributes UserDTO attributes) {
        ModelAndView mv = new ModelAndView("alarm");

        return mv;
    }
}
