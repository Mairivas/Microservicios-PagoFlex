package com.pagoflex.compromisos.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "compromiso")
public class Compromiso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String folio;

    @Column(nullable = false)
    private Long usuarioId;

    @Column(nullable = false)
    private Long empresaId;

    @Column(nullable = false)
    private String concepto;

    private Integer cuotaNumero;

    private Integer cuotaTotal;

    @Column(nullable = false)
    private Integer monto;

    @Column(nullable = false)
    private LocalDate vencimiento;

    @Column(nullable = false)
    private String estado;

    private Integer recargoAplicado;

    private LocalDateTime fechaPago;

    private String motivoAnulacion;

    private Long anuladoPorId;

    private LocalDateTime fechaAnulacion;

    private Long registradoPorId;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFolio() {
        return folio;
    }

    public void setFolio(String folio) {
        this.folio = folio;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(Long usuarioId) {
        this.usuarioId = usuarioId;
    }

    public Long getEmpresaId() {
        return empresaId;
    }

    public void setEmpresaId(Long empresaId) {
        this.empresaId = empresaId;
    }

    public String getConcepto() {
        return concepto;
    }

    public void setConcepto(String concepto) {
        this.concepto = concepto;
    }

    public Integer getCuotaNumero() {
        return cuotaNumero;
    }

    public void setCuotaNumero(Integer cuotaNumero) {
        this.cuotaNumero = cuotaNumero;
    }

    public Integer getCuotaTotal() {
        return cuotaTotal;
    }

    public void setCuotaTotal(Integer cuotaTotal) {
        this.cuotaTotal = cuotaTotal;
    }

    public Integer getMonto() {
        return monto;
    }

    public void setMonto(Integer monto) {
        this.monto = monto;
    }

    public LocalDate getVencimiento() {
        return vencimiento;
    }

    public void setVencimiento(LocalDate vencimiento) {
        this.vencimiento = vencimiento;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public Integer getRecargoAplicado() {
        return recargoAplicado;
    }

    public void setRecargoAplicado(Integer recargoAplicado) {
        this.recargoAplicado = recargoAplicado;
    }

    public LocalDateTime getFechaPago() {
        return fechaPago;
    }

    public void setFechaPago(LocalDateTime fechaPago) {
        this.fechaPago = fechaPago;
    }

    public String getMotivoAnulacion() {
        return motivoAnulacion;
    }

    public void setMotivoAnulacion(String motivoAnulacion) {
        this.motivoAnulacion = motivoAnulacion;
    }

    public Long getAnuladoPorId() {
        return anuladoPorId;
    }

    public void setAnuladoPorId(Long anuladoPorId) {
        this.anuladoPorId = anuladoPorId;
    }

    public LocalDateTime getFechaAnulacion() {
        return fechaAnulacion;
    }

    public void setFechaAnulacion(LocalDateTime fechaAnulacion) {
        this.fechaAnulacion = fechaAnulacion;
    }

    public Long getRegistradoPorId() {
        return registradoPorId;
    }

    public void setRegistradoPorId(Long registradoPorId) {
        this.registradoPorId = registradoPorId;
    }
}
