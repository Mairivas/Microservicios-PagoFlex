package com.pagoflex.pagos.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pagoflex.pagos.client.CompromisoClient;
import com.pagoflex.pagos.dto.CompromisoDto;
import com.pagoflex.pagos.dto.CumplimientoResponse;
import com.pagoflex.pagos.dto.PagoRequest;
import com.pagoflex.pagos.dto.PagoResponse;
import com.pagoflex.pagos.entity.Pago;
import com.pagoflex.pagos.exception.ApiException;
import com.pagoflex.pagos.repository.PagoRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
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
class PagoServiceTest {

    private static final LocalDate HOY = LocalDate.of(2026, 11, 22);

    @Mock
    private PagoRepository pagoRepository;
    @Mock
    private CompromisoClient compromisoClient;

    private PagoService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-11-22T15:00:00Z"), ZoneId.of("America/Santiago"));
        service = new PagoService(pagoRepository, compromisoClient, clock);
        when(pagoRepository.save(any(Pago.class))).thenAnswer(i -> i.getArgument(0));
        when(pagoRepository.count()).thenReturn(8L);
    }

    private CompromisoDto compromiso(Integer cuota, LocalDate vencimiento, int recargo) {
        return new CompromisoDto(7L, "CP-2026-0106", 1L, "Altos del Maipo", "Cuota pie", cuota, cuota == null ? null : 36,
                310000, recargo, 310000 + recargo, vencimiento);
    }

    private Pago pago(Long id, Long usuarioId, Long empresaId, LocalDateTime fecha, boolean aTiempo) {
        Pago p = new Pago();
        p.setId(id);
        p.setUsuarioId(usuarioId);
        p.setEmpresaId(empresaId);
        p.setFechaHora(fecha);
        p.setATiempo(aTiempo);
        return p;
    }

    private List<Pago> pagos(int total, int aTiempo) {
        List<Pago> lista = new ArrayList<>();
        for (int i = 0; i < total; i++) {
            lista.add(pago((long) i, 1L, 1L, HOY.minusDays(10 + i).atTime(10, 0), i < aTiempo));
        }
        return lista;
    }

    @Test
    void pagarGuardaElComprobanteConElTotalDelServidor() {
        when(compromisoClient.pagar(7L, 1L)).thenReturn(compromiso(14, HOY.minusDays(6), 8500));

        PagoResponse r = service.pagar(1L, new PagoRequest(7L, null));

        assertEquals("CPR-000609", r.comprobante());
        assertEquals(318500, r.montoPagado());
        assertEquals(8500, r.recargo());
        assertEquals("14/36", r.cuota());
        assertEquals("Pago simulado", r.medioPago());
        assertEquals("App", r.canal());
        assertFalse(r.aTiempo());
    }

    @Test
    void pagarAntesDelVencimientoEsATiempoYGuardaElMedio() {
        when(compromisoClient.pagar(7L, 1L)).thenReturn(compromiso(null, HOY.plusDays(2), 0));

        PagoResponse r = service.pagar(1L, new PagoRequest(7L, " Tarjeta ****1234 "));

        assertTrue(r.aTiempo());
        assertEquals("-", r.cuota());
        assertEquals("Tarjeta ****1234", r.medioPago());
    }

    @Test
    void siNoSePuedeGuardarElComprobanteSeRevierteElPago() {
        when(compromisoClient.pagar(7L, 1L)).thenReturn(compromiso(14, HOY, 0));
        when(pagoRepository.save(any(Pago.class))).thenThrow(new IllegalStateException("falla de base de datos"));

        ApiException ex = assertThrows(ApiException.class, () -> service.pagar(1L, new PagoRequest(7L, null)));

        assertEquals(500, ex.getStatus().value());
        verify(compromisoClient).revertirPago(7L);
    }

    @Test
    void historialFiltraPorEmpresaYPeriodo() {
        when(pagoRepository.findByUsuarioIdOrderByFechaHoraDesc(1L)).thenReturn(List.of(
                pago(1L, 1L, 1L, HOY.atTime(9, 0), true),
                pago(2L, 1L, 2L, HOY.minusDays(10).atTime(9, 0), true),
                pago(3L, 1L, 1L, HOY.minusDays(40).atTime(9, 0), true)));

        assertEquals(3, service.historial(1L, null, null, null).size());
        assertEquals(2, service.historial(1L, 1L, null, null).size());
        assertEquals(2, service.historial(1L, null, HOY.minusDays(15), null).size());
        assertEquals(1, service.historial(1L, null, HOY.minusDays(15), HOY.minusDays(5)).size());
    }

    @Test
    void obtenerComprobanteAjenoNoSeEncuentra() {
        when(pagoRepository.findById(1L)).thenReturn(Optional.of(pago(1L, 2L, 1L, HOY.atTime(9, 0), true)));
        when(pagoRepository.findById(2L)).thenReturn(Optional.of(pago(2L, 1L, 1L, HOY.atTime(9, 0), true)));

        assertThrows(ApiException.class, () -> service.obtener(1L, 1L));
        assertEquals(2L, service.obtener(2L, 1L).id());
    }

    @Test
    void cumplimientoConPocosPagosEsHistorialInsuficiente() {
        when(pagoRepository.findByUsuarioIdOrderByFechaHoraDesc(1L)).thenReturn(pagos(2, 2));

        CumplimientoResponse r = service.cumplimiento(1L);

        assertFalse(r.historialSuficiente());
        assertNull(r.porcentaje());
        assertEquals("Historial insuficiente", r.categoria());
    }

    @Test
    void cumplimientoEjemplar() {
        when(pagoRepository.findByUsuarioIdOrderByFechaHoraDesc(1L)).thenReturn(pagos(20, 19));

        CumplimientoResponse r = service.cumplimiento(1L);

        assertEquals(95.0, r.porcentaje());
        assertEquals("Ejemplar", r.categoria());
    }

    @Test
    void cumplimientoAlDiaCon917() {
        when(pagoRepository.findByUsuarioIdOrderByFechaHoraDesc(1L)).thenReturn(pagos(36, 33));

        CumplimientoResponse r = service.cumplimiento(1L);

        assertEquals(91.7, r.porcentaje());
        assertEquals("Al día", r.categoria());
    }

    @Test
    void cumplimientoEnCamino() {
        when(pagoRepository.findByUsuarioIdOrderByFechaHoraDesc(1L)).thenReturn(pagos(12, 8));

        CumplimientoResponse r = service.cumplimiento(1L);

        assertEquals(66.7, r.porcentaje());
        assertEquals("En camino", r.categoria());
    }

    @Test
    void cumplimientoIgnoraPagosDeHaceMasDeUnAnio() {
        List<Pago> lista = new ArrayList<>(pagos(3, 3));
        lista.add(pago(99L, 1L, 1L, HOY.minusYears(2).atTime(9, 0), false));
        when(pagoRepository.findByUsuarioIdOrderByFechaHoraDesc(1L)).thenReturn(lista);

        assertEquals(3, service.cumplimiento(1L).considerados());
    }
}
