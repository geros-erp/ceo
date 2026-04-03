package com.geros.backend.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.util.Set;
import java.util.stream.Collectors;

public class UserDTO {

    public static class Request {
        @NotBlank private String username;
        @NotBlank private String firstName;
        @NotBlank private String lastName;
        @Email @NotBlank private String email;
        private Set<Long> roleIds;

        public String getUsername()   { return username; }
        public String getFirstName()  { return firstName; }
        public String getLastName()   { return lastName; }
        public String getEmail()      { return email; }
        public Set<Long> getRoleIds() { return roleIds; }
    }

    public static class Response {
        private Long id;
        private String username;
        private String firstName;
        private String lastName;
        private String email;
        private Boolean isActive;
        private Set<String> roles;
        private boolean locked;
        private int failedAttempts;
        private boolean mustChangePassword;
        private String passwordChangedAt;
        private String lockedAt;
        private String createdAt;
        private String status;
        private boolean isDefaultUser;

        public Long getId()                   { return id; }
        public String getUsername()           { return username; }
        public String getFirstName()          { return firstName; }
        public String getLastName()           { return lastName; }
        public String getEmail()              { return email; }
        public Boolean getIsActive()          { return isActive; }
        public Set<String> getRoles()         { return roles; }
        public boolean isLocked()             { return locked; }
        public int getFailedAttempts()        { return failedAttempts; }
        public boolean isMustChangePassword() { return mustChangePassword; }
        public String getPasswordChangedAt()  { return passwordChangedAt; }
        public String getLockedAt()           { return lockedAt; }
        public String getCreatedAt()          { return createdAt; }
        public String getStatus()             { return status; }
        public boolean isDefaultUser()        { return isDefaultUser; }

        public static Response from(User u) {
            Response r = new Response();
            r.id                 = u.getId();
            r.username           = u.getUsername();
            r.firstName          = u.getFirstName();
            r.lastName           = u.getLastName();
            r.email              = u.getEmail();
            r.isActive           = u.getIsActive();
            r.roles              = u.getRoles().stream().map(role -> role.getName()).collect(Collectors.toSet());
            r.locked             = u.isLocked();
            r.failedAttempts     = u.getFailedAttempts();
            r.mustChangePassword = u.isMustChangePassword();
            r.passwordChangedAt  = u.getPasswordChangedAt() != null ? u.getPasswordChangedAt().toString() : null;
            r.lockedAt           = u.getLockedAt() != null ? u.getLockedAt().toString() : null;
            r.createdAt          = u.getCreatedAt().toString();
            r.status             = u.isLocked() ? "LOCKED" : Boolean.TRUE.equals(u.getIsActive()) ? "ACTIVE" : "INACTIVE";
            r.isDefaultUser      = u.isDefaultUser();
            return r;
        }
    }

    public static class SummaryResponse {
        private Long id;
        private String username;
        private String fullName;
        private String email;
        private String status;

        public Long getId()         { return id; }
        public String getUsername() { return username; }
        public String getFullName() { return fullName; }
        public String getEmail()    { return email; }
        public String getStatus()   { return status; }

        public static SummaryResponse from(User user) {
            SummaryResponse response = new SummaryResponse();
            response.id = user.getId();
            response.username = user.getUsername();
            response.fullName = user.getFirstName() + " " + user.getLastName();
            response.email = user.getEmail();
            response.status = user.isLocked() ? "LOCKED" : Boolean.TRUE.equals(user.getIsActive()) ? "ACTIVE" : "INACTIVE";
            return response;
        }
    }

    public static class UpdateRequest {
        private String firstName;
        private String lastName;
        private Boolean isActive;
        private Set<Long> roleIds;

        public String getFirstName()  { return firstName; }
        public String getLastName()   { return lastName; }
        public Boolean getIsActive()  { return isActive; }
        public Set<Long> getRoleIds() { return roleIds; }
    }

    public static class ChangePasswordRequest {
        @NotBlank private String currentPassword;
        @NotBlank private String newPassword;

        public String getCurrentPassword() { return currentPassword; }
        public String getNewPassword()     { return newPassword; }
    }

    public static class AdminChangePasswordRequest {
        @NotBlank private String newPassword;
        private boolean mustChangePassword = false;

        public String getNewPassword()       { return newPassword; }
        public boolean isMustChangePassword(){ return mustChangePassword; }
    }
}
