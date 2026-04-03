package com.geros.backend.security;

import com.geros.backend.policy.PasswordPolicy;
import com.geros.backend.user.User;
import com.geros.backend.policy.PasswordPolicyRepository;
import com.geros.backend.user.UserRepository;
import com.geros.backend.securitylog.SecurityLog;
import com.geros.backend.securitylog.SecurityLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserInactivityTask {

    private final UserRepository userRepository;
    private final PasswordPolicyRepository policyRepository;
    private final SecurityLogService securityLogService;

    // Se ejecuta todos los días a las 00:00
    @Scheduled(cron = "0 0 0 * * ?")
    @Transactional
    public void disableInactiveUsers() {
        PasswordPolicy policy = policyRepository.findById(1L).orElse(null);

        if (policy == null || policy.getMaxInactivityDays() <= 0) {
            return;
        }

        LocalDateTime threshold = LocalDateTime.now().minusDays(policy.getMaxInactivityDays());

        // Buscamos usuarios activos cuyo último login sea anterior al threshold
        // O que nunca hayan logueado y su fecha de creación sea anterior al threshold
        List<User> inactiveUsers = userRepository.findInactiveUsers(threshold);

        if (inactiveUsers.isEmpty()) {
            return;
        }

        log.info("Iniciando desactivación de {} usuarios por inactividad", inactiveUsers.size());

        for (User user : inactiveUsers) {
            user.setIsActive(false);
            userRepository.save(user);

            // Registrar en el log de seguridad usando el servicio y el Enum correcto
            securityLogService.log(
                SecurityLog.Action.ACCOUNT_LOCKED,
                user.getEmail(),
                "SYSTEM",
                "Cuenta desactivada automáticamente por superar " + policy.getMaxInactivityDays() + " días de inactividad."
            );
        }
    }
}