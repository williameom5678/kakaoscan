package com.kakaoscan.profile.domain.batch.processor;

import com.kakaoscan.profile.domain.entity.UserLog;
import lombok.NonNull;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

@Component
public class UserLogProcessor implements ItemProcessor<UserLog, UserLog> {

    @Override
    public UserLog process(@NonNull UserLog item) throws Exception {
        return item;
    }
}
