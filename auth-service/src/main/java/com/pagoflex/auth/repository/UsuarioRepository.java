package com.pagoflex.auth.repository;

import com.pagoflex.auth.entity.Usuario;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByCorreo(String correo);
    Optional<Usuario> findByRut(String rut);
    boolean existsByCorreo(String correo);
    boolean existsByRut(String rut);
}
