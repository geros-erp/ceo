package com.geros.backend.contract;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "contracts")
public class Contract {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String codigo;

    @Column(nullable = false, length = 255)
    private String descripcion;

    @Column(nullable = false, length = 50)
    private String estado = "Activo";

    @Column(length = 150)
    private String responsable;

    @Column(length = 150)
    private String contratista;

    @Column(precision = 19, scale = 2)
    private BigDecimal valor;

    @Column(name = "orden_compra", length = 100)
    private String ordenCompra;

    @Column(length = 100)
    private String categoria;

    @Column(name = "fecha_inicio")
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin")
    private LocalDate fechaFin;

    @Column(columnDefinition = "TEXT")
    private String objeto;

    @Column(columnDefinition = "TEXT")
    private String alcance;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "created_by", length = 150)
    private String createdBy;

    @Column(name = "updated_by", length = 150)
    private String updatedBy;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Getters
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
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public String getCreatedBy() { return createdBy; }
    public String getUpdatedBy() { return updatedBy; }

    // Setters
    public void setId(Long id) { this.id = id; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public void setEstado(String estado) { this.estado = estado; }
    public void setResponsable(String responsable) { this.responsable = responsable; }
    public void setContratista(String contratista) { this.contratista = contratista; }
    public void setValor(BigDecimal valor) { this.valor = valor; }
    public void setOrdenCompra(String ordenCompra) { this.ordenCompra = ordenCompra; }
    public void setCategoria(String categoria) { this.categoria = categoria; }
    public void setFechaInicio(LocalDate fechaInicio) { this.fechaInicio = fechaInicio; }
    public void setFechaFin(LocalDate fechaFin) { this.fechaFin = fechaFin; }
    public void setObjeto(String objeto) { this.objeto = objeto; }
    public void setAlcance(String alcance) { this.alcance = alcance; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
}
