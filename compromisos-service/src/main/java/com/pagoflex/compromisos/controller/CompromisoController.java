package com.pagoflex.compromisos.controller;

import com.pagoflex.compromisos.dto.AnularRequest;
import com.pagoflex.compromisos.dto.CobranzaResponse;
import com.pagoflex.compromisos.dto.CompromisoResponse;
import com.pagoflex.compromisos.dto.CrearCompromisoRequest;
import com.pagoflex.compromisos.dto.ResumenResponse;
import com.pagoflex.compromisos.exception.ApiException;
import com.pagoflex.compromisos.service.CompromisoService;
import com.pagoflex.compromisos.service.Roles;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/compromisos")
@Tag(name = "Compromisos")
public class CompromisoController {

    private final CompromisoService compromisoService;

    public CompromisoController(CompromisoService compromisoService) {
        this.compromisoService = compromisoService;
    }

    @GetMapping
    @Operation(summary = "Listar compromisos del usuario o de la empresa del ejecutivo")
    public List<CompromisoResponse> listar(
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Role") String rol,
            @RequestHeader(value = "X-Empresa-Id", required = false) String empresaId,
            @RequestParam(required = false) String estado) {
        return compromisoService.listar(userId, rol, empresa(empresaId), estado);
    }

    @GetMapping("/resumen")
    @Operation(summary = "Resumen del usuario final")
    public ResumenResponse resumen(
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Role") String rol) {
        exigir(rol, Roles.USUARIO);
        return compromisoService.resumen(userId);
    }

    @GetMapping("/cobranza")
    @Operation(summary = "Resumen de cobranza del mes de la empresa del ejecutivo")
    public CobranzaResponse cobranza(
            @RequestHeader("X-User-Role") String rol,
            @RequestHeader(value = "X-Empresa-Id", required = false) String empresaId,
            @RequestParam(required = false) String mes) {
        exigir(rol, Roles.EJECUTIVO);
        Long empresa = empresa(empresaId);
        if (empresa == null) {
            throw ApiException.forbidden("Tu cuenta no está asociada a una empresa.");
        }
        return compromisoService.cobranza(empresa, parsearMes(mes));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalle de un compromiso")
    public CompromisoResponse obtener(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Role") String rol,
            @RequestHeader(value = "X-Empresa-Id", required = false) String empresaId) {
        return compromisoService.obtener(id, userId, rol, empresa(empresaId));
    }

    @PostMapping
    @Operation(summary = "Registrar un compromiso (ejecutivo)")
    public ResponseEntity<CompromisoResponse> crear(
            @Valid @RequestBody CrearCompromisoRequest request,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Role") String rol,
            @RequestHeader(value = "X-Empresa-Id", required = false) String empresaId) {
        exigir(rol, Roles.EJECUTIVO);
        Long empresa = empresa(empresaId);
        if (empresa == null) {
            throw ApiException.forbidden("Tu cuenta no está asociada a una empresa.");
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(compromisoService.crear(request, empresa, userId));
    }

    @PostMapping("/{id}/anular")
    @Operation(summary = "Anular un compromiso de la empresa (ejecutivo)")
    public CompromisoResponse anular(
            @PathVariable Long id,
            @Valid @RequestBody AnularRequest request,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Role") String rol,
            @RequestHeader(value = "X-Empresa-Id", required = false) String empresaId) {
        exigir(rol, Roles.EJECUTIVO);
        Long empresa = empresa(empresaId);
        if (empresa == null) {
            throw ApiException.forbidden("Tu cuenta no está asociada a una empresa.");
        }
        return compromisoService.anular(id, request, empresa, userId);
    }

    private void exigir(String rol, String esperado) {
        if (!esperado.equals(rol)) {
            throw ApiException.forbidden("Tu perfil no tiene acceso a esta opción.");
        }
    }

    private Long empresa(String valor) {
        return valor == null || valor.isBlank() ? null : Long.valueOf(valor);
    }

    private YearMonth parsearMes(String mes) {
        if (mes == null || mes.isBlank()) {
            return null;
        }
        try {
            return YearMonth.parse(mes);
        } catch (DateTimeParseException e) {
            throw ApiException.badRequest("El mes debe tener el formato AAAA-MM.");
        }
    }
}
