package com.kakaoscan.profile.domain.controller.api;

import com.kakaoscan.profile.domain.service.UserRequestService;
import com.kakaoscan.profile.domain.validator.annotation.CheckKey;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@Validated
public class UserRequestController extends ApiBaseController {

    private final UserRequestService userRequestService;

    @PostMapping("/use")
    public boolean updateUseCount(@RequestParam String email, @RequestParam String remoteAddress, @RequestParam @CheckKey String key) {
        return userRequestService.updateUseCount(email, remoteAddress);
    }
}
