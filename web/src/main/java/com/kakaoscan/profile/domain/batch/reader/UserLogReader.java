package com.kakaoscan.profile.domain.batch.reader;

import com.kakaoscan.profile.domain.batch.userlog.UserLogQueue;
import com.kakaoscan.profile.domain.entity.UserLog;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.ItemReader;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserLogReader implements ItemReader<UserLog> {

    private final UserLogQueue userLogQueue;

    @Override
    public UserLog read() throws Exception {
        if (userLogQueue.size() == 0) {
            return null;
        }
        return userLogQueue.pop();
    }
}
