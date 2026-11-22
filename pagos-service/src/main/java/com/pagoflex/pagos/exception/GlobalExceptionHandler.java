package com.pagoflex.pagos.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<Map<String, Object>> handleApi(ApiException ex) {
        return build(ex.getStatus(), ex.getMessage());
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<Map<String, Object>> handleHeader(MissingRequestHeaderException ex) {
        return build(HttpStatus.UNAUTHORIZED, "Tu sesión no es válida. Inicia sesión nuevamente.");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", HttpStatus.BAD_REQUEST.value());
        Map<String, String> errors = new HashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            errors.put(fe.getField(), fe.getDefaultMessage());
        }
        body.put("errores", errors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(FeignException.class)
    public ResponseEntity<Map<String, Object>> handleFeign(FeignException ex) {
        HttpStatus status = ex.status() >= 400 && ex.status() < 500
                ? HttpStatus.valueOf(ex.status())
                : HttpStatus.SERVICE_UNAVAILABLE;
        String mensaje = "El servicio no está disponible en este momento. Intenta de nuevo.";
        if (status != HttpStatus.SERVICE_UNAVAILABLE) {
            mensaje = extraerMensaje(ex.contentUTF8(), "No pudimos completar la operación.");
        }
        return build(status, mensaje);
    }

    private String extraerMensaje(String cuerpo, String porDefecto) {
        try {
            String mensaje = new ObjectMapper().readTree(cuerpo).path("mensaje").asText("");
            return mensaje.isBlank() ? porDefecto : mensaje;
        } catch (Exception e) {
            return porDefecto;
        }
    }

    private ResponseEntity<Map<String, Object>> build(HttpStatus status, String message) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", status.value());
        body.put("mensaje", message);
        return ResponseEntity.status(status).body(body);
    }
}
