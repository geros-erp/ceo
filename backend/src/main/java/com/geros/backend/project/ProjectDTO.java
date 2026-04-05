package com.geros.backend.project;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public class ProjectDTO {

    public static class Request {
        @NotNull(message = "El contrato asociado es obligatorio")
        private Long contractId;
        
        @NotBlank(message = "El código es obligatorio")
        private String codigo;
        
        @NotBlank(message = "El nombre del proyecto es obligatorio")
        private String nombre;
        
        private String zona;
        private String estado;
        private LocalDate fechaInicio;
        private LocalDate fechaFin;
        private String observaciones;

        public Long getContractId() { return contractId; }
        public void setContractId(Long contractId) { this.contractId = contractId; }
        public String getCodigo() { return codigo; }
        public void setCodigo(String codigo) { this.codigo = codigo; }
        public String getNombre() { return nombre; }
        public void setNombre(String nombre) { this.nombre = nombre; }
        public String getZona() { return zona; }
        public void setZona(String zona) { this.zona = zona; }
        public String getEstado() { return estado; }
        public void setEstado(String estado) { this.estado = estado; }
        public LocalDate getFechaInicio() { return fechaInicio; }
        public void setFechaInicio(LocalDate fechaInicio) { this.fechaInicio = fechaInicio; }
        public LocalDate getFechaFin() { return fechaFin; }
        public void setFechaFin(LocalDate fechaFin) { this.fechaFin = fechaFin; }
        public String getObservaciones() { return observaciones; }
        public void setObservaciones(String observaciones) { this.observaciones = observaciones; }
    }

    public static class Response {
        private Long id;
        private Long contractId;
        private String contractCodigo; // Para mostrarlo en el frontend
        private String codigo;
        private String nombre;
        private String zona;
        private String estado;
        private LocalDate fechaInicio;
        private LocalDate fechaFin;
        private String observaciones;

        public static Response from(Project p) {
            Response r = new Response();
            r.id = p.getId();
            r.contractId = p.getContract().getId();
            r.contractCodigo = p.getContract().getCodigo();
            r.codigo = p.getCodigo();
            r.nombre = p.getNombre();
            r.zona = p.getZona();
            r.estado = p.getEstado();
            r.fechaInicio = p.getFechaInicio();
            r.fechaFin = p.getFechaFin();
            r.observaciones = p.getObservaciones();
            return r;
        }

        public Long getId() { return id; }
        public Long getContractId() { return contractId; }
        public String getContractCodigo() { return contractCodigo; }
        public String getCodigo() { return codigo; }
        public String getNombre() { return nombre; }
        public String getZona() { return zona; }
        public String getEstado() { return estado; }
        public LocalDate getFechaInicio() { return fechaInicio; }
        public LocalDate getFechaFin() { return fechaFin; }
        public String getObservaciones() { return observaciones; }
    }
}
