package com.geros.backend.policy;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.Set;

public interface ReservedUsernameRepository extends JpaRepository<ReservedUsername, Long> {
    @Query("SELECT LOWER(r.username) FROM ReservedUsername r")
    Set<String> findAllUsernamesLower();
    
    boolean existsByUsernameIgnoreCase(String username);
}