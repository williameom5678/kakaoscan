package com.kakaoscan.profile.domain.batch.userlog;

import com.kakaoscan.profile.domain.entity.UserLog;

import java.util.concurrent.ConcurrentLinkedQueue;

public class UserLogQueue {
    private static final ConcurrentLinkedQueue<UserLog> logList = new ConcurrentLinkedQueue<>();

    public static ConcurrentLinkedQueue<UserLog> getLogList() {
        return logList;
    }

    public static void addUserLog(UserLog userLog) {
        logList.add(userLog);
    }
}
