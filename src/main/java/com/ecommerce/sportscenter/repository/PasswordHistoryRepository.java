package com.ecommerce.sportscenter.repository;

import com.ecommerce.sportscenter.entity.PasswordHistory;
import com.ecommerce.sportscenter.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PasswordHistoryRepository extends JpaRepository<PasswordHistory, Long> {
    
    @Query("SELECT ph FROM PasswordHistory ph WHERE ph.user = :user ORDER BY ph.createdAt DESC")
    List<PasswordHistory> findByUserOrderByCreatedAtDesc(@Param("user") User user);
    
    @Query("SELECT ph FROM PasswordHistory ph WHERE ph.user = :user ORDER BY ph.createdAt DESC LIMIT 5")
    List<PasswordHistory> findTop5ByUserOrderByCreatedAtDesc(@Param("user") User user);
    
    void deleteByUser(User user);
}
