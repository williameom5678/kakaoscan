package com.kakaoscan.profile.domain.batch.userlog;

import com.kakaoscan.profile.domain.entity.UserLog;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserLogQueue implements UserLogQueueService {
    private static final String USER_LOG_LIST_KEY = "UserLogList";

    private final RedisTemplate<String, UserLog> redisTemplate;

    @Override
    public void push(UserLog userLog) {
        redisTemplate.opsForList().rightPush(USER_LOG_LIST_KEY, userLog);
    }

    @Override
    public UserLog pop() {
        return redisTemplate.opsForList().leftPop(USER_LOG_LIST_KEY);
    }

    @Override
    public long size() {
        Long size = redisTemplate.opsForList().size(USER_LOG_LIST_KEY);
        return size != null ? size : 0;
    }
}
