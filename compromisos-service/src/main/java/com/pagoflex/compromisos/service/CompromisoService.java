package com.pagoflex.compromisos.service;

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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CompromisoService {

    private static final List<String> MESES = List.of("enero", "febrero", "marzo", "abril", "mayo", "junio", "julio",
            "agosto", "septiembre", "octubre", "noviembre", "diciembre");

    private static final List<String> MOTIVOS_ANULACION = List.of(
            "Repactación", "Error de registro", "Pagado directamente a la empresa", "Otro");

    private final CompromisoRepository compromisoRepository;
    private final EmpresaClienteRepository empresaRepository;
    private final PersonaClient personaClient;
    private final Clock clock;

    public CompromisoService(
            CompromisoRepository compromisoRepository,
            EmpresaClienteRepository empresaRepository,
            PersonaClient personaClient,
            Clock clock) {
        this.compromisoRepository = compromisoRepository;
        this.empresaRepository = empresaRepository;
        this.personaClient = personaClient;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<CompromisoResponse> listar(Long userId, String rol, Long empresaId, String estado) {
        List<Compromiso> base;
        if (Roles.USUARIO.equals(rol)) {
            base = compromisoRepository.findByUsuarioIdOrderByVencimientoAsc(userId);
        } else if (Roles.EJECUTIVO.equals(rol)) {
            base = compromisoRepository.findByEmpresaIdOrderByVencimientoAsc(requerirEmpresa(empresaId));
        } else {
            throw ApiException.forbidden("Tu perfil no tiene acceso a los compromisos.");
        }
        LocalDate hoy = hoy();
        Map<Long, EmpresaCliente> empresas = empresas();
        return base.stream()
                .map(c -> toResponse(c, empresas, hoy))
                .filter(r -> estado == null || estado.isBlank() || r.estado().equalsIgnoreCase(estado))
                .toList();
    }

    @Transactional(readOnly = true)
    public CompromisoResponse obtener(Long id, Long userId, String rol, Long empresaId) {
        Compromiso c = buscar(id);
        boolean esDelUsuario = Roles.USUARIO.equals(rol) && c.getUsuarioId().equals(userId);
        boolean esDeLaEmpresa = Roles.EJECUTIVO.equals(rol) && c.getEmpresaId().equals(empresaId);
        if (!esDelUsuario && !esDeLaEmpresa) {
            throw ApiException.notFound("No encontramos ese compromiso.");
        }
        return toResponse(c);
    }

    @Transactional(readOnly = true)
    public ResumenResponse resumen(Long userId) {
        LocalDate hoy = hoy();
        LocalDate finMes = YearMonth.from(hoy).atEndOfMonth();
        Map<Long, EmpresaCliente> empresas = empresas();
        List<CompromisoResponse> porPagar = compromisoRepository.findByUsuarioIdOrderByVencimientoAsc(userId).stream()
                .map(c -> toResponse(c, empresas, hoy))
                .filter(r -> Estados.PENDIENTE.equals(r.estado()) || Estados.VENCIDO.equals(r.estado()))
                .toList();
        int total = porPagar.stream()
                .filter(r -> !r.vencimiento().isAfter(finMes))
                .mapToInt(CompromisoResponse::total)
                .sum();
        LocalDate proximo = porPagar.stream()
                .map(CompromisoResponse::vencimiento)
                .filter(v -> !v.isBefore(hoy))
                .min(Comparator.naturalOrder())
                .orElse(null);
        long vencidos = porPagar.stream().filter(r -> Estados.VENCIDO.equals(r.estado())).count();
        return new ResumenResponse(total, proximo, vencidos, LocalDateTime.now(clock));
    }

    @Transactional(readOnly = true)
    public CobranzaResponse cobranza(Long empresaId, YearMonth mes) {
        LocalDate hoy = hoy();
        YearMonth periodo = mes == null ? YearMonth.from(hoy) : mes;
        List<Compromiso> delMes = compromisoRepository.findByEmpresaIdOrderByVencimientoAsc(empresaId).stream()
                .filter(c -> YearMonth.from(c.getVencimiento()).equals(periodo))
                .toList();
        long anulados = delMes.stream().filter(c -> Estados.ANULADO.equals(c.getEstado())).count();
        List<Compromiso> pagados = delMes.stream().filter(c -> Estados.PAGADO.equals(c.getEstado())).toList();
        long vencidos = delMes.stream().filter(c -> Estados.VENCIDO.equals(estadoEfectivo(c, hoy))).count();
        long pendientes = delMes.stream().filter(c -> Estados.PENDIENTE.equals(estadoEfectivo(c, hoy))).count();
        int recaudado = pagados.stream()
                .mapToInt(c -> c.getMonto() + (c.getRecargoAplicado() == null ? 0 : c.getRecargoAplicado()))
                .sum();
        long base = delMes.size() - anulados;
        double tasa = base == 0 ? 0.0 : Math.round(pagados.size() * 1000.0 / base) / 10.0;
        return new CobranzaResponse(periodo.toString(), (long) delMes.size(), anulados, (long) pagados.size(),
                vencidos, pendientes, recaudado, tasa);
    }

    @Transactional
    public CompromisoResponse crear(CrearCompromisoRequest req, Long empresaId, Long ejecutivoId) {
        LocalDate hoy = hoy();
        if ((req.cuotaNumero() == null) != (req.cuotaTotal() == null)) {
            throw ApiException.badRequest("Indica el número de cuota y el total de cuotas, o deja ambos vacíos.");
        }
        if (req.cuotaNumero() != null && req.cuotaNumero() > req.cuotaTotal()) {
            throw ApiException.badRequest("La cuota no puede ser mayor al total de cuotas.");
        }
        if (req.vencimiento().isBefore(hoy)) {
            throw ApiException.badRequest("El vencimiento no puede ser anterior a hoy.");
        }
        PersonaResponse persona = personaClient.buscarPorRut(req.rutUsuario());
        if (compromisoRepository.existsByUsuarioIdAndEmpresaIdAndConceptoAndCuotaNumero(
                persona.id(), empresaId, req.concepto(), req.cuotaNumero())) {
            throw ApiException.conflict("Ya existe un compromiso con ese concepto y cuota para esta persona.");
        }
        Compromiso c = new Compromiso();
        c.setFolio(String.format("CP-%d-%04d", hoy.getYear(), 101 + compromisoRepository.count()));
        c.setUsuarioId(persona.id());
        c.setEmpresaId(empresaId);
        c.setConcepto(req.concepto().trim());
        c.setCuotaNumero(req.cuotaNumero());
        c.setCuotaTotal(req.cuotaTotal());
        c.setMonto(req.monto());
        c.setVencimiento(req.vencimiento());
        c.setEstado(Estados.PENDIENTE);
        c.setRegistradoPorId(ejecutivoId);
        return toResponse(compromisoRepository.save(c));
    }

    @Transactional
    public CompromisoResponse anular(Long id, AnularRequest req, Long empresaId, Long ejecutivoId) {
        Compromiso c = buscar(id);
        if (!c.getEmpresaId().equals(empresaId)) {
            throw ApiException.notFound("No encontramos ese compromiso.");
        }
        if (!MOTIVOS_ANULACION.contains(req.motivo())) {
            throw ApiException.badRequest("Elige un motivo de anulación válido.");
        }
        boolean sinComentario = req.comentario() == null || req.comentario().isBlank();
        if ("Otro".equals(req.motivo()) && sinComentario) {
            throw ApiException.badRequest("Cuéntanos el motivo en el comentario.");
        }
        if (Estados.PAGADO.equals(c.getEstado())) {
            throw ApiException.conflict("Un compromiso pagado no se puede anular.");
        }
        if (Estados.ANULADO.equals(c.getEstado())) {
            throw ApiException.conflict("Este compromiso ya está anulado.");
        }
        c.setEstado(Estados.ANULADO);
        c.setMotivoAnulacion(sinComentario ? req.motivo() : req.motivo() + ": " + req.comentario().trim());
        c.setAnuladoPorId(ejecutivoId);
        c.setFechaAnulacion(LocalDateTime.now(clock));
        return toResponse(compromisoRepository.save(c));
    }

    @Transactional
    public CompromisoResponse pagar(Long id, Long userId) {
        Compromiso c = buscar(id);
        if (!c.getUsuarioId().equals(userId)) {
            throw ApiException.notFound("No encontramos ese compromiso.");
        }
        if (!Estados.PENDIENTE.equals(c.getEstado())) {
            throw ApiException.conflict("Este compromiso no se puede pagar en este momento.");
        }
        if (compromisoRepository.existsByUsuarioIdAndEmpresaIdAndEstadoAndVencimientoBefore(
                userId, c.getEmpresaId(), Estados.PENDIENTE, c.getVencimiento())) {
            throw ApiException.conflict("Antes de pagar este compromiso, paga el de vencimiento más antiguo de la misma empresa.");
        }
        LocalDate hoy = hoy();
        Map<Long, EmpresaCliente> empresas = empresas();
        boolean vencido = Estados.VENCIDO.equals(estadoEfectivo(c, hoy));
        EmpresaCliente empresa = empresas.get(c.getEmpresaId());
        c.setRecargoAplicado(vencido && empresa != null ? empresa.getRecargo() : 0);
        c.setEstado(Estados.PAGADO);
        c.setFechaPago(LocalDateTime.now(clock));
        Compromiso pagado = compromisoRepository.save(c);
        generarSiguienteCuota(pagado);
        return toResponse(pagado);
    }

    private void generarSiguienteCuota(Compromiso pagado) {
        Integer numero = pagado.getCuotaNumero();
        if (numero == null || pagado.getCuotaTotal() == null || numero >= pagado.getCuotaTotal()) {
            return;
        }
        int siguiente = numero + 1;
        if (compromisoRepository.existsByUsuarioIdAndEmpresaIdAndConceptoAndCuotaNumero(
                pagado.getUsuarioId(), pagado.getEmpresaId(), pagado.getConcepto(), siguiente)) {
            return;
        }
        LocalDate vencimiento = pagado.getVencimiento().plusMonths(1);
        Compromiso proxima = new Compromiso();
        proxima.setFolio(String.format("CP-%d-%04d", hoy().getYear(), 101 + compromisoRepository.count()));
        proxima.setUsuarioId(pagado.getUsuarioId());
        proxima.setEmpresaId(pagado.getEmpresaId());
        proxima.setConcepto(conceptoDelMes(pagado.getConcepto(), vencimiento));
        proxima.setCuotaNumero(siguiente);
        proxima.setCuotaTotal(pagado.getCuotaTotal());
        proxima.setMonto(pagado.getMonto());
        proxima.setVencimiento(vencimiento);
        proxima.setEstado(Estados.PENDIENTE);
        compromisoRepository.save(proxima);
    }

    private String conceptoDelMes(String concepto, LocalDate vencimiento) {
        String nuevoMes = vencimiento.getMonth().getDisplayName(java.time.format.TextStyle.FULL, new java.util.Locale("es", "CL"));
        for (String mes : MESES) {
            if (concepto.toLowerCase().endsWith(" " + mes)) {
                return concepto.substring(0, concepto.length() - mes.length()) + nuevoMes;
            }
        }
        return concepto;
    }

    @Transactional
    public CompromisoResponse revertirPago(Long id) {
        Compromiso c = buscar(id);
        if (!Estados.PAGADO.equals(c.getEstado())) {
            throw ApiException.conflict("Este compromiso no está pagado.");
        }
        c.setEstado(Estados.PENDIENTE);
        c.setRecargoAplicado(null);
        c.setFechaPago(null);
        if (c.getCuotaNumero() != null) {
            compromisoRepository.findFirstByUsuarioIdAndEmpresaIdAndCuotaNumeroAndEstadoAndRegistradoPorIdIsNull(
                    c.getUsuarioId(), c.getEmpresaId(), c.getCuotaNumero() + 1, Estados.PENDIENTE)
                    .ifPresent(compromisoRepository::delete);
        }
        return toResponse(compromisoRepository.save(c));
    }

    @Transactional
    public CompromisoResponse marcarRevision(Long id) {
        Compromiso c = buscar(id);
        if (!Estados.PENDIENTE.equals(c.getEstado())) {
            throw ApiException.conflict("Este compromiso no se puede reportar en este momento.");
        }
        c.setEstado(Estados.EN_REVISION);
        return toResponse(compromisoRepository.save(c));
    }

    @Transactional
    public CompromisoResponse restaurar(Long id) {
        Compromiso c = buscar(id);
        if (Estados.EN_REVISION.equals(c.getEstado())) {
            c.setEstado(Estados.PENDIENTE);
            c = compromisoRepository.save(c);
        }
        return toResponse(c);
    }

    @Transactional(readOnly = true)
    public CompromisoResponse obtenerInterno(Long id) {
        return toResponse(buscar(id));
    }

    private Compromiso buscar(Long id) {
        return compromisoRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("No encontramos ese compromiso."));
    }

    private Long requerirEmpresa(Long empresaId) {
        if (empresaId == null) {
            throw ApiException.forbidden("Tu cuenta no está asociada a una empresa.");
        }
        return empresaId;
    }

    private LocalDate hoy() {
        return LocalDate.now(clock);
    }

    private Map<Long, EmpresaCliente> empresas() {
        Map<Long, EmpresaCliente> mapa = new HashMap<>();
        for (EmpresaCliente e : empresaRepository.findAll()) {
            mapa.put(e.getId(), e);
        }
        return mapa;
    }

    private String estadoEfectivo(Compromiso c, LocalDate hoy) {
        if (Estados.PENDIENTE.equals(c.getEstado()) && c.getVencimiento().isBefore(hoy)) {
            return Estados.VENCIDO;
        }
        return c.getEstado();
    }

    private CompromisoResponse toResponse(Compromiso c) {
        return toResponse(c, empresas(), hoy());
    }

    private CompromisoResponse toResponse(Compromiso c, Map<Long, EmpresaCliente> empresas, LocalDate hoy) {
        String estado = estadoEfectivo(c, hoy);
        EmpresaCliente empresa = empresas.get(c.getEmpresaId());
        int recargo = 0;
        int dias = 0;
        if (Estados.PAGADO.equals(estado)) {
            recargo = c.getRecargoAplicado() == null ? 0 : c.getRecargoAplicado();
        } else if (Estados.VENCIDO.equals(estado)) {
            recargo = empresa == null ? 0 : empresa.getRecargo();
            dias = (int) ChronoUnit.DAYS.between(c.getVencimiento(), hoy);
        }
        return new CompromisoResponse(
                c.getId(), c.getFolio(), c.getUsuarioId(), c.getEmpresaId(),
                empresa == null ? "Empresa" : empresa.getNombre(),
                c.getConcepto(), c.getCuotaNumero(), c.getCuotaTotal(),
                c.getMonto(), recargo, c.getMonto() + recargo,
                c.getVencimiento(), estado, dias, c.getFechaPago(), c.getMotivoAnulacion());
    }
}
