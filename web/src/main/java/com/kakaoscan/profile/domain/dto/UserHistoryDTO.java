package com.kakaoscan.profile.domain.dto;

import com.kakaoscan.profile.domain.entity.UserHistory;
import com.kakaoscan.profile.domain.model.ScanResult;
import lombok.*;
import lombok.extern.log4j.Log4j2;

import java.time.LocalDateTime;

import static com.kakaoscan.profile.utils.DateUtils.getBeforeDiffToString;

@Log4j2
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserHistoryDTO {
    private String phoneNumber;
    private ScanResult scanResult;
    private LocalDateTime modifyDt;
    private String remainingPeriod;

    public static UserHistoryDTO toDTO(UserHistory entity) {
        try {
            return UserHistoryDTO.builder()
                    .phoneNumber(entity.getPhoneNumber())
                    .scanResult(ScanResult.deserialize(entity.getMessage()))
                    .modifyDt(entity.getModifyDt())
                    .remainingPeriod(getBeforeDiffToString(entity.getModifyDt(), LocalDateTime.now()) + " 전")
                    .build();
        }catch (Exception e) {
            log.error("get all history error");
            throw new RuntimeException(e);
        }
    }
}
