package com.pagoflex.soporte.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pagoflex.soporte.client.CompromisoClient;
import com.pagoflex.soporte.dto.CompromisoDto;
import com.pagoflex.soporte.dto.ReporteRequest;
import com.pagoflex.soporte.dto.ReporteResponse;
import com.pagoflex.soporte.entity.Reporte;
import com.pagoflex.soporte.exception.ApiException;
import com.pagoflex.soporte.repository.ReporteRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ReporteServiceTest {

    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 11, 23, 11, 0);

    @Mock
    private ReporteRepository reporteRepository;
    @Mock
    private CompromisoClient compromisoClient;

    private ReporteService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-11-23T14:00:00Z"), ZoneId.of("America/Santiago"));
        service = new ReporteService(reporteRepository, compromisoClient, clock);
        when(reporteRepository.save(any(Reporte.class))).thenAnswer(i -> i.getArgument(0));
        when(reporteRepository.count()).thenReturn(6L);
        when(compromisoClient.obtener(7L)).thenReturn(new CompromisoDto(7L, "CP-2026-0106", 1L, "PENDIENTE"));
    }

    private Reporte reporte(Long id, Long usuarioId, String estado, LocalDateTime fecha, Long agenteId) {
        Reporte r = new Reporte();
        r.setId(id);
        r.setNumero("REP-00" + id);
        r.setCompromisoId(7L);
        r.setFolio("CP-2026-0106");
        r.setUsuarioId(usuarioId);
        r.setMotivo("El monto no corresponde");
        r.setEstado(estado);
        r.setFecha(fecha);
        r.setAgenteId(agenteId);
        return r;
    }

    @Test
    void crearRegistraElReporteAbiertoYPasaElCompromisoARevision() {
        ReporteResponse r = service.crear(1L, new ReporteRequest(7L, "El monto no corresponde", " No sé por qué "));

        assertEquals("REP-0026", r.numero());
        assertEquals("ABIERTO", r.estado());
        assertEquals("No sé por qué", r.comentario());
        assertEquals("CP-2026-0106", r.folio());
        verify(compromisoClient).marcarRevision(7L);
    }

    @Test
    void crearConMotivoInvalidoEsRechazado() {
        assertThrows(ApiException.class, () -> service.crear(1L, new ReporteRequest(7L, "Porque sí", null)));
        verify(compromisoClient, never()).marcarRevision(any());
    }

    @Test
    void crearConOtroSinComentarioEsRechazado() {
        ApiException ex = assertThrows(ApiException.class, () -> service.crear(1L, new ReporteRequest(7L, "Otro", " ")));
        assertEquals(400, ex.getStatus().value());
    }

    @Test
    void crearSobreUnCompromisoAjenoNoSeEncuentra() {
        ApiException ex = assertThrows(ApiException.class,
                () -> service.crear(2L, new ReporteRequest(7L, "No reconozco este cobro", null)));
        assertEquals(404, ex.getStatus().value());
    }

    @Test
    void crearConUnReporteAbiertoMuestraElExistente() {
        when(reporteRepository.findFirstByCompromisoIdAndEstadoIn(any(), any()))
                .thenReturn(Optional.of(reporte(12L, 1L, "ABIERTO", AHORA, null)));

        ApiException ex = assertThrows(ApiException.class,
                () -> service.crear(1L, new ReporteRequest(7L, "No reconozco este cobro", null)));

        assertEquals(409, ex.getStatus().value());
        assertTrue(ex.getMessage().contains("REP-0012"));
    }

    @Test
    void siFallaElGuardadoSeRestauraElCompromiso() {
        when(reporteRepository.save(any(Reporte.class))).thenThrow(new IllegalStateException("falla"));

        ApiException ex = assertThrows(ApiException.class,
                () -> service.crear(1L, new ReporteRequest(7L, "No reconozco este cobro", null)));

        assertEquals(500, ex.getStatus().value());
        verify(compromisoClient).restaurar(7L);
    }

    @Test
    void listarDelUsuarioYDelAgente() {
        when(reporteRepository.findByUsuarioIdOrderByFechaDesc(1L)).thenReturn(List.of(reporte(1L, 1L, "ABIERTO", AHORA, null)));
        when(reporteRepository.findAllByOrderByFechaDesc()).thenReturn(List.of(
                reporte(1L, 1L, "ABIERTO", AHORA, null), reporte(2L, 2L, "RESUELTO", AHORA, 9L)));

        assertEquals(1, service.listar(1L, "USUARIO", null).size());
        assertEquals(2, service.listar(9L, "AGENTE", null).size());
        assertEquals(1, service.listar(9L, "AGENTE", "resuelto").size());
        assertEquals(2, service.listar(9L, "AGENTE", " ").size());
    }

    @Test
    void listarComoEjecutivoEsProhibido() {
        assertThrows(ApiException.class, () -> service.listar(14L, "EJECUTIVO", null));
    }

    @Test
    void unReporteAbiertoConMasDeDosDiasHabilesEstaAtrasado() {
        when(reporteRepository.findAllByOrderByFechaDesc()).thenReturn(List.of(
                reporte(1L, 1L, "ABIERTO", LocalDateTime.of(2026, 11, 17, 10, 0), null),
                reporte(2L, 1L, "ABIERTO", LocalDateTime.of(2026, 11, 20, 10, 0), null),
                reporte(3L, 1L, "EN_GESTION", LocalDateTime.of(2026, 11, 10, 10, 0), 9L)));

        List<ReporteResponse> lista = service.listar(9L, "AGENTE", null);

        assertTrue(lista.get(0).atrasado());
        assertFalse(lista.get(1).atrasado());
        assertFalse(lista.get(2).atrasado());
    }

    @Test
    void obtenerRespetaElDuenoYElAgente() {
        when(reporteRepository.findById(1L)).thenReturn(Optional.of(reporte(1L, 1L, "ABIERTO", AHORA, null)));

        assertEquals(1L, service.obtener(1L, 1L, "USUARIO").id());
        assertEquals(1L, service.obtener(1L, 9L, "AGENTE").id());
        assertThrows(ApiException.class, () -> service.obtener(1L, 2L, "USUARIO"));
        assertThrows(ApiException.class, () -> service.obtener(1L, 14L, "EJECUTIVO"));
    }

    @Test
    void obtenerInexistenteEsNoEncontrado() {
        when(reporteRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ApiException.class, () -> service.obtener(99L, 1L, "USUARIO"));
    }

    @Test
    void tomarDejaAlAgenteComoResponsable() {
        when(reporteRepository.findById(1L)).thenReturn(Optional.of(reporte(1L, 1L, "ABIERTO", AHORA, null)));

        ReporteResponse r = service.tomar(1L, 9L);

        assertEquals("EN_GESTION", r.estado());
        assertEquals(9L, r.agenteId());
    }

    @Test
    void tomarUnReporteYaTomadoEsConflicto() {
        when(reporteRepository.findById(1L)).thenReturn(Optional.of(reporte(1L, 1L, "EN_GESTION", AHORA, 10L)));

        assertThrows(ApiException.class, () -> service.tomar(1L, 9L));
    }

    @Test
    void resolverGuardaLaRespuestaYRestauraElCompromiso() {
        when(reporteRepository.findById(1L)).thenReturn(Optional.of(reporte(1L, 1L, "EN_GESTION", AHORA, 9L)));

        ReporteResponse r = service.resolver(1L, 9L, " La empresa corrigió el monto ");

        assertEquals("RESUELTO", r.estado());
        assertEquals("La empresa corrigió el monto", r.respuesta());
        verify(compromisoClient).restaurar(7L);
    }

    @Test
    void resolverSinHaberTomadoElReporteEsConflicto() {
        when(reporteRepository.findById(1L)).thenReturn(Optional.of(reporte(1L, 1L, "ABIERTO", AHORA, null)));

        assertThrows(ApiException.class, () -> service.resolver(1L, 9L, "Listo"));
    }

    @Test
    void resolverUnReporteDeOtroAgenteEsProhibido() {
        when(reporteRepository.findById(1L)).thenReturn(Optional.of(reporte(1L, 1L, "EN_GESTION", AHORA, 10L)));

        ApiException ex = assertThrows(ApiException.class, () -> service.resolver(1L, 9L, "Listo"));
        assertEquals(403, ex.getStatus().value());
    }
}
