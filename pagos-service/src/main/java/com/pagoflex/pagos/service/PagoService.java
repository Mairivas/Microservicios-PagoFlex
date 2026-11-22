package com.pagoflex.pagos.service;

import com.pagoflex.pagos.client.CompromisoClient;
import com.pagoflex.pagos.dto.CompromisoDto;
import com.pagoflex.pagos.dto.CumplimientoResponse;
import com.pagoflex.pagos.dto.PagoRequest;
import com.pagoflex.pagos.dto.PagoResponse;
import com.pagoflex.pagos.entity.Pago;
import com.pagoflex.pagos.exception.ApiException;
import com.pagoflex.pagos.repository.PagoRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class PagoService {

    private final PagoRepository pagoRepository;
    private final CompromisoClient compromisoClient;
    private final Clock clock;

    public PagoService(PagoRepository pagoRepository, CompromisoClient compromisoClient, Clock clock) {
        this.pagoRepository = pagoRepository;
        this.compromisoClient = compromisoClient;
        this.clock = clock;
    }

    public PagoResponse pagar(Long usuarioId, PagoRequest request) {
        CompromisoDto compromiso = compromisoClient.pagar(request.compromisoId(), usuarioId);
        LocalDateTime ahora = LocalDateTime.now(clock);
        Pago pago = new Pago();
        pago.setComprobante(String.format("CPR-%06d", 600 + pagoRepository.count() + 1));
        pago.setCompromisoId(compromiso.id());
        pago.setFolio(compromiso.folio());
        pago.setUsuarioId(usuarioId);
        pago.setEmpresaId(compromiso.empresaId());
        pago.setEmpresaNombre(compromiso.empresaNombre());
        pago.setConcepto(compromiso.concepto());
        pago.setCuota(compromiso.cuotaNumero() == null ? "-" : compromiso.cuotaNumero() + "/" + compromiso.cuotaTotal());
        pago.setMonto(compromiso.monto());
        pago.setRecargo(compromiso.recargo());
        pago.setMontoPagado(compromiso.total());
        pago.setMedioPago(request.medioPago() == null || request.medioPago().isBlank()
                ? "Pago simulado" : request.medioPago().trim());
        pago.setCanal("App");
        pago.setFechaHora(ahora);
        pago.setATiempo(!ahora.toLocalDate().isAfter(compromiso.vencimiento()));
        try {
            return toResponse(pagoRepository.save(pago));
        } catch (RuntimeException e) {
            compromisoClient.revertirPago(compromiso.id());
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "No pudimos confirmar tu pago. No se realizó ningún cobro, intenta de nuevo.");
        }
    }

    public List<PagoResponse> historial(Long usuarioId, Long empresaId, LocalDate desde, LocalDate hasta) {
        return pagoRepository.findByUsuarioIdOrderByFechaHoraDesc(usuarioId).stream()
                .filter(p -> empresaId == null || empresaId.equals(p.getEmpresaId()))
                .filter(p -> desde == null || !p.getFechaHora().toLocalDate().isBefore(desde))
                .filter(p -> hasta == null || !p.getFechaHora().toLocalDate().isAfter(hasta))
                .map(this::toResponse)
                .toList();
    }

    public PagoResponse obtener(Long id, Long usuarioId) {
        Pago pago = pagoRepository.findById(id)
                .filter(p -> p.getUsuarioId().equals(usuarioId))
                .orElseThrow(() -> ApiException.notFound("No encontramos ese comprobante."));
        return toResponse(pago);
    }

    public CumplimientoResponse cumplimiento(Long usuarioId) {
        LocalDateTime desde = LocalDateTime.now(clock).minusMonths(12);
        List<Pago> ultimos = pagoRepository.findByUsuarioIdOrderByFechaHoraDesc(usuarioId).stream()
                .filter(p -> !p.getFechaHora().isBefore(desde))
                .toList();
        int considerados = ultimos.size();
        int aTiempo = (int) ultimos.stream().filter(Pago::getATiempo).count();
        if (considerados < 3) {
            return new CumplimientoResponse(considerados, aTiempo, null, "Historial insuficiente", false,
                    "Aún no tenemos suficiente historial para mostrar tu indicador. ¡Cada pago suma!");
        }
        double porcentaje = Math.round(aTiempo * 1000.0 / considerados) / 10.0;
        if (porcentaje >= 95) {
            return new CumplimientoResponse(considerados, aTiempo, porcentaje, "Ejemplar", true,
                    "¡Excelente! Tu puntualidad es un ejemplo.");
        }
        if (porcentaje >= 80) {
            return new CumplimientoResponse(considerados, aTiempo, porcentaje, "Al día", true,
                    "¡Vas muy bien! Estás al día con tus compromisos.");
        }
        return new CumplimientoResponse(considerados, aTiempo, porcentaje, "En camino", true,
                "Cada pago a tiempo suma. ¡Sigue adelante!");
    }

    private PagoResponse toResponse(Pago p) {
        return new PagoResponse(p.getId(), p.getComprobante(), p.getCompromisoId(), p.getFolio(), p.getEmpresaId(),
                p.getEmpresaNombre(), p.getConcepto(), p.getCuota(), p.getMonto(), p.getRecargo(), p.getMontoPagado(),
                p.getMedioPago(), p.getCanal(), p.getFechaHora(), p.getATiempo());
    }
}
