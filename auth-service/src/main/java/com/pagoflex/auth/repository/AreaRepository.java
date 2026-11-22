package com.pagoflex.auth.repository;

import com.pagoflex.auth.entity.Area;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AreaRepository extends JpaRepository<Area, Long> {
    Optional<Area> findByNombre(String nombre);
}
