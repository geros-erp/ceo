package com.geros.backend.securitylog;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "security_log_exports")
public class SecurityLogExport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "file_path", nullable = false, length = 1000)
    private String filePath;

    @Column(name = "created_by", nullable = false, length = 150)
    private String createdBy;

    @Column(name = "filter_email", length = 150)
    private String filterEmail;

    @Column(name = "filter_action", length = 100)
    private String filterAction;

    @Column(name = "record_count", nullable = false)
    private int recordCount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public SecurityLogExport() {
    }

    public SecurityLogExport(String fileName, String filePath, String createdBy,
                             String filterEmail, String filterAction, int recordCount) {
        this.fileName = fileName;
        this.filePath = filePath;
        this.createdBy = createdBy;
        this.filterEmail = filterEmail;
        this.filterAction = filterAction;
        this.recordCount = recordCount;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getFileName() { return fileName; }
    public String getFilePath() { return filePath; }
    public String getCreatedBy() { return createdBy; }
    public String getFilterEmail() { return filterEmail; }
    public String getFilterAction() { return filterAction; }
    public int getRecordCount() { return recordCount; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
