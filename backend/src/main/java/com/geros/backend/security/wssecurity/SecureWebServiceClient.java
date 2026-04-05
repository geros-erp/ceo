package com.geros.backend.security.wssecurity;

import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.soap.security.wss4j2.Wss4jSecurityInterceptor;
import org.springframework.ws.transport.http.HttpComponentsMessageSender;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.security.KeyStore;

/**
 * Cliente SOAP con WS-Security y TLS 1.2
 * Implementa todos los requisitos de seguridad:
 * - WS-Security 1.0 con firma digital
 * - Certificados PKI (privado/público)
 * - TLS 1.2 para transporte seguro
 * - No repudio mediante firma de request/response
 */
@Configuration
@ConditionalOnProperty(name = "ws.security.enabled", havingValue = "true")
public class SecureWebServiceClient {

    @Value("${ws.security.keystore.path}")
    private Resource keystorePath;

    @Value("${ws.security.keystore.password}")
    private String keystorePassword;

    @Value("${ws.security.keystore.type:PKCS12}")
    private String keystoreType;

    @Value("${ws.security.truststore.path}")
    private Resource truststorePath;

    @Value("${ws.security.truststore.password}")
    private String truststorePassword;

    @Value("${ws.security.truststore.type:PKCS12}")
    private String truststoreType;

    @Autowired
    private Wss4jSecurityInterceptor securityInterceptorRequest;

    @Autowired
    private Wss4jSecurityInterceptor securityInterceptorResponse;

    /**
     * Configuración de TLS 1.2 con autenticación mutua (mTLS)
     * Usa certificados PKI para identificación y autenticación
     */
    @Bean
    public HttpComponentsMessageSender httpComponentsMessageSender() throws Exception {
        HttpComponentsMessageSender messageSender = new HttpComponentsMessageSender();
        messageSender.setHttpClient(createSecureHttpClient());
        return messageSender;
    }

    /**
     * Cliente HTTP con TLS 1.2 y certificados PKI
     */
    private CloseableHttpClient createSecureHttpClient() throws Exception {
        // Cargar keystore con certificado privado (para autenticación del cliente)
        KeyStore keyStore = KeyStore.getInstance(keystoreType);
        keyStore.load(keystorePath.getInputStream(), keystorePassword.toCharArray());

        // Cargar truststore con certificados públicos (para validar servidor)
        KeyStore trustStore = KeyStore.getInstance(truststoreType);
        trustStore.load(truststorePath.getInputStream(), truststorePassword.toCharArray());

        // Configurar SSLContext con TLS 1.2
        SSLContext sslContext = SSLContextBuilder.create()
                .setProtocol("TLSv1.2")
                .loadKeyMaterial(keyStore, keystorePassword.toCharArray())
                .loadTrustMaterial(trustStore, null)
                .build();

        // Configurar socket factory con TLS 1.2 únicamente
        SSLConnectionSocketFactory socketFactory = new SSLConnectionSocketFactory(
                sslContext,
                new String[]{"TLSv1.2"},
                null,
                SSLConnectionSocketFactory.getDefaultHostnameVerifier()
        );

        return HttpClients.custom()
                .setSSLSocketFactory(socketFactory)
                .build();
    }

    /**
     * Marshaller JAXB para serialización XML
     * Configurar con los paquetes de tus clases JAXB generadas
     */
    @Bean
    public Jaxb2Marshaller marshaller() {
        Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
        // Configurar con el paquete de tus clases JAXB
        // marshaller.setContextPath("com.geros.backend.ws.client.generated");
        return marshaller;
    }

    /**
     * Template de WebService con seguridad completa:
     * - WS-Security 1.0 (firma digital)
     * - TLS 1.2 (transporte)
     * - Certificados PKI
     */
    @Bean
    public WebServiceTemplate webServiceTemplate() throws Exception {
        WebServiceTemplate template = new WebServiceTemplate();
        template.setMarshaller(marshaller());
        template.setUnmarshaller(marshaller());
        template.setMessageSender(httpComponentsMessageSender());
        
        // Aplicar interceptores de seguridad WS-Security
        template.setInterceptors(new org.springframework.ws.client.support.interceptor.ClientInterceptor[]{
                securityInterceptorRequest,
                securityInterceptorResponse
        });
        
        return template;
    }
}
