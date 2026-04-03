package com.geros.backend.policy;

import com.geros.backend.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PasswordHistoryRepository extends JpaRepository<PasswordHistory, Long> {

    @Query("SELECT h FROM PasswordHistory h WHERE h.user = :user ORDER BY h.changedAt DESC")
    List<PasswordHistory> findByUserOrderByChangedAtDesc(@Param("user") User user);
}
