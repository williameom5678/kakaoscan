package com.kakaoscan.profile.domain.repository;

import com.kakaoscan.profile.domain.entity.UserLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserLogsRepository extends JpaRepository<UserLog, Long> {
}
