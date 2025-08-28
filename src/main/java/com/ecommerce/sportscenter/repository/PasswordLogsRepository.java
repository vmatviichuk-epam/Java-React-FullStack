package com.ecommerce.sportscenter.repository;

import com.ecommerce.sportscenter.entity.PasswordLogs;
import com.ecommerce.sportscenter.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PasswordLogsRepository extends JpaRepository<PasswordLogs, Long> {
    List<PasswordLogs> findByUserOrderByTimestampDesc(User user);
}
