package com.pagoflex.auth.config;

import com.pagoflex.auth.entity.Area;
import com.pagoflex.auth.entity.Comuna;
import com.pagoflex.auth.entity.Rol;
import com.pagoflex.auth.entity.Usuario;
import com.pagoflex.auth.repository.AreaRepository;
import com.pagoflex.auth.repository.ComunaRepository;
import com.pagoflex.auth.repository.RolRepository;
import com.pagoflex.auth.repository.UsuarioRepository;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataSeeder {

    public static final String CLAVE_COMUN = "PagoFlex2026!";

    @Bean
    public CommandLineRunner seed(
            RolRepository rolRepository,
            ComunaRepository comunaRepository,
            AreaRepository areaRepository,
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder) {
        return args -> {
            if (rolRepository.count() > 0) {
                return;
            }
            Rol rolUsuario = rolRepository.save(new Rol(null, "USUARIO"));
            Rol rolAgente = rolRepository.save(new Rol(null, "AGENTE"));
            Rol rolEjecutivo = rolRepository.save(new Rol(null, "EJECUTIVO"));

            Area soporte = areaRepository.save(new Area(null, "Soporte"));
            Area comercial = areaRepository.save(new Area(null, "Comercial"));

            List<String> comunasNombres = List.of(
                    "Maipú", "Puente Alto", "San Miguel", "La Florida", "Ñuñoa",
                    "Quilicura", "San Bernardo", "Estación Central", "Santiago", "Providencia");
            Map<String, Comuna> comunas = new HashMap<>();
            for (String nombre : comunasNombres) {
                comunas.put(nombre, comunaRepository.save(new Comuna(null, nombre)));
            }

            String hash = passwordEncoder.encode(CLAVE_COMUN);

            crearUsuarioFinal(usuarioRepository, rolUsuario, comunas, hash,
                    "15.834.207-3", "Camila Andrea", "Rojas Peña", "crojas@correo.ejemplo",
                    "+56 9 0000 0101", "Maipú", LocalDate.of(2023, 3, 14), true, 3);
            crearUsuarioFinal(usuarioRepository, rolUsuario, comunas, hash,
                    "12.476.318-5", "Luis Alberto", "Muñoz Tapia", "lmunoz@correo.ejemplo",
                    "+56 9 0000 0102", "Puente Alto", LocalDate.of(2023, 11, 2), true, 1);
            crearUsuarioFinal(usuarioRepository, rolUsuario, comunas, hash,
                    "6.893.541-5", "Rosa Elena", "Contreras Díaz", "rcontreras@correo.ejemplo",
                    "+56 9 0000 0103", "San Miguel", LocalDate.of(2024, 6, 20), true, 5);
            crearUsuarioFinal(usuarioRepository, rolUsuario, comunas, hash,
                    "21.045.873-5", "Matías Ignacio", "Soto Vergara", "msoto@correo.ejemplo",
                    "+56 9 0000 0104", "La Florida", LocalDate.of(2025, 3, 7), false, 0);
            crearUsuarioFinal(usuarioRepository, rolUsuario, comunas, hash,
                    "17.562.904-1", "Fernanda Paz", "Godoy Lagos", "fgodoy@correo.ejemplo",
                    "+56 9 0000 0105", "Ñuñoa", LocalDate.of(2024, 1, 15), true, 3);
            crearUsuarioFinal(usuarioRepository, rolUsuario, comunas, hash,
                    "10.318.265-4", "Héctor Manuel", "Pizarro Campos", "hpizarro@correo.ejemplo",
                    "+56 9 0000 0106", "Quilicura", LocalDate.of(2023, 8, 28), true, 3);
            crearUsuarioFinal(usuarioRepository, rolUsuario, comunas, hash,
                    "19.287.430-0", "Javiera Constanza", "Reyes Molina", "jreyes@correo.ejemplo",
                    "+56 9 0000 0107", "San Bernardo", LocalDate.of(2025, 5, 3), true, 1);
            crearUsuarioFinal(usuarioRepository, rolUsuario, comunas, hash,
                    "18.704.396-4", "Benjamín Esteban", "Araya Fuentes", "baraya@correo.ejemplo",
                    "+56 9 0000 0108", "Estación Central", LocalDate.of(2026, 9, 11), true, 3);
            

            crearAgente(usuarioRepository, rolAgente, soporte, hash,
                    "16.248.317-K", "Andrés Felipe", "Lagos Meza", "alagos@pagoflex.ejemplo", true);
            crearAgente(usuarioRepository, rolAgente, soporte, hash,
                    "18.352.091-1", "Daniela Sofía", "Ortiz Bravo", "dortiz@pagoflex.ejemplo", true);
            crearAgente(usuarioRepository, rolAgente, soporte, hash,
                    "13.907.264-2", "Ricardo José", "Salinas Cáceres", "rsalinas@pagoflex.ejemplo", false);
            crearAgente(usuarioRepository, rolAgente, comercial, hash,
                    "17.781.435-0", "Valentina Ignacia", "Paredes Silva", "vparedes@pagoflex.ejemplo", true);
            crearAgente(usuarioRepository, rolAgente, comercial, hash,
                    "19.564.028-9", "Tomás Alejandro", "Vidal Rojas", "tvidal@pagoflex.ejemplo", true);

            crearEjecutivo(usuarioRepository, rolEjecutivo, hash,
                    "14.629.853-2", "Paula Andrea", "Saavedra Núñez", "psaavedra@altosmaipo.ejemplo",
                    "+56 9 0000 0401", 1L);
            crearEjecutivo(usuarioRepository, rolEjecutivo, hash,
                    "11.853.720-3", "Marco Antonio", "Fuentes Riquelme", "mfuentes@bosquenativo.ejemplo",
                    "+56 9 0000 0402", 2L);
            crearEjecutivo(usuarioRepository, rolEjecutivo, hash,
                    "15.370.946-7", "Claudia Beatriz", "Henríquez Soto", "chenriquez@creditoandino.ejemplo",
                    "+56 9 0000 0403", 3L);
            crearEjecutivo(usuarioRepository, rolEjecutivo, hash,
                    "17.095.482-3", "Felipe Ignacio", "Morales Tapia", "fmorales@creditoandino.ejemplo",
                    "+56 9 0000 0404", 3L);
            crearEjecutivo(usuarioRepository, rolEjecutivo, hash,
                    "9.846.217-1", "Gloria Inés", "Carrasco Vega", "gcarrasco@loshalcones.ejemplo",
                    "+56 9 0000 0405", 6L);
            crearEjecutivo(usuarioRepository, rolEjecutivo, hash,
                    "18.930.571-0", "Sebastián Andrés", "Núñez Olivares", "snunez@itcordillera.ejemplo",
                    "+56 9 0000 0406", 7L);
            crearEjecutivo(usuarioRepository, rolEjecutivo, hash,
                    "12.584.396-4", "Mónica Patricia", "Leiva Castro", "mleiva@plazaoriente.ejemplo",
                    "+56 9 0000 0407", 8L);
        };
    }

    private void crearUsuarioFinal(UsuarioRepository repo, Rol rol, Map<String, Comuna> comunas,
            String hash, String rut, String nombres, String apellidos, String correo,
            String telefono, String comunaNombre, LocalDate incorporacion, boolean avisos, int dias) {
        Usuario u = new Usuario();
        u.setRut(rut);
        u.setNombres(nombres);
        u.setApellidos(apellidos);
        u.setCorreo(correo);
        u.setTelefono(telefono);
        u.setComuna(comunas.get(comunaNombre));
        u.setRol(rol);
        u.setClaveHash(hash);
        u.setActivo(true);
        u.setFechaIncorporacion(incorporacion);
        u.setAvisosActivos(avisos);
        u.setAvisosDias(dias);
        repo.save(u);
    }

    private void crearAgente(UsuarioRepository repo, Rol rol, Area area,
            String hash, String rut, String nombres, String apellidos, String correo, boolean activo) {
        Usuario u = new Usuario();
        u.setRut(rut);
        u.setNombres(nombres);
        u.setApellidos(apellidos);
        u.setCorreo(correo);
        u.setRol(rol);
        u.setArea(area);
        u.setClaveHash(hash);
        u.setActivo(activo);
        u.setFechaIncorporacion(LocalDate.now());
        u.setAvisosActivos(false);
        u.setAvisosDias(0);
        repo.save(u);
    }

    private void crearEjecutivo(UsuarioRepository repo, Rol rol,
            String hash, String rut, String nombres, String apellidos, String correo,
            String telefono, Long empresaId) {
        Usuario u = new Usuario();
        u.setRut(rut);
        u.setNombres(nombres);
        u.setApellidos(apellidos);
        u.setCorreo(correo);
        u.setTelefono(telefono);
        u.setRol(rol);
        u.setEmpresaId(empresaId);
        u.setClaveHash(hash);
        u.setActivo(true);
        u.setFechaIncorporacion(LocalDate.now());
        u.setAvisosActivos(false);
        u.setAvisosDias(0);
        repo.save(u);
    }
}
