package com.pagoflex.auth.repository;

import com.pagoflex.auth.entity.Comuna;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ComunaRepository extends JpaRepository<Comuna, Long> {
    Optional<Comuna> findByNombre(String nombre);
}
