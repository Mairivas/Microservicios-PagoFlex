package com.pagoflex.compromisos.client;

import com.pagoflex.compromisos.dto.PersonaResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "auth-service")
public interface PersonaClient {

    @GetMapping("/api/usuarios/buscar")
    PersonaResponse buscarPorRut(@RequestParam("rut") String rut);
}
