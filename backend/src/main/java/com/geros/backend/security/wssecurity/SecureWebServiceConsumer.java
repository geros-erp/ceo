package com.geros.backend.security.wssecurity;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.ws.client.core.WebServiceTemplate;

/**
 * Servicio de ejemplo para consumo de servicios web con WS-Security
 * 
 * Este servicio demuestra cómo consumir servicios web externos cumpliendo con:
 * - WS-Security 1.0 (firma digital de mensajes)
 * - PKI con certificados digitales
 * - TLS 1.2 para transporte
 * - No repudio mediante auditoría de mensajes
 * 
 * IMPORTANTE: Adaptar este ejemplo a tus necesidades específicas:
 * 1. Generar clases JAXB desde el WSDL del servicio externo
 * 2. Configurar el marshaller con el paquete correcto
 * 3. Implementar los métodos de negocio específicos
 */
@Service
@ConditionalOnProperty(name = "ws.security.enabled", havingValue = "true")
public class SecureWebServiceConsumer {

    @Autowired
    private WebServiceTemplate webServiceTemplate;

    @Value("${ws.external.service.url}")
    private String serviceUrl;

    /**
     * Ejemplo de consumo de servicio web seguro
     * 
     * @param request Objeto JAXB generado desde el WSDL
     * @return Objeto JAXB de respuesta
     */
    public Object callSecureWebService(Object request) {
        try {
            // El WebServiceTemplate automáticamente:
            // 1. Firma digitalmente el request con el certificado privado
            // 2. Incluye timestamp para prevenir replay attacks
            // 3. Envía el mensaje por TLS 1.2
            // 4. Valida la firma digital del response
            // 5. Registra todo en el log de auditoría (no repudio)
            
            Object response = webServiceTemplate.marshalSendAndReceive(serviceUrl, request);
            
            return response;
            
        } catch (Exception e) {
            // Manejar excepciones de seguridad
            throw new RuntimeException("Error al consumir servicio web seguro: " + e.getMessage(), e);
        }
    }

    /**
     * Ejemplo específico: Consulta de información
     * Reemplazar con tus clases JAXB generadas
     */
    /*
    public ConsultaResponse consultarInformacion(String parametro) {
        ConsultaRequest request = new ConsultaRequest();
        request.setParametro(parametro);
        
        return (ConsultaResponse) callSecureWebService(request);
    }
    */
}
