package com.pagoflex.compromisos.repository;

import com.pagoflex.compromisos.entity.Compromiso;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompromisoRepository extends JpaRepository<Compromiso, Long> {

    List<Compromiso> findByUsuarioIdOrderByVencimientoAsc(Long usuarioId);

    List<Compromiso> findByEmpresaIdOrderByVencimientoAsc(Long empresaId);

    boolean existsByUsuarioIdAndEmpresaIdAndEstadoAndVencimientoBefore(
            Long usuarioId, Long empresaId, String estado, LocalDate vencimiento);

    boolean existsByUsuarioIdAndEmpresaIdAndConceptoAndCuotaNumero(
            Long usuarioId, Long empresaId, String concepto, Integer cuotaNumero);

    Optional<Compromiso> findFirstByUsuarioIdAndEmpresaIdAndCuotaNumeroAndEstadoAndRegistradoPorIdIsNull(
            Long usuarioId, Long empresaId, Integer cuotaNumero, String estado);
}
