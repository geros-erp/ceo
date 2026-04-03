package com.geros.backend.security;

import com.geros.backend.role.RoleRepository;
import com.geros.backend.securitylog.SecurityLog;
import com.geros.backend.securitylog.SecurityLogService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class PrivilegedActivityAuditFilter extends OncePerRequestFilter {

    private final SecurityLogService securityLogService;
    private final RoleRepository roleRepository;

    public PrivilegedActivityAuditFilter(SecurityLogService securityLogService, RoleRepository roleRepository) {
        this.securityLogService = securityLogService;
        this.roleRepository = roleRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        filterChain.doFilter(request, response);

        if ("OPTIONS".equalsIgnoreCase(request.getMethod()) || request.getRequestURI().startsWith("/error")) {
            return;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication.getName() == null || authentication.getName().isBlank()) {
            return;
        }

        Set<String> roleNames = authentication.getAuthorities().stream()
                .map(grantedAuthority -> grantedAuthority.getAuthority())
                .filter(authority -> authority != null && authority.startsWith("ROLE_"))
                .map(authority -> authority.substring("ROLE_".length()))
                .collect(Collectors.toSet());

        boolean privilegedActor = roleNames.contains("ADMIN") || roleRepository.existsByNameInAndPrivilegedTrue(roleNames);
        if (!privilegedActor) {
            return;
        }

        String requestUri = request.getRequestURI();
        String queryString = request.getQueryString();
        String queryData = queryString != null && !queryString.isBlank() ? queryString : "N/A";

        securityLogService.log(new SecurityLogService.AuditEntry(SecurityLog.Action.PRIVILEGED_USER_ACTIVITY)
                .eventType(response.getStatus() >= 400 ? SecurityLog.EventType.ERROR : SecurityLog.EventType.SUCCESS)
                .eventCode("PRIVILEGED_USER_ACTIVITY")
                .origin("Actividad de usuario privilegiado")
                .target(requestUri)
                .performedBy(authentication.getName())
                .description("Operacion ejecutada por usuario privilegiado")
                .oldValue("metodo=" + request.getMethod() + ", query=" + queryData)
                .newValue("estadoHttp=" + response.getStatus()));
    }
}
