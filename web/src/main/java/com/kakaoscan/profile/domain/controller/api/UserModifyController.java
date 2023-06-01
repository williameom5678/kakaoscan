package com.kakaoscan.profile.domain.controller.api;

import com.kakaoscan.profile.domain.dto.UserModifyDTO;
import com.kakaoscan.profile.domain.service.UserService;
import com.kakaoscan.profile.global.security.annotation.AdminRoleAccess;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@Log4j2
public class UserModifyController extends ApiBaseController {

    private final UserService userService;

    @PostMapping("/modify")
    @AdminRoleAccess
    public ResponseEntity<?> modifyUser(@RequestBody UserModifyDTO userModifyDTO) {
        userService.modifyUserRole(userModifyDTO);

        return ResponseEntity.ok().build();
    }
}
