package com.geros.backend.trace;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
public class TransactionTraceFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String transactionId = resolveTransactionId(request);
        request.setAttribute(TransactionContext.REQUEST_ATTRIBUTE, transactionId);
        response.setHeader(TransactionContext.HEADER_NAME, transactionId);
        TransactionContext.setCurrentTransactionId(transactionId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            TransactionContext.clear();
        }
    }

    private String resolveTransactionId(HttpServletRequest request) {
        String incoming = request.getHeader(TransactionContext.HEADER_NAME);
        if (incoming != null && !incoming.isBlank()) {
            return incoming.trim();
        }
        return UUID.randomUUID().toString();
    }
}
