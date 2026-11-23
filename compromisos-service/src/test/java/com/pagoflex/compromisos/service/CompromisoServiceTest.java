package com.pagoflex.compromisos.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pagoflex.compromisos.client.PersonaClient;
import com.pagoflex.compromisos.dto.AnularRequest;
import com.pagoflex.compromisos.dto.CobranzaResponse;
import com.pagoflex.compromisos.dto.CompromisoResponse;
import com.pagoflex.compromisos.dto.CrearCompromisoRequest;
import com.pagoflex.compromisos.dto.PersonaResponse;
import com.pagoflex.compromisos.dto.ResumenResponse;
import com.pagoflex.compromisos.entity.Compromiso;
import com.pagoflex.compromisos.entity.EmpresaCliente;
import com.pagoflex.compromisos.exception.ApiException;
import com.pagoflex.compromisos.repository.CompromisoRepository;
import com.pagoflex.compromisos.repository.EmpresaClienteRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.mockito.ArgumentCaptor;
import static org.mockito.Mockito.times;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CompromisoServiceTest {

    private static final LocalDate HOY = LocalDate.of(2026, 11, 22);

    @Mock
    private CompromisoRepository compromisoRepository;
    @Mock
    private EmpresaClienteRepository empresaRepository;
    @Mock
    private PersonaClient personaClient;

    private CompromisoService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-11-22T15:00:00Z"), ZoneId.of("America/Santiago"));
        service = new CompromisoService(compromisoRepository, empresaRepository, personaClient, clock);
        EmpresaCliente altos = new EmpresaCliente();
        altos.setId(1L);
        altos.setNombre("Altos del Maipo");
        altos.setRecargo(8500);
        EmpresaCliente colegio = new EmpresaCliente();
        colegio.setId(2L);
        colegio.setNombre("Colegio Bosque Nativo");
        colegio.setRecargo(5000);
        when(empresaRepository.findAll()).thenReturn(List.of(altos, colegio));
        when(compromisoRepository.save(any(Compromiso.class))).thenAnswer(i -> i.getArgument(0));
    }

    private Compromiso compromiso(Long id, Long usuarioId, Long empresaId, String estado, LocalDate vencimiento) {
        Compromiso c = new Compromiso();
        c.setId(id);
        c.setFolio("CP-2026-01" + id);
        c.setUsuarioId(usuarioId);
        c.setEmpresaId(empresaId);
        c.setConcepto("Cuota");
        c.setCuotaNumero(1);
        c.setCuotaTotal(12);
        c.setMonto(100000);
        c.setVencimiento(vencimiento);
        c.setEstado(estado);
        return c;
    }

    private CrearCompromisoRequest solicitud(Integer cuota, Integer total, LocalDate vencimiento) {
        return new CrearCompromisoRequest("15.834.207-3", "Cuota pie", 310000, vencimiento, cuota, total);
    }

    @Test
    void listarDelUsuarioMarcaVencidoYCalculaRecargo() {
        when(compromisoRepository.findByUsuarioIdOrderByVencimientoAsc(1L))
                .thenReturn(List.of(compromiso(1L, 1L, 1L, "PENDIENTE", HOY.minusDays(6))));

        List<CompromisoResponse> lista = service.listar(1L, "USUARIO", null, null);

        assertEquals(1, lista.size());
        assertEquals("VENCIDO", lista.get(0).estado());
        assertEquals(6, lista.get(0).diasAtraso());
        assertEquals(8500, lista.get(0).recargo());
        assertEquals(108500, lista.get(0).total());
    }

    @Test
    void listarPendienteNoTieneRecargo() {
        when(compromisoRepository.findByUsuarioIdOrderByVencimientoAsc(1L))
                .thenReturn(List.of(compromiso(1L, 1L, 1L, "PENDIENTE", HOY)));

        CompromisoResponse r = service.listar(1L, "USUARIO", null, null).get(0);

        assertEquals("PENDIENTE", r.estado());
        assertEquals(0, r.recargo());
        assertEquals(100000, r.total());
    }

    @Test
    void listarFiltraPorEstado() {
        when(compromisoRepository.findByUsuarioIdOrderByVencimientoAsc(1L)).thenReturn(List.of(
                compromiso(1L, 1L, 1L, "PENDIENTE", HOY.minusDays(1)),
                compromiso(2L, 1L, 1L, "PENDIENTE", HOY.plusDays(5))));

        assertEquals(1, service.listar(1L, "USUARIO", null, "vencido").size());
        assertEquals(2, service.listar(1L, "USUARIO", null, " ").size());
    }

    @Test
    void listarDelEjecutivoUsaSuEmpresa() {
        when(compromisoRepository.findByEmpresaIdOrderByVencimientoAsc(2L))
                .thenReturn(List.of(compromiso(3L, 5L, 2L, "PENDIENTE", HOY)));

        assertEquals(1, service.listar(15L, "EJECUTIVO", 2L, null).size());
    }

    @Test
    void listarEjecutivoSinEmpresaEsProhibido() {
        ApiException ex = assertThrows(ApiException.class, () -> service.listar(15L, "EJECUTIVO", null, null));
        assertEquals(403, ex.getStatus().value());
    }

    @Test
    void listarAgenteEsProhibido() {
        ApiException ex = assertThrows(ApiException.class, () -> service.listar(9L, "AGENTE", null, null));
        assertEquals(403, ex.getStatus().value());
    }

    @Test
    void obtenerDeOtroUsuarioNoSeEncuentra() {
        when(compromisoRepository.findById(1L)).thenReturn(Optional.of(compromiso(1L, 2L, 1L, "PENDIENTE", HOY)));

        ApiException ex = assertThrows(ApiException.class, () -> service.obtener(1L, 1L, "USUARIO", null));
        assertEquals(404, ex.getStatus().value());
    }

    @Test
    void obtenerDeOtraEmpresaNoSeEncuentra() {
        when(compromisoRepository.findById(1L)).thenReturn(Optional.of(compromiso(1L, 2L, 1L, "PENDIENTE", HOY)));

        assertThrows(ApiException.class, () -> service.obtener(1L, 15L, "EJECUTIVO", 2L));
    }

    @Test
    void obtenerDelDuenoEntregaElDetalle() {
        when(compromisoRepository.findById(1L)).thenReturn(Optional.of(compromiso(1L, 1L, 1L, "PENDIENTE", HOY)));

        assertEquals("Altos del Maipo", service.obtener(1L, 1L, "USUARIO", null).empresaNombre());
        assertEquals(1L, service.obtener(1L, 14L, "EJECUTIVO", 1L).id());
    }

    @Test
    void obtenerInexistenteEsNoEncontrado() {
        when(compromisoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ApiException.class, () -> service.obtenerInterno(99L));
    }

    @Test
    void resumenSumaLoQueVenceHastaFinDeMesYEncuentraElProximo() {
        when(compromisoRepository.findByUsuarioIdOrderByVencimientoAsc(1L)).thenReturn(List.of(
                compromiso(1L, 1L, 1L, "PENDIENTE", HOY.minusDays(2)),
                compromiso(2L, 1L, 1L, "PENDIENTE", HOY.plusDays(3)),
                compromiso(3L, 1L, 1L, "PENDIENTE", HOY.plusDays(40)),
                compromiso(4L, 1L, 1L, "PAGADO", HOY.minusDays(10))));

        ResumenResponse r = service.resumen(1L);

        assertEquals(108500 + 100000, r.totalPorPagarMes());
        assertEquals(HOY.plusDays(3), r.proximoVencimiento());
        assertEquals(1L, r.vencidos());
    }

    @Test
    void resumenSinCompromisosNoTieneProximoVencimiento() {
        when(compromisoRepository.findByUsuarioIdOrderByVencimientoAsc(1L)).thenReturn(List.of());

        ResumenResponse r = service.resumen(1L);

        assertEquals(0, r.totalPorPagarMes());
        assertNull(r.proximoVencimiento());
    }

    @Test
    void cobranzaCalculaLaTasaDelMes() {
        Compromiso pagado = compromiso(1L, 1L, 1L, "PAGADO", LocalDate.of(2026, 11, 10));
        pagado.setRecargoAplicado(8500);
        Compromiso vencido = compromiso(2L, 2L, 1L, "PENDIENTE", LocalDate.of(2026, 11, 5));
        Compromiso pendiente = compromiso(3L, 3L, 1L, "PENDIENTE", LocalDate.of(2026, 11, 28));
        Compromiso anulado = compromiso(4L, 4L, 1L, "ANULADO", LocalDate.of(2026, 11, 12));
        Compromiso otroMes = compromiso(5L, 5L, 1L, "PENDIENTE", LocalDate.of(2026, 12, 12));
        when(compromisoRepository.findByEmpresaIdOrderByVencimientoAsc(1L))
                .thenReturn(List.of(pagado, vencido, pendiente, anulado, otroMes));

        CobranzaResponse r = service.cobranza(1L, null);

        assertEquals("2026-11", r.mes());
        assertEquals(4L, r.compromisos());
        assertEquals(1L, r.anulados());
        assertEquals(1L, r.pagados());
        assertEquals(1L, r.vencidos());
        assertEquals(1L, r.pendientes());
        assertEquals(108500, r.montoRecaudado());
        assertEquals(33.3, r.tasa());
    }

    @Test
    void cobranzaSinCompromisosTieneTasaCero() {
        when(compromisoRepository.findByEmpresaIdOrderByVencimientoAsc(1L)).thenReturn(List.of());

        assertEquals(0.0, service.cobranza(1L, YearMonth.of(2026, 9)).tasa());
    }

    @Test
    void crearRegistraCompromisoPendienteConFolio() {
        when(personaClient.buscarPorRut("15.834.207-3"))
                .thenReturn(new PersonaResponse(1L, "15.834.207-3", "Camila", "Rojas"));
        when(compromisoRepository.count()).thenReturn(12L);

        CompromisoResponse r = service.crear(solicitud(14, 36, HOY.plusDays(10)), 1L, 14L);

        assertEquals("CP-2026-0113", r.folio());
        assertEquals("PENDIENTE", r.estado());
        assertEquals(1L, r.usuarioId());
        assertEquals(1L, r.empresaId());
    }

    @Test
    void crearConCuotaIncompletaEsRechazado() {
        assertThrows(ApiException.class, () -> service.crear(solicitud(3, null, HOY), 1L, 14L));
        verify(compromisoRepository, never()).save(any());
    }

    @Test
    void crearConCuotaMayorAlTotalEsRechazado() {
        assertThrows(ApiException.class, () -> service.crear(solicitud(13, 12, HOY), 1L, 14L));
    }

    @Test
    void crearConVencimientoPasadoEsRechazado() {
        ApiException ex = assertThrows(ApiException.class,
                () -> service.crear(solicitud(1, 12, HOY.minusDays(1)), 1L, 14L));
        assertEquals(400, ex.getStatus().value());
    }

    @Test
    void crearDuplicadoEsConflicto() {
        when(personaClient.buscarPorRut("15.834.207-3"))
                .thenReturn(new PersonaResponse(1L, "15.834.207-3", "Camila", "Rojas"));
        when(compromisoRepository.existsByUsuarioIdAndEmpresaIdAndConceptoAndCuotaNumero(1L, 1L, "Cuota pie", 14))
                .thenReturn(true);

        ApiException ex = assertThrows(ApiException.class, () -> service.crear(solicitud(14, 36, HOY), 1L, 14L));
        assertEquals(409, ex.getStatus().value());
    }

    @Test
    void anularRegistraMotivoYEjecutivo() {
        when(compromisoRepository.findById(1L)).thenReturn(Optional.of(compromiso(1L, 1L, 1L, "PENDIENTE", HOY)));

        CompromisoResponse r = service.anular(1L, new AnularRequest("Repactación", null), 1L, 14L);

        assertEquals("ANULADO", r.estado());
        assertEquals("Repactación", r.motivoAnulacion());
    }

    @Test
    void anularConOtroGuardaElComentario() {
        when(compromisoRepository.findById(1L)).thenReturn(Optional.of(compromiso(1L, 1L, 1L, "EN_REVISION", HOY)));

        CompromisoResponse r = service.anular(1L, new AnularRequest("Otro", " cobro duplicado "), 1L, 14L);

        assertEquals("Otro: cobro duplicado", r.motivoAnulacion());
    }

    @Test
    void anularConOtroSinComentarioEsRechazado() {
        when(compromisoRepository.findById(1L)).thenReturn(Optional.of(compromiso(1L, 1L, 1L, "PENDIENTE", HOY)));

        assertThrows(ApiException.class, () -> service.anular(1L, new AnularRequest("Otro", " "), 1L, 14L));
    }

    @Test
    void anularConMotivoInvalidoEsRechazado() {
        when(compromisoRepository.findById(1L)).thenReturn(Optional.of(compromiso(1L, 1L, 1L, "PENDIENTE", HOY)));

        assertThrows(ApiException.class, () -> service.anular(1L, new AnularRequest("Porque sí", null), 1L, 14L));
    }

    @Test
    void anularPagadoEsConflicto() {
        when(compromisoRepository.findById(1L)).thenReturn(Optional.of(compromiso(1L, 1L, 1L, "PAGADO", HOY)));

        ApiException ex = assertThrows(ApiException.class,
                () -> service.anular(1L, new AnularRequest("Repactación", null), 1L, 14L));
        assertEquals(409, ex.getStatus().value());
    }

    @Test
    void anularYaAnuladoEsConflicto() {
        when(compromisoRepository.findById(1L)).thenReturn(Optional.of(compromiso(1L, 1L, 1L, "ANULADO", HOY)));

        assertThrows(ApiException.class, () -> service.anular(1L, new AnularRequest("Repactación", null), 1L, 14L));
    }

    @Test
    void anularDeOtraEmpresaNoSeEncuentra() {
        when(compromisoRepository.findById(1L)).thenReturn(Optional.of(compromiso(1L, 1L, 2L, "PENDIENTE", HOY)));

        ApiException ex = assertThrows(ApiException.class,
                () -> service.anular(1L, new AnularRequest("Repactación", null), 1L, 14L));
        assertEquals(404, ex.getStatus().value());
    }

    @Test
    void pagarVencidoAplicaElRecargoDeLaEmpresa() {
        when(compromisoRepository.findById(1L)).thenReturn(Optional.of(compromiso(1L, 1L, 1L, "PENDIENTE", HOY.minusDays(3))));

        CompromisoResponse r = service.pagar(1L, 1L);

        assertEquals("PAGADO", r.estado());
        assertEquals(8500, r.recargo());
        assertEquals(108500, r.total());
        assertTrue(r.fechaPago() != null);
    }

    @Test
    void pagarAlDiaNoCobraRecargo() {
        when(compromisoRepository.findById(1L)).thenReturn(Optional.of(compromiso(1L, 1L, 1L, "PENDIENTE", HOY)));

        assertEquals(0, service.pagar(1L, 1L).recargo());
    }

    @Test
    void pagarDeOtroUsuarioNoSeEncuentra() {
        when(compromisoRepository.findById(1L)).thenReturn(Optional.of(compromiso(1L, 2L, 1L, "PENDIENTE", HOY)));

        ApiException ex = assertThrows(ApiException.class, () -> service.pagar(1L, 1L));
        assertEquals(404, ex.getStatus().value());
    }

    @Test
    void pagarEnRevisionEsConflicto() {
        when(compromisoRepository.findById(1L)).thenReturn(Optional.of(compromiso(1L, 1L, 1L, "EN_REVISION", HOY)));

        assertThrows(ApiException.class, () -> service.pagar(1L, 1L));
    }

    @Test
    void pagarRespetaElOrdenDeVencimientoDeLaEmpresa() {
        Compromiso nuevo = compromiso(2L, 1L, 1L, "PENDIENTE", HOY.plusDays(5));
        when(compromisoRepository.findById(2L)).thenReturn(Optional.of(nuevo));
        when(compromisoRepository.existsByUsuarioIdAndEmpresaIdAndEstadoAndVencimientoBefore(
                eq(1L), eq(1L), eq("PENDIENTE"), any(LocalDate.class))).thenReturn(true);

        ApiException ex = assertThrows(ApiException.class, () -> service.pagar(2L, 1L));
        assertEquals(409, ex.getStatus().value());
        verify(compromisoRepository, never()).save(any());
    }

    @Test
    void pagarUnaCuotaGeneraLaSiguienteConSuNuevoVencimiento() {
        Compromiso c = compromiso(1L, 1L, 1L, "PENDIENTE", HOY.plusDays(2));
        c.setConcepto("Cuota social octubre");
        c.setCuotaNumero(10);
        c.setCuotaTotal(12);
        when(compromisoRepository.findById(1L)).thenReturn(Optional.of(c));

        service.pagar(1L, 1L);

        ArgumentCaptor<Compromiso> captor = ArgumentCaptor.forClass(Compromiso.class);
        verify(compromisoRepository, times(2)).save(captor.capture());
        Compromiso proxima = captor.getAllValues().get(1);
        assertEquals(11, proxima.getCuotaNumero());
        assertEquals(12, proxima.getCuotaTotal());
        assertEquals("PENDIENTE", proxima.getEstado());
        assertEquals(HOY.plusDays(2).plusMonths(1), proxima.getVencimiento());
        assertEquals("Cuota social diciembre", proxima.getConcepto());
        assertEquals(100000, proxima.getMonto());
    }

    @Test
    void pagarLaUltimaCuotaNoGeneraOtra() {
        Compromiso c = compromiso(1L, 1L, 1L, "PENDIENTE", HOY.plusDays(2));
        c.setCuotaNumero(12);
        c.setCuotaTotal(12);
        when(compromisoRepository.findById(1L)).thenReturn(Optional.of(c));

        service.pagar(1L, 1L);

        verify(compromisoRepository, times(1)).save(any(Compromiso.class));
    }

    @Test
    void pagarUnaCuotaCuyaSiguienteYaExisteNoLaDuplica() {
        Compromiso c = compromiso(1L, 1L, 1L, "PENDIENTE", HOY.plusDays(2));
        when(compromisoRepository.findById(1L)).thenReturn(Optional.of(c));
        when(compromisoRepository.existsByUsuarioIdAndEmpresaIdAndConceptoAndCuotaNumero(1L, 1L, "Cuota", 2)).thenReturn(true);

        service.pagar(1L, 1L);

        verify(compromisoRepository, times(1)).save(any(Compromiso.class));
    }

    @Test
    void pagarUnCompromisoSinCuotasNoGeneraOtro() {
        Compromiso c = compromiso(1L, 1L, 1L, "PENDIENTE", HOY.plusDays(2));
        c.setCuotaNumero(null);
        c.setCuotaTotal(null);
        when(compromisoRepository.findById(1L)).thenReturn(Optional.of(c));

        service.pagar(1L, 1L);

        verify(compromisoRepository, times(1)).save(any(Compromiso.class));
    }

    @Test
    void revertirPagoEliminaLaCuotaGeneradaAutomaticamente() {
        Compromiso c = compromiso(1L, 1L, 1L, "PAGADO", HOY);
        Compromiso generada = compromiso(2L, 1L, 1L, "PENDIENTE", HOY.plusMonths(1));
        when(compromisoRepository.findById(1L)).thenReturn(Optional.of(c));
        when(compromisoRepository.findFirstByUsuarioIdAndEmpresaIdAndCuotaNumeroAndEstadoAndRegistradoPorIdIsNull(
                1L, 1L, 2, "PENDIENTE")).thenReturn(Optional.of(generada));

        service.revertirPago(1L);

        verify(compromisoRepository).delete(generada);
    }

    @Test
    void revertirPagoDejaElCompromisoPendiente() {
        Compromiso c = compromiso(1L, 1L, 1L, "PAGADO", HOY);
        c.setRecargoAplicado(0);
        when(compromisoRepository.findById(1L)).thenReturn(Optional.of(c));

        assertEquals("PENDIENTE", service.revertirPago(1L).estado());
    }

    @Test
    void revertirPagoDeUnoNoPagadoEsConflicto() {
        when(compromisoRepository.findById(1L)).thenReturn(Optional.of(compromiso(1L, 1L, 1L, "PENDIENTE", HOY)));

        assertThrows(ApiException.class, () -> service.revertirPago(1L));
    }

    @Test
    void marcarRevisionSoloDesdePendiente() {
        when(compromisoRepository.findById(1L)).thenReturn(Optional.of(compromiso(1L, 1L, 1L, "PENDIENTE", HOY)));
        when(compromisoRepository.findById(2L)).thenReturn(Optional.of(compromiso(2L, 1L, 1L, "PAGADO", HOY)));

        assertEquals("EN_REVISION", service.marcarRevision(1L).estado());
        assertThrows(ApiException.class, () -> service.marcarRevision(2L));
    }

    @Test
    void restaurarSaleDeRevisionYNoTocaLosAnulados() {
        when(compromisoRepository.findById(1L)).thenReturn(Optional.of(compromiso(1L, 1L, 1L, "EN_REVISION", HOY)));
        when(compromisoRepository.findById(2L)).thenReturn(Optional.of(compromiso(2L, 1L, 1L, "ANULADO", HOY)));

        assertEquals("PENDIENTE", service.restaurar(1L).estado());
        assertEquals("ANULADO", service.restaurar(2L).estado());
    }
}
