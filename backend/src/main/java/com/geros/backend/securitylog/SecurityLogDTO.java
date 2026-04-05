package com.geros.backend.securitylog;

public class SecurityLogDTO {

    public static class ExportResponse {
        private Long id;
        private String fileName;
        private String createdBy;
        private String filterEmail;
        private String filterAction;
        private int recordCount;
        private String createdAt;

        public Long getId() { return id; }
        public String getFileName() { return fileName; }
        public String getCreatedBy() { return createdBy; }
        public String getFilterEmail() { return filterEmail; }
        public String getFilterAction() { return filterAction; }
        public int getRecordCount() { return recordCount; }
        public String getCreatedAt() { return createdAt; }

        public static ExportResponse from(SecurityLogExport export) {
            ExportResponse response = new ExportResponse();
            response.id = export.getId();
            response.fileName = export.getFileName();
            response.createdBy = SecurityLogMaskingUtils.maskEmail(export.getCreatedBy());
            response.filterEmail = SecurityLogMaskingUtils.maskEmail(export.getFilterEmail());
            response.filterAction = export.getFilterAction();
            response.recordCount = export.getRecordCount();
            response.createdAt = export.getCreatedAt().toString();
            return response;
        }
    }

    public static class Response {
        private Long id;
        private String action;
        private String eventType;
        private String eventCode;
        private String origin;
        private String transactionId;
        private String targetEmail;
        private String targetStatus;
        private String performedBy;
        private String detail;
        private String ipAddress;
        private String hostName;
        private String oldValue;
        private String newValue;
        private String createdAt;

        public Long getId()            { return id; }
        public String getAction()      { return action; }
        public String getEventType()   { return eventType; }
        public String getEventCode()   { return eventCode; }
        public String getOrigin()      { return origin; }
        public String getTransactionId() { return transactionId; }
        public String getTargetEmail() { return targetEmail; }
        public String getTargetStatus() { return targetStatus; }
        public String getPerformedBy() { return performedBy; }
        public String getDetail()      { return detail; }
        public String getIpAddress()   { return ipAddress; }
        public String getHostName()    { return hostName; }
        public String getOldValue()    { return oldValue; }
        public String getNewValue()    { return newValue; }
        public String getCreatedAt()   { return createdAt; }

        public static Response from(SecurityLog s) {
            Response r = new Response();
            r.id          = s.getId();
            r.action      = s.getAction().name();
            r.eventType   = s.getEventType().name();
            r.eventCode   = s.getEventCode();
            r.origin      = s.getOrigin();
            r.transactionId = s.getTransactionId();
            r.targetEmail = SecurityLogMaskingUtils.maskEmail(s.getTargetEmail());
            r.targetStatus = resolveTargetStatus(s);
            r.performedBy = SecurityLogMaskingUtils.maskEmail(s.getPerformedBy());
            r.detail      = SecurityLogMaskingUtils.maskFreeText(s.getDetail());
            r.ipAddress   = SecurityLogMaskingUtils.maskIp(s.getIpAddress());
            r.hostName    = SecurityLogMaskingUtils.maskHost(s.getHostName());
            r.oldValue    = SecurityLogMaskingUtils.maskFreeText(s.getOldValue());
            r.newValue    = SecurityLogMaskingUtils.maskFreeText(s.getNewValue());
            r.createdAt   = s.getCreatedAt().toString();
            return r;
        }

        private static String resolveTargetStatus(SecurityLog s) {
            return null;
        }
    }
}
