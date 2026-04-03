package com.geros.backend.securitylog;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.time.LocalDateTime;

public interface SecurityLogRepository extends JpaRepository<SecurityLog, Long> {

    long countByCreatedAtBefore(LocalDateTime createdAt);

    long deleteByCreatedAtBefore(LocalDateTime createdAt);

    @Query(value = "SELECT s.* FROM public.security_log s " +
                   "LEFT JOIN auth.users u ON LOWER(u.email) = LOWER(s.target_email) WHERE " +
                   "(:email IS NULL OR LOWER(s.target_email) LIKE LOWER(CONCAT('%', :email, '%'))) AND " +
                   "(:userFrom IS NULL OR LOWER(s.target_email) >= LOWER(:userFrom)) AND " +
                   "(:userTo IS NULL OR LOWER(s.target_email) <= LOWER(:userTo)) AND " +
                   "(:status IS NULL OR " +
                   "     (:status = 'ACTIVE' AND u.is_active = true AND u.locked_at IS NULL) OR " +
                   "     (:status = 'INACTIVE' AND u.is_active = false) OR " +
                   "     (:status = 'LOCKED' AND u.locked_at IS NOT NULL)) AND " +
                   "(:action IS NULL OR s.action = :action) AND " +
                   "(CAST(:createdFrom AS timestamp) IS NULL OR s.created_at >= CAST(:createdFrom AS timestamp)) AND " +
                   "(CAST(:createdTo AS timestamp) IS NULL OR s.created_at <= CAST(:createdTo AS timestamp)) " +
                   "ORDER BY s.created_at DESC",
           countQuery = "SELECT COUNT(*) FROM public.security_log s " +
                        "LEFT JOIN auth.users u ON LOWER(u.email) = LOWER(s.target_email) WHERE " +
                        "(:email IS NULL OR LOWER(s.target_email) LIKE LOWER(CONCAT('%', :email, '%'))) AND " +
                        "(:userFrom IS NULL OR LOWER(s.target_email) >= LOWER(:userFrom)) AND " +
                        "(:userTo IS NULL OR LOWER(s.target_email) <= LOWER(:userTo)) AND " +
                        "(:status IS NULL OR " +
                        "     (:status = 'ACTIVE' AND u.is_active = true AND u.locked_at IS NULL) OR " +
                        "     (:status = 'INACTIVE' AND u.is_active = false) OR " +
                        "     (:status = 'LOCKED' AND u.locked_at IS NOT NULL)) AND " +
                        "(:action IS NULL OR s.action = :action) AND " +
                        "(CAST(:createdFrom AS timestamp) IS NULL OR s.created_at >= CAST(:createdFrom AS timestamp)) AND " +
                        "(CAST(:createdTo AS timestamp) IS NULL OR s.created_at <= CAST(:createdTo AS timestamp))",
           nativeQuery = true)
    Page<SecurityLog> findWithFilters(
            @Param("email")  String email,
            @Param("userFrom") String userFrom,
            @Param("userTo") String userTo,
            @Param("status") String status,
            @Param("action") String action,
            @Param("createdFrom") LocalDateTime createdFrom,
            @Param("createdTo") LocalDateTime createdTo,
            Pageable pageable);

    @Query(value = "SELECT s.* FROM public.security_log s " +
                   "LEFT JOIN auth.users u ON LOWER(u.email) = LOWER(s.target_email) WHERE " +
                   "(:email IS NULL OR LOWER(s.target_email) LIKE LOWER(CONCAT('%', :email, '%'))) AND " +
                   "(:userFrom IS NULL OR LOWER(s.target_email) >= LOWER(:userFrom)) AND " +
                   "(:userTo IS NULL OR LOWER(s.target_email) <= LOWER(:userTo)) AND " +
                   "(:status IS NULL OR " +
                   "     (:status = 'ACTIVE' AND u.is_active = true AND u.locked_at IS NULL) OR " +
                   "     (:status = 'INACTIVE' AND u.is_active = false) OR " +
                   "     (:status = 'LOCKED' AND u.locked_at IS NOT NULL)) AND " +
                   "(:action IS NULL OR s.action = :action) AND " +
                   "(CAST(:createdFrom AS timestamp) IS NULL OR s.created_at >= CAST(:createdFrom AS timestamp)) AND " +
                   "(CAST(:createdTo AS timestamp) IS NULL OR s.created_at <= CAST(:createdTo AS timestamp)) " +
                   "ORDER BY s.created_at DESC",
           nativeQuery = true)
    List<SecurityLog> findAllForExport(
            @Param("email") String email,
            @Param("userFrom") String userFrom,
            @Param("userTo") String userTo,
            @Param("status") String status,
            @Param("action") String action,
            @Param("createdFrom") LocalDateTime createdFrom,
            @Param("createdTo") LocalDateTime createdTo);
}
