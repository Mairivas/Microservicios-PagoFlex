package com.pagoflex.pagos.repository;

import com.pagoflex.pagos.entity.Pago;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PagoRepository extends JpaRepository<Pago, Long> {

    List<Pago> findByUsuarioIdOrderByFechaHoraDesc(Long usuarioId);
}
