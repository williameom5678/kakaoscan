package com.kakaoscan.profile.domain.controller.api;

import com.kakaoscan.profile.domain.service.CacheService;
import com.kakaoscan.profile.domain.validator.annotation.CheckKey;
import com.kakaoscan.profile.domain.validator.annotation.Phone;
import com.kakaoscan.profile.global.security.annotation.BasicRoleAccess;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@Validated
public class PhoneNumberCacheController extends ApiBaseController {

    private final CacheService cacheService;

    @GetMapping("/cache")
    @BasicRoleAccess
    public boolean readCache(@RequestParam(required = false) @Phone String phoneNumber) {
        return cacheService.isEnabledPhoneNumber(phoneNumber);
    }

    @PostMapping("/cache")
    public boolean updateCache(@RequestParam(required = false) @Phone String phoneNumber,
                               @RequestParam @CheckKey String key) {

        return cacheService.updatePhoneNumberCache(phoneNumber);
    }
}
