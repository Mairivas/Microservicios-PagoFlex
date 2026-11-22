package com.pagoflex.compromisos.controller;

import com.pagoflex.compromisos.dto.CompromisoResponse;
import com.pagoflex.compromisos.service.CompromisoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/internal/compromisos")
@Tag(name = "Uso interno entre servicios")
public class InternalCompromisoController {

    private final CompromisoService compromisoService;

    public InternalCompromisoController(CompromisoService compromisoService) {
        this.compromisoService = compromisoService;
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener un compromiso")
    public CompromisoResponse obtener(@PathVariable Long id) {
        return compromisoService.obtenerInterno(id);
    }

    @PutMapping("/{id}/pagar")
    @Operation(summary = "Marcar el compromiso como pagado")
    public CompromisoResponse pagar(@PathVariable Long id, @RequestParam Long usuarioId) {
        return compromisoService.pagar(id, usuarioId);
    }

    @PutMapping("/{id}/revertir-pago")
    @Operation(summary = "Revertir un pago no confirmado")
    public CompromisoResponse revertirPago(@PathVariable Long id) {
        return compromisoService.revertirPago(id);
    }

    @PutMapping("/{id}/revision")
    @Operation(summary = "Pasar el compromiso a En revisión")
    public CompromisoResponse revision(@PathVariable Long id) {
        return compromisoService.marcarRevision(id);
    }

    @PutMapping("/{id}/restaurar")
    @Operation(summary = "Sacar el compromiso de En revisión")
    public CompromisoResponse restaurar(@PathVariable Long id) {
        return compromisoService.restaurar(id);
    }
}
