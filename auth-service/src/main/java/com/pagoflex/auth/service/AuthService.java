package com.pagoflex.auth.service;

import com.pagoflex.auth.dto.ActualizarPerfilRequest;
import com.pagoflex.auth.dto.CambiarClaveRequest;
import com.pagoflex.auth.dto.LoginRequest;
import com.pagoflex.auth.dto.LoginResponse;
import com.pagoflex.auth.dto.PerfilResponse;
import com.pagoflex.auth.dto.PersonaResponse;
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
import com.pagoflex.auth.util.RutValidator;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final Set<Integer> DIAS_VALIDOS = Set.of(1, 3, 5);

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final ComunaRepository comunaRepository;
    private final RecuperacionClaveRepository recuperacionRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final SecureRandom random = new SecureRandom();

    public AuthService(
            UsuarioRepository usuarioRepository,
            RolRepository rolRepository,
            ComunaRepository comunaRepository,
            RecuperacionClaveRepository recuperacionRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.comunaRepository = comunaRepository;
        this.recuperacionRepository = recuperacionRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public LoginResponse login(LoginRequest request) {
        Usuario usuario = usuarioRepository.findByCorreo(request.getCorreo().toLowerCase())
                .orElseThrow(() -> new UnauthorizedException("Credenciales inválidas"));
        if (!Boolean.TRUE.equals(usuario.getActivo())) {
            throw new UnauthorizedException("Usuario inactivo");
        }
        if (!passwordEncoder.matches(request.getClave(), usuario.getClaveHash())) {
            throw new UnauthorizedException("Credenciales inválidas");
        }
        String token = jwtService.generateToken(usuario);
        String nombre = usuario.getNombres() + " " + usuario.getApellidos();
        return new LoginResponse(token, usuario.getId(), nombre, usuario.getCorreo(),
                usuario.getRol().getNombre(), usuario.getEmpresaId());
    }

    public PersonaResponse buscarPersonaPorRut(String rut) {
        if (!RutValidator.isValid(rut)) {
            throw new BadRequestException("RUT inválido");
        }
        Usuario usuario = usuarioRepository.findByRut(RutValidator.normalize(rut))
                .filter(u -> "USUARIO".equals(u.getRol().getNombre()))
                .orElseThrow(() -> new ResourceNotFoundException("La persona no está registrada en PagoFlex"));
        return new PersonaResponse(usuario.getId(), usuario.getRut(), usuario.getNombres(), usuario.getApellidos());
    }

    @Transactional
    public PerfilResponse registrar(RegistroRequest request) {
        if (!RutValidator.isValid(request.getRut())) {
            throw new BadRequestException("RUT inválido");
        }
        if (!request.getClave().equals(request.getConfirmacionClave())) {
            throw new BadRequestException("Las claves no coinciden");
        }
        String rutNormalizado = RutValidator.normalize(request.getRut());
        String correo = request.getCorreo().toLowerCase();
        if (usuarioRepository.existsByRut(rutNormalizado)) {
            throw new ConflictException("Ya existe un usuario con ese RUT");
        }
        if (usuarioRepository.existsByCorreo(correo)) {
            throw new ConflictException("Ya existe un usuario con ese correo");
        }
        Rol rol = rolRepository.findByNombre("USUARIO")
                .orElseThrow(() -> new ResourceNotFoundException("Rol USUARIO no encontrado"));
        Comuna comuna = comunaRepository.findById(request.getComunaId())
                .orElseThrow(() -> new ResourceNotFoundException("Comuna no encontrada"));

        Usuario usuario = new Usuario();
        usuario.setRut(rutNormalizado);
        usuario.setNombres(request.getNombres());
        usuario.setApellidos(request.getApellidos());
        usuario.setCorreo(correo);
        usuario.setTelefono(request.getTelefono());
        usuario.setComuna(comuna);
        usuario.setRol(rol);
        usuario.setClaveHash(passwordEncoder.encode(request.getClave()));
        usuario.setActivo(true);
        usuario.setFechaIncorporacion(LocalDate.now());
        usuario.setAvisosActivos(true);
        usuario.setAvisosDias(3);
        Usuario guardado = usuarioRepository.save(usuario);
        return toPerfil(guardado);
    }

    @Transactional
    public void solicitarRecuperacion(RecuperarClaveSolicitudRequest request) {
        Usuario usuario = usuarioRepository.findByCorreo(request.getCorreo().toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("No existe un usuario con ese correo"));
        String codigo = String.format("%06d", random.nextInt(1_000_000));
        RecuperacionClave rec = new RecuperacionClave();
        rec.setUsuario(usuario);
        rec.setCodigo(codigo);
        rec.setFechaCreacion(LocalDateTime.now());
        rec.setFechaExpiracion(LocalDateTime.now().plusMinutes(15));
        rec.setUsado(false);
        recuperacionRepository.save(rec);
        log.info("Código de recuperación para {}: {}", usuario.getCorreo(), codigo);
    }

    @Transactional
    public void confirmarRecuperacion(RecuperarClaveConfirmacionRequest request) {
        if (!request.getNuevaClave().equals(request.getConfirmacionClave())) {
            throw new BadRequestException("Las claves no coinciden");
        }
        Usuario usuario = usuarioRepository.findByCorreo(request.getCorreo().toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("No existe un usuario con ese correo"));
        RecuperacionClave rec = recuperacionRepository
                .findTopByUsuarioAndCodigoAndUsadoFalseOrderByFechaCreacionDesc(usuario, request.getCodigo())
                .orElseThrow(() -> new BadRequestException("Código inválido"));
        if (rec.getFechaExpiracion().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("El código expiró");
        }
        usuario.setClaveHash(passwordEncoder.encode(request.getNuevaClave()));
        usuarioRepository.save(usuario);
        rec.setUsado(true);
        recuperacionRepository.save(rec);
    }

    @Transactional
    public void cambiarClave(Long usuarioId, CambiarClaveRequest request) {
        if (!request.getNuevaClave().equals(request.getConfirmacionClave())) {
            throw new BadRequestException("Las claves no coinciden");
        }
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        if (!passwordEncoder.matches(request.getClaveActual(), usuario.getClaveHash())) {
            throw new UnauthorizedException("La clave actual no es correcta");
        }
        usuario.setClaveHash(passwordEncoder.encode(request.getNuevaClave()));
        usuarioRepository.save(usuario);
    }

    @Transactional(readOnly = true)
    public PerfilResponse obtenerPerfil(Long usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        return toPerfil(usuario);
    }

    @Transactional
    public PerfilResponse actualizarPerfil(Long usuarioId, ActualizarPerfilRequest request) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        Comuna comuna = comunaRepository.findById(request.getComunaId())
                .orElseThrow(() -> new ResourceNotFoundException("Comuna no encontrada"));
        usuario.setNombres(request.getNombres());
        usuario.setApellidos(request.getApellidos());
        usuario.setTelefono(request.getTelefono());
        usuario.setComuna(comuna);
        return toPerfil(usuarioRepository.save(usuario));
    }

    @Transactional
    public PerfilResponse actualizarPreferencias(Long usuarioId, PreferenciasAvisosRequest request) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        boolean activos = Boolean.TRUE.equals(request.getAvisosActivos());
        if (activos && !DIAS_VALIDOS.contains(request.getAvisosDias())) {
            throw new BadRequestException("Los días de aviso deben ser 1, 3 o 5");
        }
        usuario.setAvisosActivos(activos);
        usuario.setAvisosDias(activos ? request.getAvisosDias() : 0);
        return toPerfil(usuarioRepository.save(usuario));
    }

    private PerfilResponse toPerfil(Usuario u) {
        PerfilResponse p = new PerfilResponse();
        p.setId(u.getId());
        p.setRut(u.getRut());
        p.setNombres(u.getNombres());
        p.setApellidos(u.getApellidos());
        p.setCorreo(u.getCorreo());
        p.setTelefono(u.getTelefono());
        if (u.getComuna() != null) {
            p.setComunaId(u.getComuna().getId());
            p.setComunaNombre(u.getComuna().getNombre());
        }
        p.setRol(u.getRol().getNombre());
        p.setEmpresaId(u.getEmpresaId());
        if (u.getArea() != null) {
            p.setAreaId(u.getArea().getId());
            p.setAreaNombre(u.getArea().getNombre());
        }
        p.setAvisosActivos(u.getAvisosActivos());
        p.setAvisosDias(u.getAvisosDias());
        return p;
    }
}
