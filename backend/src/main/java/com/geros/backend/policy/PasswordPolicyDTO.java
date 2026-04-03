package com.geros.backend.policy;

import java.util.List;

public class PasswordPolicyDTO {

    public static class Request {
        private boolean enabled;
        private int minLength;
        private int maxLength;
        private boolean requireUppercase;
        private boolean requireLowercase;
        private boolean requireNumbers;
        private boolean requireSpecialChars;
        private int maxFailedAttempts;
        private int lockDurationMinutes;
        private int expirationDays;
        private int notifyBeforeExpirationDays;
        private int minPasswordAgeDays;
        private int passwordHistoryCount;
        private int maxSequenceLength;
        private Integer sessionTimeoutSeconds;
        private List<String> reservedUsernames;

        public boolean isEnabled()                   { return enabled; }
        public int getMinLength()                    { return minLength; }
        public int getMaxLength()                    { return maxLength; }
        public boolean isRequireUppercase()          { return requireUppercase; }
        public boolean isRequireLowercase()          { return requireLowercase; }
        public boolean isRequireNumbers()            { return requireNumbers; }
        public boolean isRequireSpecialChars()       { return requireSpecialChars; }
        public int getMaxFailedAttempts()            { return maxFailedAttempts; }
        public int getLockDurationMinutes()          { return lockDurationMinutes; }
        public int getExpirationDays()               { return expirationDays; }
        public int getNotifyBeforeExpirationDays()   { return notifyBeforeExpirationDays; }
        public int getMinPasswordAgeDays()           { return minPasswordAgeDays; }
        public int getPasswordHistoryCount()         { return passwordHistoryCount; }
        public int getMaxSequenceLength()            { return maxSequenceLength; }
        public Integer getSessionTimeoutSeconds()    { return sessionTimeoutSeconds; }
        public List<String> getReservedUsernames()   { return reservedUsernames; }
    }

    public static class Response {
        private boolean enabled;
        private int minLength;
        private int maxLength;
        private boolean requireUppercase;
        private boolean requireLowercase;
        private boolean requireNumbers;
        private boolean requireSpecialChars;
        private int maxFailedAttempts;
        private int lockDurationMinutes;
        private int expirationDays;
        private int notifyBeforeExpirationDays;
        private int minPasswordAgeDays;
        private int passwordHistoryCount;
        private int maxSequenceLength;
        private Integer sessionTimeoutSeconds;
        private List<String> reservedUsernames;

        public boolean isEnabled()                   { return enabled; }
        public int getMinLength()                    { return minLength; }
        public int getMaxLength()                    { return maxLength; }
        public boolean isRequireUppercase()          { return requireUppercase; }
        public boolean isRequireLowercase()          { return requireLowercase; }
        public boolean isRequireNumbers()            { return requireNumbers; }
        public boolean isRequireSpecialChars()       { return requireSpecialChars; }
        public int getMaxFailedAttempts()            { return maxFailedAttempts; }
        public int getLockDurationMinutes()          { return lockDurationMinutes; }
        public int getExpirationDays()               { return expirationDays; }
        public int getNotifyBeforeExpirationDays()   { return notifyBeforeExpirationDays; }
        public int getMinPasswordAgeDays()           { return minPasswordAgeDays; }
        public int getPasswordHistoryCount()         { return passwordHistoryCount; }
        public int getMaxSequenceLength()            { return maxSequenceLength; }
        public Integer getSessionTimeoutSeconds()    { return sessionTimeoutSeconds; }
        public List<String> getReservedUsernames()   { return reservedUsernames; }

        public static Response from(PasswordPolicy p) {
            Response r = new Response();
            r.enabled                    = p.isEnabled();
            r.minLength                  = p.getMinLength();
            r.maxLength                  = p.getMaxLength();
            r.requireUppercase           = p.isRequireUppercase();
            r.requireLowercase           = p.isRequireLowercase();
            r.requireNumbers             = p.isRequireNumbers();
            r.requireSpecialChars        = p.isRequireSpecialChars();
            r.maxFailedAttempts          = p.getMaxFailedAttempts();
            r.lockDurationMinutes        = p.getLockDurationMinutes();
            r.expirationDays             = p.getExpirationDays();
            r.notifyBeforeExpirationDays = p.getNotifyBeforeExpirationDays();
            r.minPasswordAgeDays         = p.getMinPasswordAgeDays();
            r.passwordHistoryCount       = p.getPasswordHistoryCount();
            r.maxSequenceLength          = p.getMaxSequenceLength();
            r.sessionTimeoutSeconds      = p.getSessionTimeoutSeconds();
            r.reservedUsernames          = p.getReservedUsernames();
            return r;
        }
    }
}
