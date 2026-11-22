package com.pagoflex.auth.controller;

import com.pagoflex.auth.entity.Comuna;
import com.pagoflex.auth.repository.ComunaRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/comunas")
@Tag(name = "Comunas")
public class ComunaController {

    private final ComunaRepository comunaRepository;

    public ComunaController(ComunaRepository comunaRepository) {
        this.comunaRepository = comunaRepository;
    }

    @GetMapping
    @Operation(summary = "Listar comunas")
    public ResponseEntity<List<Comuna>> listar() {
        return ResponseEntity.ok(comunaRepository.findAll());
    }
}
