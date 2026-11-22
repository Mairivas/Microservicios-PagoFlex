package com.pagoflex.auth.repository;

import com.pagoflex.auth.entity.RecuperacionClave;
import com.pagoflex.auth.entity.Usuario;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecuperacionClaveRepository extends JpaRepository<RecuperacionClave, Long> {
    Optional<RecuperacionClave> findTopByUsuarioAndCodigoAndUsadoFalseOrderByFechaCreacionDesc(Usuario usuario, String codigo);
    void deleteByUsuario(Usuario usuario);
}
