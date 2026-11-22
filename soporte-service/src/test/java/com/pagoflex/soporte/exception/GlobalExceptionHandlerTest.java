package com.pagoflex.soporte.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    private Request peticion() {
        return Request.create(Request.HttpMethod.GET, "/x", Map.of(), null, StandardCharsets.UTF_8, new RequestTemplate());
    }

    @Test
    void errorDeClienteConservaElEstadoYElMensajeDelOtroServicio() {
        FeignException ex = new FeignException.NotFound("no", peticion(),
                "{\"mensaje\":\"Persona no encontrada\"}".getBytes(StandardCharsets.UTF_8), Map.of());

        ResponseEntity<Map<String, Object>> r = handler.handleFeign(ex);

        assertEquals(HttpStatus.NOT_FOUND, r.getStatusCode());
        assertEquals("Persona no encontrada", r.getBody().get("mensaje"));
    }

    @Test
    void errorDeClienteConCuerpoInvalidoUsaMensajeGeneral() {
        FeignException ex = new FeignException.Conflict("no", peticion(), "no es json".getBytes(StandardCharsets.UTF_8), Map.of());

        ResponseEntity<Map<String, Object>> r = handler.handleFeign(ex);

        assertEquals(HttpStatus.CONFLICT, r.getStatusCode());
        assertTrue(r.getBody().get("mensaje").toString().startsWith("No pudimos"));
    }

    @Test
    void errorDeClienteSinMensajeUsaMensajeGeneral() {
        FeignException ex = new FeignException.BadRequest("no", peticion(), "{}".getBytes(StandardCharsets.UTF_8), Map.of());

        assertEquals(HttpStatus.BAD_REQUEST, handler.handleFeign(ex).getStatusCode());
    }

    @Test
    void servicioCaidoEntregaServicioNoDisponible() {
        FeignException ex = new FeignException.InternalServerError("no", peticion(), new byte[0], Map.of());

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, handler.handleFeign(ex).getStatusCode());
    }
}
