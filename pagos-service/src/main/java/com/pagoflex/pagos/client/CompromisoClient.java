package com.pagoflex.pagos.client;

import com.pagoflex.pagos.dto.CompromisoDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "compromisos-service")
public interface CompromisoClient {

    @PutMapping("/api/internal/compromisos/{id}/pagar")
    CompromisoDto pagar(@PathVariable("id") Long id, @RequestParam("usuarioId") Long usuarioId);

    @PutMapping("/api/internal/compromisos/{id}/revertir-pago")
    CompromisoDto revertirPago(@PathVariable("id") Long id);
}
