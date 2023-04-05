package com.kakaoscan.profile.domain.batch.reader;

import com.kakaoscan.profile.domain.entity.UserLog;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.ItemReader;
import org.springframework.stereotype.Component;

import static com.kakaoscan.profile.domain.batch.userlog.UserLogQueue.getLogList;

@Component
@RequiredArgsConstructor
public class UserLogReader implements ItemReader<UserLog> {

    @Override
    public UserLog read() throws Exception {
        if (getLogList().isEmpty()) {
            return null;
        }
        return getLogList().poll();
    }
}
