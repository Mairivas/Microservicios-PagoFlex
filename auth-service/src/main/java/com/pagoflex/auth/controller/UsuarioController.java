package com.pagoflex.auth.controller;

import com.pagoflex.auth.dto.ActualizarPerfilRequest;
import com.pagoflex.auth.dto.CambiarClaveRequest;
import com.pagoflex.auth.dto.MensajeResponse;
import com.pagoflex.auth.dto.PerfilResponse;
import com.pagoflex.auth.dto.PersonaResponse;
import com.pagoflex.auth.dto.PreferenciasAvisosRequest;
import com.pagoflex.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/usuarios")
@Tag(name = "Usuarios")
public class UsuarioController {

    private final AuthService authService;

    public UsuarioController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/perfil")
    @Operation(summary = "Ver perfil del usuario autenticado")
    public ResponseEntity<PerfilResponse> verPerfil(@RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(authService.obtenerPerfil(userId));
    }

    @GetMapping("/buscar")
    @Operation(summary = "Buscar una persona por RUT (uso de ejecutivos)")
    public ResponseEntity<PersonaResponse> buscarPorRut(@RequestParam("rut") String rut) {
        return ResponseEntity.ok(authService.buscarPersonaPorRut(rut));
    }

    @PutMapping("/perfil")
    @Operation(summary = "Editar perfil del usuario autenticado")
    public ResponseEntity<PerfilResponse> editarPerfil(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody ActualizarPerfilRequest request) {
        return ResponseEntity.ok(authService.actualizarPerfil(userId, request));
    }

    @PostMapping("/cambiar-clave")
    @Operation(summary = "Cambiar clave del usuario autenticado")
    public ResponseEntity<MensajeResponse> cambiarClave(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody CambiarClaveRequest request) {
        authService.cambiarClave(userId, request);
        return ResponseEntity.ok(new MensajeResponse("Clave actualizada correctamente"));
    }

    @PutMapping("/preferencias-avisos")
    @Operation(summary = "Actualizar preferencias de avisos")
    public ResponseEntity<PerfilResponse> preferencias(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody PreferenciasAvisosRequest request) {
        return ResponseEntity.ok(authService.actualizarPreferencias(userId, request));
    }
}
