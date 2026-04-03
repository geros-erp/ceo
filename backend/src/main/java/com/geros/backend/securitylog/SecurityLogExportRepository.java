package com.geros.backend.securitylog;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SecurityLogExportRepository extends JpaRepository<SecurityLogExport, Long> {
    List<SecurityLogExport> findTop20ByOrderByCreatedAtDesc();
}
