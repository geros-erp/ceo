package com.geros.backend.contract;

import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.time.LocalDate;

public class ContractDTO {

    public static class Request {
        @NotBlank private String codigo;
        @NotBlank private String descripcion;
        private String estado;
        private String responsable;
        private String contratista;
        private BigDecimal valor;
        private String ordenCompra;
        private String categoria;
        private LocalDate fechaInicio;
        private LocalDate fechaFin;
        private String objeto;
        private String alcance;

        public String getCodigo() { return codigo; }
        public void setCodigo(String codigo) { this.codigo = codigo; }
        public String getDescripcion() { return descripcion; }
        public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
        public String getEstado() { return estado; }
        public void setEstado(String estado) { this.estado = estado; }
        public String getResponsable() { return responsable; }
        public void setResponsable(String responsable) { this.responsable = responsable; }
        public String getContratista() { return contratista; }
        public void setContratista(String contratista) { this.contratista = contratista; }
        public BigDecimal getValor() { return valor; }
        public void setValor(BigDecimal valor) { this.valor = valor; }
        public String getOrdenCompra() { return ordenCompra; }
        public void setOrdenCompra(String ordenCompra) { this.ordenCompra = ordenCompra; }
        public String getCategoria() { return categoria; }
        public void setCategoria(String categoria) { this.categoria = categoria; }
        public LocalDate getFechaInicio() { return fechaInicio; }
        public void setFechaInicio(LocalDate fechaInicio) { this.fechaInicio = fechaInicio; }
        public LocalDate getFechaFin() { return fechaFin; }
        public void setFechaFin(LocalDate fechaFin) { this.fechaFin = fechaFin; }
        public String getObjeto() { return objeto; }
        public void setObjeto(String objeto) { this.objeto = objeto; }
        public String getAlcance() { return alcance; }
        public void setAlcance(String alcance) { this.alcance = alcance; }
    }

    public static class Response {
        private Long id;
        private String codigo;
        private String descripcion;
        private String estado;
        private String responsable;
        private String contratista;
        private BigDecimal valor;
        private String ordenCompra;
        private String categoria;
        private LocalDate fechaInicio;
        private LocalDate fechaFin;
        private String objeto;
        private String alcance;

        public static Response from(Contract c) {
            Response r = new Response();
            r.id = c.getId();
            r.codigo = c.getCodigo();
            r.descripcion = c.getDescripcion();
            r.estado = c.getEstado();
            r.responsable = c.getResponsable();
            r.contratista = c.getContratista();
            r.valor = c.getValor();
            r.ordenCompra = c.getOrdenCompra();
            r.categoria = c.getCategoria();
            r.fechaInicio = c.getFechaInicio();
            r.fechaFin = c.getFechaFin();
            r.objeto = c.getObjeto();
            r.alcance = c.getAlcance();
            return r;
        }

        public Long getId() { return id; }
        public String getCodigo() { return codigo; }
        public String getDescripcion() { return descripcion; }
        public String getEstado() { return estado; }
        public String getResponsable() { return responsable; }
        public String getContratista() { return contratista; }
        public BigDecimal getValor() { return valor; }
        public String getOrdenCompra() { return ordenCompra; }
        public String getCategoria() { return categoria; }
        public LocalDate getFechaInicio() { return fechaInicio; }
        public LocalDate getFechaFin() { return fechaFin; }
        public String getObjeto() { return objeto; }
        public String getAlcance() { return alcance; }
    }
}
