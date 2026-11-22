package com.pagoflex.auth.controller;

import com.pagoflex.auth.dto.CambiarClaveRequest;
import com.pagoflex.auth.dto.LoginRequest;
import com.pagoflex.auth.dto.LoginResponse;
import com.pagoflex.auth.dto.MensajeResponse;
import com.pagoflex.auth.dto.PerfilResponse;
import com.pagoflex.auth.dto.RecuperarClaveConfirmacionRequest;
import com.pagoflex.auth.dto.RecuperarClaveSolicitudRequest;
import com.pagoflex.auth.dto.RegistroRequest;
import com.pagoflex.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Autenticación")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    @Operation(summary = "Iniciar sesión y obtener JWT")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/registro")
    @Operation(summary = "Registrar usuario final")
    public ResponseEntity<PerfilResponse> registro(@Valid @RequestBody RegistroRequest request) {
        PerfilResponse perfil = authService.registrar(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(perfil);
    }

    @PostMapping("/recuperar-clave/solicitud")
    @Operation(summary = "Solicitar código de recuperación (se registra en el log)")
    public ResponseEntity<MensajeResponse> solicitarRecuperacion(@Valid @RequestBody RecuperarClaveSolicitudRequest request) {
        authService.solicitarRecuperacion(request);
        return ResponseEntity.ok(new MensajeResponse("Si el correo existe, recibirás un código de recuperación"));
    }

    @PostMapping("/recuperar-clave/confirmacion")
    @Operation(summary = "Confirmar código y establecer nueva clave")
    public ResponseEntity<MensajeResponse> confirmarRecuperacion(@Valid @RequestBody RecuperarClaveConfirmacionRequest request) {
        authService.confirmarRecuperacion(request);
        return ResponseEntity.ok(new MensajeResponse("Clave actualizada correctamente"));
    }

    @PostMapping("/cambiar-clave")
    @Operation(summary = "Cambiar clave (autenticado)")
    public ResponseEntity<MensajeResponse> cambiarClave(
            HttpServletRequest request,
            @Valid @RequestBody CambiarClaveRequest body) {
        Long usuarioId = Long.valueOf(request.getHeader("X-User-Id"));
        authService.cambiarClave(usuarioId, body);
        return ResponseEntity.ok(new MensajeResponse("Clave actualizada correctamente"));
    }
}
