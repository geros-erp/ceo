package com.geros.backend.policy;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class PasswordGenerator {

    private static final String UPPER   = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String LOWER   = "abcdefghijklmnopqrstuvwxyz";
    private static final String DIGITS  = "0123456789";
    private static final String SPECIAL = "!@#$%^&*()_+-=[]{}";

    private final PasswordPolicyService policyService;
    private final SecureRandom random = new SecureRandom();

    public PasswordGenerator(PasswordPolicyService policyService) {
        this.policyService = policyService;
    }

    public String generate() {
        PasswordPolicy policy = policyService.getPolicy();

        StringBuilder pool = new StringBuilder(LOWER);
        StringBuilder password = new StringBuilder();

        // Garantizar al menos un carácter de cada tipo requerido
        if (policy.isEnabled()) {
            if (policy.isRequireUppercase())    { pool.append(UPPER);   password.append(randomChar(UPPER)); }
            if (policy.isRequireLowercase())    {                        password.append(randomChar(LOWER)); }
            if (policy.isRequireNumbers())      { pool.append(DIGITS);  password.append(randomChar(DIGITS)); }
            if (policy.isRequireSpecialChars()) { pool.append(SPECIAL); password.append(randomChar(SPECIAL)); }
        }

        int length = policy.isEnabled() ? Math.max(policy.getMinLength(), 12) : 12;

        // Completar hasta la longitud requerida
        String poolStr = pool.toString();
        while (password.length() < length)
            password.append(randomChar(poolStr));

        // Mezclar para evitar patrones predecibles
        return shuffle(password.toString());
    }

    private char randomChar(String source) {
        return source.charAt(random.nextInt(source.length()));
    }

    private String shuffle(String input) {
        char[] chars = input.toCharArray();
        for (int i = chars.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            char tmp = chars[i]; chars[i] = chars[j]; chars[j] = tmp;
        }
        return new String(chars);
    }
}
