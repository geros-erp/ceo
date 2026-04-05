package com.geros.backend.security.wssecurity;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.ws.soap.security.wss4j2.Wss4jSecurityInterceptor;
import org.springframework.ws.soap.security.wss4j2.support.CryptoFactoryBean;
import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.dom.WSConstants;

import java.io.IOException;
import java.util.Properties;

/**
 * Configuración de WS-Security 1.0 para consumo de servicios web
 * Cumple con estándares de seguridad Promigas:
 * - WS-Security 1.0
 * - PKI con certificados digitales
 * - Firma digital de mensajes (request/response)
 * - No repudio mediante firma digital
 * - TLS 1.2 para transporte
 */
@Configuration
@ConditionalOnProperty(name = "ws.security.enabled", havingValue = "true")
public class WsSecurityConfig {

    @Value("${ws.security.keystore.path}")
    private Resource keystorePath;

    @Value("${ws.security.keystore.password}")
    private String keystorePassword;

    @Value("${ws.security.keystore.type:PKCS12}")
    private String keystoreType;

    @Value("${ws.security.key.alias}")
    private String keyAlias;

    @Value("${ws.security.key.password}")
    private String keyPassword;

    @Value("${ws.security.truststore.path}")
    private Resource truststorePath;

    @Value("${ws.security.truststore.password}")
    private String truststorePassword;

    @Value("${ws.security.truststore.type:PKCS12}")
    private String truststoreType;

    /**
     * Configuración de Crypto para firma digital con certificado privado
     */
    @Bean(name = "clientCrypto")
    public Crypto clientCrypto() throws Exception {
        CryptoFactoryBean cryptoFactoryBean = new CryptoFactoryBean();
        cryptoFactoryBean.setKeyStoreLocation(keystorePath);
        cryptoFactoryBean.setKeyStorePassword(keystorePassword);
        cryptoFactoryBean.setKeyStoreType(keystoreType);
        cryptoFactoryBean.afterPropertiesSet();
        return cryptoFactoryBean.getObject();
    }

    /**
     * Configuración de Crypto para validación de certificados públicos
     */
    @Bean(name = "serverCrypto")
    public Crypto serverCrypto() throws Exception {
        CryptoFactoryBean cryptoFactoryBean = new CryptoFactoryBean();
        cryptoFactoryBean.setKeyStoreLocation(truststorePath);
        cryptoFactoryBean.setKeyStorePassword(truststorePassword);
        cryptoFactoryBean.setKeyStoreType(truststoreType);
        cryptoFactoryBean.afterPropertiesSet();
        return cryptoFactoryBean.getObject();
    }

    /**
     * Interceptor de seguridad para requests (firma digital del mensaje)
     * Implementa:
     * - Firma digital del cuerpo del mensaje con certificado privado
     * - Timestamp para prevenir ataques de replay
     * - Binary Security Token con certificado X.509
     */
    @Bean
    public Wss4jSecurityInterceptor securityInterceptorRequest() throws Exception {
        Wss4jSecurityInterceptor interceptor = new Wss4jSecurityInterceptor();
        
        // Acciones de seguridad: Timestamp + Firma Digital
        interceptor.setSecurementActions("Timestamp Signature");
        
        // Configuración de firma digital
        interceptor.setSecurementUsername(keyAlias);
        interceptor.setSecurementPassword(keyPassword);
        interceptor.setSecurementSignatureCrypto(clientCrypto());
        
        // Partes del mensaje a firmar (Body completo para no repudio)
        interceptor.setSecurementSignatureParts("{Element}{http://schemas.xmlsoap.org/soap/envelope/}Body");
        
        // Algoritmos de firma (compatibles con WS-Security 1.0)
        interceptor.setSecurementSignatureAlgorithm(WSConstants.RSA_SHA256);
        interceptor.setSecurementSignatureDigestAlgorithm(WSConstants.SHA256);
        
        // Incluir certificado X.509 en el mensaje
        interceptor.setSecurementSignatureKeyIdentifier("DirectReference");
        
        // Timestamp de 300 segundos (5 minutos)
        interceptor.setTimestampPrecisionInMilliseconds(true);
        
        return interceptor;
    }

    /**
     * Interceptor de seguridad para responses (validación de firma digital)
     * Implementa:
     * - Validación de firma digital del response
     * - Validación de certificado del servidor
     * - Validación de timestamp
     */
    @Bean
    public Wss4jSecurityInterceptor securityInterceptorResponse() throws Exception {
        Wss4jSecurityInterceptor interceptor = new Wss4jSecurityInterceptor();
        
        // Acciones de validación: Timestamp + Firma Digital
        interceptor.setValidationActions("Timestamp Signature");
        
        // Configuración de validación de firma
        interceptor.setValidationSignatureCrypto(serverCrypto());
        
        // Validar timestamp
        interceptor.setTimestampStrict(true);
        
        return interceptor;
    }
}
