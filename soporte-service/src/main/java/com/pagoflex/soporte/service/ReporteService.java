package com.pagoflex.soporte.service;

import com.pagoflex.soporte.client.CompromisoClient;
import com.pagoflex.soporte.dto.CompromisoDto;
import com.pagoflex.soporte.dto.ReporteRequest;
import com.pagoflex.soporte.dto.ReporteResponse;
import com.pagoflex.soporte.entity.EvidenciaReporte;
import com.pagoflex.soporte.entity.Reporte;
import com.pagoflex.soporte.exception.ApiException;
import com.pagoflex.soporte.repository.EvidenciaRepository;
import com.pagoflex.soporte.repository.ReporteRepository;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class ReporteService {

    private static final int TAMANO_MAXIMO = 2 * 1024 * 1024;

    public static final String ABIERTO = "ABIERTO";
    public static final String EN_GESTION = "EN_GESTION";
    public static final String RESUELTO = "RESUELTO";

    private static final List<String> MOTIVOS = List.of(
            "Ya pagué por otro medio", "No reconozco este cobro", "El monto no corresponde", "Otro");

    private final ReporteRepository reporteRepository;
    private final EvidenciaRepository evidenciaRepository;
    private final CompromisoClient compromisoClient;
    private final Clock clock;

    public ReporteService(ReporteRepository reporteRepository, EvidenciaRepository evidenciaRepository,
            CompromisoClient compromisoClient, Clock clock) {
        this.reporteRepository = reporteRepository;
        this.evidenciaRepository = evidenciaRepository;
        this.compromisoClient = compromisoClient;
        this.clock = clock;
    }

    public ReporteResponse crear(Long usuarioId, ReporteRequest request) {
        if (!MOTIVOS.contains(request.motivo())) {
            throw ApiException.badRequest("Elige uno de los motivos de la lista.");
        }
        boolean sinComentario = request.comentario() == null || request.comentario().isBlank();
        if ("Otro".equals(request.motivo()) && sinComentario) {
            throw ApiException.badRequest("Cuéntanos qué ocurrió en el comentario.");
        }
        CompromisoDto compromiso = compromisoClient.obtener(request.compromisoId());
        if (!compromiso.usuarioId().equals(usuarioId)) {
            throw ApiException.notFound("No encontramos ese compromiso.");
        }
        Optional<Reporte> existente = reporteRepository.findFirstByCompromisoIdAndEstadoIn(
                request.compromisoId(), List.of(ABIERTO, EN_GESTION));
        if (existente.isPresent()) {
            throw ApiException.conflict("Ya existe el reporte " + existente.get().getNumero()
                    + " para este compromiso. Puedes ver su estado en Mis reportes.");
        }
        compromisoClient.marcarRevision(request.compromisoId());
        Reporte reporte = new Reporte();
        reporte.setNumero(String.format("REP-%04d", 20 + reporteRepository.count()));
        reporte.setCompromisoId(request.compromisoId());
        reporte.setFolio(compromiso.folio());
        reporte.setUsuarioId(usuarioId);
        reporte.setMotivo(request.motivo());
        reporte.setComentario(sinComentario ? null : request.comentario().trim());
        reporte.setFecha(LocalDateTime.now(clock));
        reporte.setEstado(ABIERTO);
        try {
            return toResponse(reporteRepository.save(reporte));
        } catch (RuntimeException e) {
            compromisoClient.restaurar(request.compromisoId());
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "No pudimos registrar tu reporte. Intenta de nuevo en un momento.");
        }
    }

    public List<ReporteResponse> listar(Long userId, String rol, String estado) {
        List<Reporte> base;
        if ("USUARIO".equals(rol)) {
            base = reporteRepository.findByUsuarioIdOrderByFechaDesc(userId);
        } else if ("AGENTE".equals(rol)) {
            base = reporteRepository.findAllByOrderByFechaDesc();
        } else {
            throw ApiException.forbidden("Tu perfil no tiene acceso a los reportes.");
        }
        return base.stream()
                .filter(r -> estado == null || estado.isBlank() || r.getEstado().equalsIgnoreCase(estado))
                .map(this::toResponse)
                .toList();
    }

    public ReporteResponse obtener(Long id, Long userId, String rol) {
        Reporte reporte = buscar(id);
        boolean esAgente = "AGENTE".equals(rol);
        boolean esDelUsuario = "USUARIO".equals(rol) && reporte.getUsuarioId().equals(userId);
        if (!esAgente && !esDelUsuario) {
            throw ApiException.notFound("No encontramos ese reporte.");
        }
        return toResponse(reporte);
    }

    public ReporteResponse obtenerPorCompromiso(Long compromisoId, String rol, Long empresaId) {
        if (!"EJECUTIVO".equals(rol) || empresaId == null) {
            throw ApiException.forbidden("Tu perfil no tiene acceso a esta opción.");
        }
        CompromisoDto compromiso = compromisoClient.obtener(compromisoId);
        if (!empresaId.equals(compromiso.empresaId())) {
            throw ApiException.notFound("No encontramos ese compromiso.");
        }
        Reporte reporte = reporteRepository.findFirstByCompromisoIdOrderByFechaDesc(compromisoId)
                .orElseThrow(() -> ApiException.notFound("Este compromiso no tiene reportes."));
        return toResponse(reporte);
    }

    public ReporteResponse tomar(Long id, Long agenteId) {
        Reporte reporte = buscar(id);
        if (!ABIERTO.equals(reporte.getEstado())) {
            throw ApiException.conflict("Este reporte ya está en gestión o resuelto.");
        }
        reporte.setEstado(EN_GESTION);
        reporte.setAgenteId(agenteId);
        return toResponse(reporteRepository.save(reporte));
    }

    public ReporteResponse resolver(Long id, Long agenteId, String respuesta) {
        Reporte reporte = buscar(id);
        if (!EN_GESTION.equals(reporte.getEstado())) {
            throw ApiException.conflict("Primero toma el reporte para poder resolverlo.");
        }
        if (!reporte.getAgenteId().equals(agenteId)) {
            throw ApiException.forbidden("Este reporte lo está gestionando otro agente.");
        }
        compromisoClient.restaurar(reporte.getCompromisoId());
        reporte.setEstado(RESUELTO);
        reporte.setRespuesta(respuesta.trim());
        reporte.setFechaResolucion(LocalDateTime.now(clock));
        return toResponse(reporteRepository.save(reporte));
    }

    public ReporteResponse guardarEvidencia(Long id, Long usuarioId, String imagenBase64) {
        Reporte reporte = buscar(id);
        if (!reporte.getUsuarioId().equals(usuarioId)) {
            throw ApiException.notFound("No encontramos ese reporte.");
        }
        byte[] imagen;
        try {
            imagen = Base64.getDecoder().decode(imagenBase64);
        } catch (IllegalArgumentException e) {
            throw ApiException.badRequest("La imagen no es válida.");
        }
        if (imagen.length == 0 || imagen.length > TAMANO_MAXIMO) {
            throw ApiException.badRequest("La imagen debe pesar menos de 2 MB.");
        }
        EvidenciaReporte evidencia = evidenciaRepository.findByReporteId(id).orElseGet(EvidenciaReporte::new);
        evidencia.setReporteId(id);
        evidencia.setImagen(imagen);
        evidenciaRepository.save(evidencia);
        return toResponse(reporte);
    }

    public String obtenerEvidencia(Long id, Long userId, String rol) {
        obtener(id, userId, rol);
        EvidenciaReporte evidencia = evidenciaRepository.findByReporteId(id)
                .orElseThrow(() -> ApiException.notFound("Este reporte no tiene imagen adjunta."));
        return Base64.getEncoder().encodeToString(evidencia.getImagen());
    }

    private Reporte buscar(Long id) {
        return reporteRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("No encontramos ese reporte."));
    }

    private boolean estaAtrasado(Reporte reporte) {
        if (!ABIERTO.equals(reporte.getEstado())) {
            return false;
        }
        LocalDate hoy = LocalDate.now(clock);
        int habiles = 0;
        for (LocalDate dia = reporte.getFecha().toLocalDate().plusDays(1); !dia.isAfter(hoy); dia = dia.plusDays(1)) {
            if (dia.getDayOfWeek() != DayOfWeek.SATURDAY && dia.getDayOfWeek() != DayOfWeek.SUNDAY) {
                habiles++;
            }
        }
        return habiles > 2;
    }

    private ReporteResponse toResponse(Reporte r) {
        return new ReporteResponse(r.getId(), r.getNumero(), r.getCompromisoId(), r.getFolio(), r.getUsuarioId(),
                r.getMotivo(), r.getComentario(), r.getFecha(), r.getEstado(), r.getAgenteId(), r.getRespuesta(),
                estaAtrasado(r), r.getFechaResolucion(),
                r.getId() != null && evidenciaRepository.existsByReporteId(r.getId()));
    }
}
