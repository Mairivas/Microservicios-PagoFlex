package com.pagoflex.soporte.client;

import com.pagoflex.soporte.dto.CompromisoDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;

@FeignClient(name = "compromisos-service")
public interface CompromisoClient {

    @GetMapping("/api/internal/compromisos/{id}")
    CompromisoDto obtener(@PathVariable("id") Long id);

    @PutMapping("/api/internal/compromisos/{id}/revision")
    CompromisoDto marcarRevision(@PathVariable("id") Long id);

    @PutMapping("/api/internal/compromisos/{id}/restaurar")
    CompromisoDto restaurar(@PathVariable("id") Long id);
}
