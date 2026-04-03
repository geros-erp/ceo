package com.geros.backend.user;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByRoles_Id(Long roleId);

    @Query(value = "SELECT * FROM auth.users u WHERE " +
           "(:search IS NULL OR LOWER(u.username) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(u.first_name) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(u.last_name) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "AND (:isActive IS NULL OR u.is_active = :isActive) " +
           "AND (:roleId IS NULL OR EXISTS (SELECT 1 FROM auth.user_roles ur WHERE ur.user_id = u.id AND ur.role_id = :roleId)) " +
           "AND (:status IS NULL OR " +
           "     (:status = 'ACTIVE' AND u.is_active = true AND u.locked_at IS NULL) OR " +
           "     (:status = 'INACTIVE' AND u.is_active = false) OR " +
           "     (:status = 'LOCKED' AND u.locked_at IS NOT NULL)) " +
           "ORDER BY u.id ASC",
           countQuery = "SELECT COUNT(*) FROM auth.users u WHERE " +
           "(:search IS NULL OR LOWER(u.username) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(u.first_name) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(u.last_name) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "AND (:isActive IS NULL OR u.is_active = :isActive) " +
           "AND (:roleId IS NULL OR EXISTS (SELECT 1 FROM auth.user_roles ur WHERE ur.user_id = u.id AND ur.role_id = :roleId)) " +
           "AND (:status IS NULL OR " +
           "     (:status = 'ACTIVE' AND u.is_active = true AND u.locked_at IS NULL) OR " +
           "     (:status = 'INACTIVE' AND u.is_active = false) OR " +
           "     (:status = 'LOCKED' AND u.locked_at IS NOT NULL))",
           nativeQuery = true)
    Page<User> findAllWithFilters(@Param("search") String search,
                                  @Param("isActive") Boolean isActive,
                                  @Param("roleId") Long roleId,
                                  @Param("status") String status,
                                  Pageable pageable);

    @Query("SELECT u FROM User u WHERE u.isActive = true AND " +
           "((u.lastLoginAt IS NOT NULL AND u.lastLoginAt < :threshold) OR " +
           "(u.lastLoginAt IS NULL AND u.createdAt < :threshold))")
       List<User> findInactiveUsers(@Param("threshold") LocalDateTime threshold);
}
