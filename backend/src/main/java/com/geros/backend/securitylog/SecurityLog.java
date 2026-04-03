package com.geros.backend.securitylog;

import com.geros.backend.user.User;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "security_log")
public class SecurityLog {

    public enum EventType {
        SUCCESS,
        ERROR
    }

    public enum Action {
        USER_CREATED,
        USER_UPDATED,
        USER_DELETED,
        USER_DATA_ACCESSED,
        USER_LIST_ACCESSED,
        CUSTOMER_DATA_ACCESSED,
        CUSTOMER_LIST_ACCESSED,
        SECURITY_LOG_QUERIED,
        SECURITY_LOG_EXPORTED,
        PRIVILEGED_USER_ACTIVITY,
        LOGIN_OUTSIDE_ALLOWED_HOURS,
        IMPORTANT_FILE_MODIFIED,
        PASSWORD_POLICY_UPDATED,
        AUTHORIZATION_PARAMETERS_UPDATED,
        ROLE_CREATED,
        ROLE_UPDATED,
        ROLE_DELETED,
        PASSWORD_CHANGED_BY_USER,
        PASSWORD_CHANGED_BY_ADMIN,
        PASSWORD_FORCED_CHANGE,
        LOGIN_FAILED,
        ACCOUNT_LOCKED,
        ACCOUNT_UNLOCKED,
        LOGIN_SUCCESS,
        LOGOUT
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private Action action;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 20)
    private EventType eventType;

    @Column(name = "event_code", nullable = false, length = 100)
    private String eventCode;

    @Column(name = "origin", length = 200)
    private String origin;

    @Column(name = "transaction_id", length = 100)
    private String transactionId;

    @Column(name = "target_email", nullable = false, length = 150)
    private String targetEmail;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_email", referencedColumnName = "email", insertable = false, updatable = false)
    private User targetUser;

    @Column(name = "performed_by", length = 150)
    private String performedBy;

    @Column(length = 500)
    private String detail;

    @Column(name = "ip_address", length = 100)
    private String ipAddress;

    @Column(name = "host_name", length = 255)
    private String hostName;

    @Column(name = "old_value", length = 2000)
    private String oldValue;

    @Column(name = "new_value", length = 2000)
    private String newValue;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public SecurityLog() {}

    public SecurityLog(Action action, EventType eventType, String eventCode, String origin, String transactionId,
                       String targetEmail, String performedBy, String detail,
                       String ipAddress, String hostName, String oldValue, String newValue) {
        this.action      = action;
        this.eventType   = eventType;
        this.eventCode   = eventCode;
        this.origin      = origin;
        this.transactionId = transactionId;
        this.targetEmail = targetEmail;
        this.performedBy = performedBy;
        this.detail      = detail;
        this.ipAddress   = ipAddress;
        this.hostName    = hostName;
        this.oldValue    = oldValue;
        this.newValue    = newValue;
        this.createdAt   = LocalDateTime.now();
    }

    public Long getId()            { return id; }
    public Action getAction()      { return action; }
    public EventType getEventType() { return eventType; }
    public String getEventCode()   { return eventCode; }
    public String getOrigin()      { return origin; }
    public String getTransactionId() { return transactionId; }
    public String getTargetEmail() { return targetEmail; }
    public User getTargetUser()    { return targetUser; }
    public String getPerformedBy() { return performedBy; }
    public String getDetail()      { return detail; }
    public String getIpAddress()   { return ipAddress; }
    public String getHostName()    { return hostName; }
    public String getOldValue()    { return oldValue; }
    public String getNewValue()    { return newValue; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
