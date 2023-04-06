package com.kakaoscan.profile.domain.batch.userlog;

import com.kakaoscan.profile.domain.entity.UserLog;

public interface UserLogQueueService {
    void push(UserLog userLog);

    UserLog pop();

    long size();
}
