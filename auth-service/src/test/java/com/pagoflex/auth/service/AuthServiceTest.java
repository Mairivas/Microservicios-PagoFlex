package com.pagoflex.auth.service;

import com.pagoflex.auth.dto.ActualizarPerfilRequest;
import com.pagoflex.auth.dto.CambiarClaveRequest;
import com.pagoflex.auth.dto.LoginRequest;
import com.pagoflex.auth.dto.LoginResponse;
import com.pagoflex.auth.dto.PerfilResponse;
import com.pagoflex.auth.dto.PreferenciasAvisosRequest;
import com.pagoflex.auth.dto.RecuperarClaveConfirmacionRequest;
import com.pagoflex.auth.dto.RecuperarClaveSolicitudRequest;
import com.pagoflex.auth.dto.RegistroRequest;
import com.pagoflex.auth.entity.Comuna;
import com.pagoflex.auth.entity.RecuperacionClave;
import com.pagoflex.auth.entity.Rol;
import com.pagoflex.auth.entity.Usuario;
import com.pagoflex.auth.exception.BadRequestException;
import com.pagoflex.auth.exception.ConflictException;
import com.pagoflex.auth.exception.ResourceNotFoundException;
import com.pagoflex.auth.exception.UnauthorizedException;
import com.pagoflex.auth.repository.ComunaRepository;
import com.pagoflex.auth.repository.RecuperacionClaveRepository;
import com.pagoflex.auth.repository.RolRepository;
import com.pagoflex.auth.repository.UsuarioRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private RolRepository rolRepository;
    @Mock
    private ComunaRepository comunaRepository;
    @Mock
    private RecuperacionClaveRepository recuperacionRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    private Usuario usuarioActivo;

    @BeforeEach
    void setUp() {
        usuarioActivo = new Usuario();
        usuarioActivo.setId(1L);
        usuarioActivo.setRut("15.834.207-3");
        usuarioActivo.setNombres("Camila");
        usuarioActivo.setApellidos("Rojas");
        usuarioActivo.setCorreo("camila@correo.ejemplo");
        usuarioActivo.setClaveHash("$2a$10$hash");
        usuarioActivo.setActivo(true);
        Rol rol = new Rol(1L, "USUARIO");
        usuarioActivo.setRol(rol);
        usuarioActivo.setAvisosActivos(true);
        usuarioActivo.setAvisosDias(3);
    }

    @Test
    void loginExitosoRetornaToken() {
        LoginRequest request = new LoginRequest();
        request.setCorreo("camila@correo.ejemplo");
        request.setClave("password123");
        when(usuarioRepository.findByCorreo("camila@correo.ejemplo")).thenReturn(Optional.of(usuarioActivo));
        when(passwordEncoder.matches("password123", "$2a$10$hash")).thenReturn(true);
        when(jwtService.generateToken(usuarioActivo)).thenReturn("jwt-token");

        LoginResponse response = authService.login(request);

        assertEquals("jwt-token", response.getToken());
        assertEquals(1L, response.getId());
        assertEquals("USUARIO", response.getRol());
    }

    @Test
    void loginFallaCuandoCorreoNoExiste() {
        LoginRequest request = new LoginRequest();
        request.setCorreo("noexiste@correo.ejemplo");
        request.setClave("cualquiera");
        when(usuarioRepository.findByCorreo(anyString())).thenReturn(Optional.empty());

        assertThrows(UnauthorizedException.class, () -> authService.login(request));
    }

    @Test
    void loginFallaConClaveIncorrecta() {
        LoginRequest request = new LoginRequest();
        request.setCorreo("camila@correo.ejemplo");
        request.setClave("mala");
        when(usuarioRepository.findByCorreo("camila@correo.ejemplo")).thenReturn(Optional.of(usuarioActivo));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        assertThrows(UnauthorizedException.class, () -> authService.login(request));
    }

    @Test
    void loginFallaConUsuarioInactivo() {
        usuarioActivo.setActivo(false);
        LoginRequest request = new LoginRequest();
        request.setCorreo("camila@correo.ejemplo");
        request.setClave("password123");
        when(usuarioRepository.findByCorreo("camila@correo.ejemplo")).thenReturn(Optional.of(usuarioActivo));

        assertThrows(UnauthorizedException.class, () -> authService.login(request));
    }

    @Test
    void registroRechazaRutInvalido() {
        RegistroRequest request = new RegistroRequest();
        request.setRut("11.111.111-2");
        request.setClave("password123");
        request.setConfirmacionClave("password123");

        assertThrows(BadRequestException.class, () -> authService.registrar(request));
    }

    @Test
    void registroRechazaClavesQueNoCoinciden() {
        RegistroRequest request = new RegistroRequest();
        request.setRut("15.834.207-3");
        request.setClave("password123");
        request.setConfirmacionClave("otra");

        assertThrows(BadRequestException.class, () -> authService.registrar(request));
    }

    @Test
    void registroRechazaRutDuplicado() {
        RegistroRequest request = new RegistroRequest();
        request.setRut("15.834.207-3");
        request.setCorreo("nuevo@correo.ejemplo");
        request.setClave("password123");
        request.setConfirmacionClave("password123");
        when(usuarioRepository.existsByRut("15.834.207-3")).thenReturn(true);

        assertThrows(ConflictException.class, () -> authService.registrar(request));
    }

    @Test
    void registroRechazaCorreoDuplicado() {
        RegistroRequest request = new RegistroRequest();
        request.setRut("15.834.207-3");
        request.setCorreo("camila@correo.ejemplo");
        request.setClave("password123");
        request.setConfirmacionClave("password123");
        when(usuarioRepository.existsByRut(anyString())).thenReturn(false);
        when(usuarioRepository.existsByCorreo("camila@correo.ejemplo")).thenReturn(true);

        assertThrows(ConflictException.class, () -> authService.registrar(request));
    }

    @Test
    void registroExitosoCreaUsuario() {
        RegistroRequest request = new RegistroRequest();
        request.setRut("15.834.207-3");
        request.setNombres("Camila");
        request.setApellidos("Rojas");
        request.setCorreo("nueva@correo.ejemplo");
        request.setComunaId(1L);
        request.setClave("password123");
        request.setConfirmacionClave("password123");

        Rol rolUsuario = new Rol(1L, "USUARIO");
        Comuna comuna = new Comuna(1L, "Maipú");
        when(usuarioRepository.existsByRut(anyString())).thenReturn(false);
        when(usuarioRepository.existsByCorreo(anyString())).thenReturn(false);
        when(rolRepository.findByNombre("USUARIO")).thenReturn(Optional.of(rolUsuario));
        when(comunaRepository.findById(1L)).thenReturn(Optional.of(comuna));
        when(passwordEncoder.encode("password123")).thenReturn("$2a$hash");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> {
            Usuario u = inv.getArgument(0);
            u.setId(50L);
            return u;
        });

        var perfil = authService.registrar(request);

        assertNotNull(perfil);
        assertEquals("nueva@correo.ejemplo", perfil.getCorreo());
        assertEquals("USUARIO", perfil.getRol());
        assertTrue(perfil.getAvisosActivos());
        assertEquals(3, perfil.getAvisosDias());
    }

    @Test
    void solicitarRecuperacionGeneraCodigoYGuarda() {
        when(usuarioRepository.findByCorreo("camila@correo.ejemplo")).thenReturn(Optional.of(usuarioActivo));
        when(recuperacionRepository.save(any(RecuperacionClave.class))).thenAnswer(inv -> inv.getArgument(0));

        RecuperarClaveSolicitudRequest req = new RecuperarClaveSolicitudRequest();
        req.setCorreo("camila@correo.ejemplo");
        authService.solicitarRecuperacion(req);

        ArgumentCaptor<RecuperacionClave> captor = ArgumentCaptor.forClass(RecuperacionClave.class);
        verify(recuperacionRepository).save(captor.capture());
        RecuperacionClave rec = captor.getValue();
        assertEquals(usuarioActivo, rec.getUsuario());
        assertNotNull(rec.getCodigo());
        assertEquals(6, rec.getCodigo().length());
        assertFalse(rec.getUsado());
        assertTrue(rec.getFechaExpiracion().isAfter(LocalDateTime.now()));
        assertTrue(rec.getFechaExpiracion().isBefore(LocalDateTime.now().plusMinutes(20)));
    }

    @Test
    void solicitarRecuperacionFallaCorreoNoExiste() {
        when(usuarioRepository.findByCorreo(anyString())).thenReturn(Optional.empty());

        RecuperarClaveSolicitudRequest req = new RecuperarClaveSolicitudRequest();
        req.setCorreo("noexiste@correo.ejemplo");
        assertThrows(ResourceNotFoundException.class, () -> authService.solicitarRecuperacion(req));
    }

    @Test
    void confirmarRecuperacionExitoso() {
        RecuperacionClave rec = new RecuperacionClave();
        rec.setUsuario(usuarioActivo);
        rec.setCodigo("123456");
        rec.setFechaCreacion(LocalDateTime.now());
        rec.setFechaExpiracion(LocalDateTime.now().plusMinutes(10));
        rec.setUsado(false);

        when(usuarioRepository.findByCorreo("camila@correo.ejemplo")).thenReturn(Optional.of(usuarioActivo));
        when(recuperacionRepository.findTopByUsuarioAndCodigoAndUsadoFalseOrderByFechaCreacionDesc(usuarioActivo, "123456")).thenReturn(Optional.of(rec));
        when(passwordEncoder.encode("nuevaClave123")).thenReturn("$2a$newhash");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));
        when(recuperacionRepository.save(any(RecuperacionClave.class))).thenAnswer(inv -> inv.getArgument(0));

        RecuperarClaveConfirmacionRequest req = new RecuperarClaveConfirmacionRequest();
        req.setCorreo("camila@correo.ejemplo");
        req.setCodigo("123456");
        req.setNuevaClave("nuevaClave123");
        req.setConfirmacionClave("nuevaClave123");
        authService.confirmarRecuperacion(req);

        assertEquals("$2a$newhash", usuarioActivo.getClaveHash());
        assertTrue(rec.getUsado());
    }

    @Test
    void confirmarRecuperacionFallaClavesNoCoinciden() {
        RecuperarClaveConfirmacionRequest req = new RecuperarClaveConfirmacionRequest();
        req.setCorreo("camila@correo.ejemplo");
        req.setCodigo("123456");
        req.setNuevaClave("clave1");
        req.setConfirmacionClave("clave2");
        assertThrows(BadRequestException.class, () -> authService.confirmarRecuperacion(req));
    }

    @Test
    void confirmarRecuperacionFallaCodigoInvalido() {
        when(usuarioRepository.findByCorreo("camila@correo.ejemplo")).thenReturn(Optional.of(usuarioActivo));
        when(recuperacionRepository.findTopByUsuarioAndCodigoAndUsadoFalseOrderByFechaCreacionDesc(usuarioActivo, "999999")).thenReturn(Optional.empty());

        RecuperarClaveConfirmacionRequest req = new RecuperarClaveConfirmacionRequest();
        req.setCorreo("camila@correo.ejemplo");
        req.setCodigo("999999");
        req.setNuevaClave("clave123");
        req.setConfirmacionClave("clave123");
        assertThrows(BadRequestException.class, () -> authService.confirmarRecuperacion(req));
    }

    @Test
    void confirmarRecuperacionFallaCodigoExpirado() {
        RecuperacionClave rec = new RecuperacionClave();
        rec.setUsuario(usuarioActivo);
        rec.setCodigo("123456");
        rec.setFechaCreacion(LocalDateTime.now().minusMinutes(20));
        rec.setFechaExpiracion(LocalDateTime.now().minusMinutes(5));
        rec.setUsado(false);

        when(usuarioRepository.findByCorreo("camila@correo.ejemplo")).thenReturn(Optional.of(usuarioActivo));
        when(recuperacionRepository.findTopByUsuarioAndCodigoAndUsadoFalseOrderByFechaCreacionDesc(usuarioActivo, "123456")).thenReturn(Optional.of(rec));

        RecuperarClaveConfirmacionRequest req = new RecuperarClaveConfirmacionRequest();
        req.setCorreo("camila@correo.ejemplo");
        req.setCodigo("123456");
        req.setNuevaClave("clave123");
        req.setConfirmacionClave("clave123");
        assertThrows(BadRequestException.class, () -> authService.confirmarRecuperacion(req));
    }

    @Test
    void cambiarClaveExitoso() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioActivo));
        when(passwordEncoder.matches("actual123", "$2a$10$hash")).thenReturn(true);
        when(passwordEncoder.encode("nuevaClave123")).thenReturn("$2a$newhash");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        CambiarClaveRequest req = new CambiarClaveRequest();
        req.setClaveActual("actual123");
        req.setNuevaClave("nuevaClave123");
        req.setConfirmacionClave("nuevaClave123");
        authService.cambiarClave(1L, req);

        assertEquals("$2a$newhash", usuarioActivo.getClaveHash());
    }

    @Test
    void cambiarClaveFallaClavesNoCoinciden() {
        CambiarClaveRequest req = new CambiarClaveRequest();
        req.setClaveActual("actual123");
        req.setNuevaClave("nueva1");
        req.setConfirmacionClave("nueva2");
        assertThrows(BadRequestException.class, () -> authService.cambiarClave(1L, req));
    }

    @Test
    void cambiarClaveFallaClaveActualIncorrecta() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioActivo));
        when(passwordEncoder.matches("mala", "$2a$10$hash")).thenReturn(false);

        CambiarClaveRequest req = new CambiarClaveRequest();
        req.setClaveActual("mala");
        req.setNuevaClave("nueva123");
        req.setConfirmacionClave("nueva123");
        assertThrows(UnauthorizedException.class, () -> authService.cambiarClave(1L, req));
    }

    @Test
    void cambiarClaveFallaUsuarioNoEncontrado() {
        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

        CambiarClaveRequest req = new CambiarClaveRequest();
        req.setClaveActual("actual123");
        req.setNuevaClave("nueva123");
        req.setConfirmacionClave("nueva123");
        assertThrows(ResourceNotFoundException.class, () -> authService.cambiarClave(99L, req));
    }

    @Test
    void obtenerPerfilExitoso() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioActivo));

        PerfilResponse perfil = authService.obtenerPerfil(1L);

        assertEquals(1L, perfil.getId());
        assertEquals("camila@correo.ejemplo", perfil.getCorreo());
        assertEquals("USUARIO", perfil.getRol());
    }

    @Test
    void obtenerPerfilFallaNoEncontrado() {
        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> authService.obtenerPerfil(99L));
    }

    @Test
    void actualizarPerfilExitoso() {
        Comuna comuna = new Comuna(2L, "Santiago");
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioActivo));
        when(comunaRepository.findById(2L)).thenReturn(Optional.of(comuna));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        ActualizarPerfilRequest req = new ActualizarPerfilRequest();
        req.setNombres("Camila");
        req.setApellidos("Rojas Peña");
        req.setTelefono("+56 9 1234 5678");
        req.setComunaId(2L);
        PerfilResponse perfil = authService.actualizarPerfil(1L, req);

        assertEquals("Camila", perfil.getNombres());
        assertEquals("Rojas Peña", perfil.getApellidos());
        assertEquals("+56 9 1234 5678", perfil.getTelefono());
        assertEquals(2L, perfil.getComunaId());
    }

    @Test
    void actualizarPerfilFallaComunaNoEncontrada() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioActivo));
        when(comunaRepository.findById(99L)).thenReturn(Optional.empty());

        ActualizarPerfilRequest req = new ActualizarPerfilRequest();
        req.setNombres("C");
        req.setApellidos("R");
        req.setTelefono("+56 9 1234 5678");
        req.setComunaId(99L);
        assertThrows(ResourceNotFoundException.class, () -> authService.actualizarPerfil(1L, req));
    }

    @Test
    void preferenciasRechazaDiasInvalidos() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioActivo));
        PreferenciasAvisosRequest req = new PreferenciasAvisosRequest();
        req.setAvisosActivos(true);
        req.setAvisosDias(2);

        assertThrows(BadRequestException.class, () -> authService.actualizarPreferencias(1L, req));
    }

    @Test
    void preferenciasDesactivarPoneDiasCero() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioActivo));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));
        PreferenciasAvisosRequest req = new PreferenciasAvisosRequest();
        req.setAvisosActivos(false);
        req.setAvisosDias(3);

        var perfil = authService.actualizarPreferencias(1L, req);

        assertEquals(false, perfil.getAvisosActivos());
        assertEquals(0, perfil.getAvisosDias());
    }

    @Test
    void preferenciasActivarConDiasValidos() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioActivo));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));
        PreferenciasAvisosRequest req = new PreferenciasAvisosRequest();
        req.setAvisosActivos(true);
        req.setAvisosDias(5);

        var perfil = authService.actualizarPreferencias(1L, req);

        assertEquals(true, perfil.getAvisosActivos());
        assertEquals(5, perfil.getAvisosDias());
    }

    @Test
    void perfilNoEncontradoLanzaNotFound() {
        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> authService.obtenerPerfil(99L));
    }
}
