package com.kakaoscan.profile.domain.service;

import com.kakaoscan.profile.domain.dto.UserHistoryDTO;
import com.kakaoscan.profile.domain.entity.UserHistory;
import com.kakaoscan.profile.domain.model.ScanResult;
import com.kakaoscan.profile.domain.repository.UserHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Log4j2
@Service
@RequiredArgsConstructor
public class UserHistoryService {

    private static final String IMAGE_URL_FORMAT = "%s/%s.mp4.jpg";
    private static final String VIDEO_URL_FORMAT = "%s/%s.mp4";

    private final UserHistoryRepository userHistoryRepository;

    @Transactional
    public void updateHistory(String email, String phoneNumber, String message) {
        Optional<UserHistory> optionalUserHistory = userHistoryRepository.findByEmailAndPhoneNumber(email, phoneNumber);
        UserHistory userHistory = optionalUserHistory.orElseGet(UserHistory::new);

        userHistory.update(email, phoneNumber, message);
        userHistoryRepository.save(userHistory);
    }

    @Transactional(readOnly = true)
    public List<UserHistoryDTO> getHistory(String email) {
        Optional<List<UserHistory>> optionalUserHistories = userHistoryRepository.findByEmailOrderByModifyDtDesc(email);
        if (optionalUserHistories.isEmpty()) {
            return null;
        }

        List<UserHistory> userHistories = optionalUserHistories.get();
        List<UserHistoryDTO> userHistoryDTOS = userHistories.stream()
                .map(userHistory -> {
                    UserHistoryDTO userHistoryDTO = UserHistoryDTO.toDTO((userHistory));

                    ScanResult scanResult = userHistoryDTO.getScanResult();

                    List<ScanResult.ImageUrl> imageUrlList = scanResult.getImageUrlList();
                    imageUrlList.forEach(v -> {
                        v.setUrl(String.format(IMAGE_URL_FORMAT, scanResult.getHost() + v.getDir(), v.getName()));
                    });

                    List<ScanResult.ImageUrl> bgImageUrlList = scanResult.getBgImageUrlList();
                    bgImageUrlList.forEach(v -> {
                        v.setUrl(String.format(IMAGE_URL_FORMAT, scanResult.getHost() + v.getDir(), v.getName()));
                    });

                    List<ScanResult.VideoUrl> videoUrlList = scanResult.getVideoUrlList();
                    videoUrlList.forEach(v -> {
                        v.setUrl(String.format(VIDEO_URL_FORMAT, scanResult.getHost() + v.getDir(), v.getName()));
                    });

                    userHistoryDTO.setScanResult(scanResult);

                    return userHistoryDTO;
                })
                .collect(Collectors.toList());

        return userHistoryDTOS;

    }

    @Transactional(readOnly = true)
    public boolean isScannedHistory(String phoneNumber) {
        Optional<List<UserHistory>> optionalUserHistories = userHistoryRepository.findByPhoneNumber(phoneNumber);
        if (optionalUserHistories.isPresent()) {
            List<UserHistory> userHistories = optionalUserHistories.get();
            return userHistories.stream()
                    .anyMatch(userHistory -> phoneNumber.equals(userHistory.getPhoneNumber()));
        }
        return false;
    }

    @Transactional(readOnly = true)
    public List<UserHistoryDTO> getAllHistory() {
        List<UserHistory> userHistories = userHistoryRepository.findAll();

        return userHistories.stream()
                .map(UserHistoryDTO::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    @Scheduled(cron = "0 0 0 * * *") // 매일 자정에 실행
    public void deleteOldHistory() {
        Optional<List<UserHistory>> optionalUserHistories = userHistoryRepository.findByModifyDtBefore(LocalDateTime.now().minusDays(7));
        if (optionalUserHistories.isPresent()) {
            List<UserHistory> userHistories = optionalUserHistories.get();
            int deletedCount = userHistories.size();
            userHistoryRepository.deleteAll(userHistories);

            log.info("scheduled.. delete old history : {}", deletedCount);
        }else {
            log.info("scheduled.. not found old history");
        }
    }

}
