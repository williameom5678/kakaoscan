package com.kakaoscan.profile.domain.batch.userlog;

import com.kakaoscan.profile.domain.entity.UserLog;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentLinkedQueue;

@Profile("dev")
@Component
@RequiredArgsConstructor
public class UserLogQueueMemory implements UserLogQueueService {
    private static final ConcurrentLinkedQueue<UserLog> userLogs = new ConcurrentLinkedQueue<>();

    @Override
    public void push(UserLog userLog) {
        userLogs.add(userLog);
    }

    @Override
    public UserLog pop() {
        return userLogs.poll();
    }

    @Override
    public long size() {
        return userLogs.size();
    }
}
