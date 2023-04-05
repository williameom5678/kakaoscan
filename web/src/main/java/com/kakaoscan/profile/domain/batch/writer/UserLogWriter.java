package com.kakaoscan.profile.domain.batch.writer;

import com.kakaoscan.profile.domain.batch.config.BatchConfig;
import com.kakaoscan.profile.domain.entity.UserLog;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;

@Component
public class UserLogWriter implements ItemWriter<UserLog> {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public void write(List<? extends UserLog> items) throws Exception {
        for (int i = 0; i < items.size(); i++) {
            UserLog userLog = items.get(i);

            entityManager.persist(userLog);
            if ((i + 1) % BatchConfig.BATCH_SIZE == 0) {
                entityManager.flush();
                entityManager.clear();
            }
        }

        entityManager.flush();
    }
}
