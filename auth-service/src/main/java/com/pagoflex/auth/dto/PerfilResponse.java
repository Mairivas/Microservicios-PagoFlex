package com.pagoflex.auth.dto;

public class PerfilResponse {

    private Long id;
    private String rut;
    private String nombres;
    private String apellidos;
    private String correo;
    private String telefono;
    private Long comunaId;
    private String comunaNombre;
    private String rol;
    private Long empresaId;
    private Long areaId;
    private String areaNombre;
    private Boolean avisosActivos;
    private Integer avisosDias;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRut() {
        return rut;
    }

    public void setRut(String rut) {
        this.rut = rut;
    }

    public String getNombres() {
        return nombres;
    }

    public void setNombres(String nombres) {
        this.nombres = nombres;
    }

    public String getApellidos() {
        return apellidos;
    }

    public void setApellidos(String apellidos) {
        this.apellidos = apellidos;
    }

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public Long getComunaId() {
        return comunaId;
    }

    public void setComunaId(Long comunaId) {
        this.comunaId = comunaId;
    }

    public String getComunaNombre() {
        return comunaNombre;
    }

    public void setComunaNombre(String comunaNombre) {
        this.comunaNombre = comunaNombre;
    }

    public String getRol() {
        return rol;
    }

    public void setRol(String rol) {
        this.rol = rol;
    }

    public Long getEmpresaId() {
        return empresaId;
    }

    public void setEmpresaId(Long empresaId) {
        this.empresaId = empresaId;
    }

    public Long getAreaId() {
        return areaId;
    }

    public void setAreaId(Long areaId) {
        this.areaId = areaId;
    }

    public String getAreaNombre() {
        return areaNombre;
    }

    public void setAreaNombre(String areaNombre) {
        this.areaNombre = areaNombre;
    }

    public Boolean getAvisosActivos() {
        return avisosActivos;
    }

    public void setAvisosActivos(Boolean avisosActivos) {
        this.avisosActivos = avisosActivos;
    }

    public Integer getAvisosDias() {
        return avisosDias;
    }

    public void setAvisosDias(Integer avisosDias) {
        this.avisosDias = avisosDias;
    }
}
