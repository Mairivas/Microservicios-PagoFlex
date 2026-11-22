package com.pagoflex.pagos.controller;

import com.pagoflex.pagos.dto.CumplimientoResponse;
import com.pagoflex.pagos.dto.PagoRequest;
import com.pagoflex.pagos.dto.PagoResponse;
import com.pagoflex.pagos.exception.ApiException;
import com.pagoflex.pagos.service.PagoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
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
@RequestMapping("/api/pagos")
@Tag(name = "Pagos")
public class PagoController {

    private static final String ROL_USUARIO = "USUARIO";

    private final PagoService pagoService;

    public PagoController(PagoService pagoService) {
        this.pagoService = pagoService;
    }

    @PostMapping
    @Operation(summary = "Pagar un compromiso (simulado)")
    public ResponseEntity<PagoResponse> pagar(
            @Valid @RequestBody PagoRequest request,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Role") String rol) {
        exigirUsuario(rol);
        return ResponseEntity.status(HttpStatus.CREATED).body(pagoService.pagar(userId, request));
    }

    @GetMapping
    @Operation(summary = "Historial de pagos del usuario")
    public List<PagoResponse> historial(
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Role") String rol,
            @RequestParam(required = false) Long empresaId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        exigirUsuario(rol);
        return pagoService.historial(userId, empresaId, desde, hasta);
    }

    @GetMapping("/cumplimiento")
    @Operation(summary = "Indicador de cumplimiento de los últimos 12 meses")
    public CumplimientoResponse cumplimiento(
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Role") String rol) {
        exigirUsuario(rol);
        return pagoService.cumplimiento(userId);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Comprobante de un pago")
    public PagoResponse obtener(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Role") String rol) {
        exigirUsuario(rol);
        return pagoService.obtener(id, userId);
    }

    private void exigirUsuario(String rol) {
        if (!ROL_USUARIO.equals(rol)) {
            throw ApiException.forbidden("Esta opción es solo para usuarios finales.");
        }
    }
}
