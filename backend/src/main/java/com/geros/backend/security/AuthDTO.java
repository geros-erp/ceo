package com.geros.backend.security;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class AuthDTO {

    public static class LoginRequest {
        @NotBlank private String username;  // puede ser email o username
        @NotBlank private String password;
        private String recaptchaToken;

        public String getUsername()        { return username; }
        public String getPassword()        { return password; }
        public String getRecaptchaToken()  { return recaptchaToken; }
    }

    public static class RegisterRequest {
        @NotBlank private String username;
        @NotBlank private String firstName;
        @NotBlank private String lastName;
        @Email @NotBlank private String email;
        @NotBlank private String password;

        public String getUsername()  { return username; }
        public String getFirstName() { return firstName; }
        public String getLastName()  { return lastName; }
        public String getEmail()     { return email; }
        public String getPassword()  { return password; }
    }

    public static class ForgotPasswordRequest {
        @Email @NotBlank private String email;
        private String recaptchaToken;

        public String getEmail()          { return email; }
        public String getRecaptchaToken() { return recaptchaToken; }
    }

    public static class ResetPasswordRequest {
        @NotBlank private String token;
        @NotBlank private String newPassword;

        public String getToken()       { return token; }
        public String getNewPassword() { return newPassword; }
    }

    public static class LoginResponse {
        private String token;
        private String email;
        private String role;
        private boolean mustChangePassword;
        private Integer passwordExpiresInDays;
        private String currentLoginIp;
        private String previousLoginIp;
        private String currentLoginAt;
        private String previousLoginAt;
        private Integer sessionTimeoutSeconds;

        public LoginResponse(String token, String email, String role,
                             boolean mustChangePassword, Integer passwordExpiresInDays,
                             String currentLoginIp, String previousLoginIp,
                             String currentLoginAt, String previousLoginAt,
                             Integer sessionTimeoutSeconds) {
            this.token                 = token;
            this.email                 = email;
            this.role                  = role;
            this.mustChangePassword    = mustChangePassword;
            this.passwordExpiresInDays = passwordExpiresInDays;
            this.currentLoginIp        = currentLoginIp;
            this.previousLoginIp       = previousLoginIp;
            this.currentLoginAt        = currentLoginAt;
            this.previousLoginAt       = previousLoginAt;
            this.sessionTimeoutSeconds = sessionTimeoutSeconds;
        }

        public String getToken()                  { return token; }
        public String getEmail()                  { return email; }
        public String getRole()                   { return role; }
        public boolean isMustChangePassword()     { return mustChangePassword; }
        public Integer getPasswordExpiresInDays() { return passwordExpiresInDays; }
        public String getCurrentLoginIp()         { return currentLoginIp; }
        public String getPreviousLoginIp()        { return previousLoginIp; }
        public String getCurrentLoginAt()         { return currentLoginAt; }
        public String getPreviousLoginAt()        { return previousLoginAt; }
        public Integer getSessionTimeoutSeconds() { return sessionTimeoutSeconds; }
    }
}
