package com.pagoflex.soporte.controller;

import com.pagoflex.soporte.dto.EvidenciaDto;
import com.pagoflex.soporte.dto.ReporteRequest;
import com.pagoflex.soporte.dto.ReporteResponse;
import com.pagoflex.soporte.dto.ResolverRequest;
import com.pagoflex.soporte.exception.ApiException;
import com.pagoflex.soporte.service.ReporteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reportes")
@Tag(name = "Reportes")
public class ReporteController {

    private final ReporteService reporteService;

    public ReporteController(ReporteService reporteService) {
        this.reporteService = reporteService;
    }

    @PostMapping
    @Operation(summary = "Reportar un problema con un compromiso")
    public ResponseEntity<ReporteResponse> crear(
            @Valid @RequestBody ReporteRequest request,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Role") String rol) {
        exigir(rol, "USUARIO");
        return ResponseEntity.status(HttpStatus.CREATED).body(reporteService.crear(userId, request));
    }

    @GetMapping
    @Operation(summary = "Listar reportes del usuario o todos los reportes (agente)")
    public List<ReporteResponse> listar(
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Role") String rol,
            @RequestParam(required = false) String estado) {
        return reporteService.listar(userId, rol, estado);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalle de un reporte")
    public ReporteResponse obtener(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Role") String rol) {
        return reporteService.obtener(id, userId, rol);
    }

    @GetMapping("/compromiso/{compromisoId}")
    @Operation(summary = "Reporte de un compromiso de la empresa (ejecutivo, solo lectura)")
    public ReporteResponse porCompromiso(
            @PathVariable Long compromisoId,
            @RequestHeader("X-User-Role") String rol,
            @RequestHeader(value = "X-Empresa-Id", required = false) String empresaId) {
        Long empresa = empresaId == null || empresaId.isBlank() ? null : Long.valueOf(empresaId);
        return reporteService.obtenerPorCompromiso(compromisoId, rol, empresa);
    }

    @PutMapping("/{id}/tomar")
    @Operation(summary = "Tomar un reporte abierto (agente)")
    public ReporteResponse tomar(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Role") String rol) {
        exigir(rol, "AGENTE");
        return reporteService.tomar(id, userId);
    }

    @PutMapping("/{id}/resolver")
    @Operation(summary = "Resolver un reporte con una respuesta (agente)")
    public ReporteResponse resolver(
            @PathVariable Long id,
            @Valid @RequestBody ResolverRequest request,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Role") String rol) {
        exigir(rol, "AGENTE");
        return reporteService.resolver(id, userId, request.respuesta());
    }

    @PutMapping("/{id}/evidencia")
    @Operation(summary = "Adjuntar imagen de evidencia al reporte (usuario)")
    public ReporteResponse guardarEvidencia(
            @PathVariable Long id,
            @Valid @RequestBody EvidenciaDto request,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Role") String rol) {
        exigir(rol, "USUARIO");
        return reporteService.guardarEvidencia(id, userId, request.imagenBase64());
    }

    @GetMapping("/{id}/evidencia")
    @Operation(summary = "Obtener la imagen de evidencia del reporte")
    public EvidenciaDto evidencia(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Role") String rol) {
        return new EvidenciaDto(reporteService.obtenerEvidencia(id, userId, rol));
    }

    private void exigir(String rol, String esperado) {
        if (!esperado.equals(rol)) {
            throw ApiException.forbidden("Tu perfil no tiene acceso a esta opción.");
        }
    }
}
