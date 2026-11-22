package com.pagoflex.soporte.repository;

import com.pagoflex.soporte.entity.Reporte;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReporteRepository extends JpaRepository<Reporte, Long> {

    List<Reporte> findByUsuarioIdOrderByFechaDesc(Long usuarioId);

    List<Reporte> findAllByOrderByFechaDesc();

    Optional<Reporte> findFirstByCompromisoIdAndEstadoIn(Long compromisoId, Collection<String> estados);
}
