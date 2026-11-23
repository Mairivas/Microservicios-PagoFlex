package com.pagoflex.soporte.repository;

import com.pagoflex.soporte.entity.EvidenciaReporte;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EvidenciaRepository extends JpaRepository<EvidenciaReporte, Long> {

    Optional<EvidenciaReporte> findByReporteId(Long reporteId);

    boolean existsByReporteId(Long reporteId);
}
