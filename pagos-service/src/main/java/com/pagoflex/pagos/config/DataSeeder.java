package com.pagoflex.pagos.config;

import com.pagoflex.pagos.entity.Pago;
import com.pagoflex.pagos.repository.PagoRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataSeeder {

    private static final LocalDate REFERENCIA = LocalDate.of(2026, 10, 6);

    @Bean
    public CommandLineRunner seedPagos(PagoRepository pagoRepository, Clock clock) {
        return args -> {
            if (pagoRepository.count() > 0) {
                return;
            }
            LocalDate hoy = LocalDate.now(clock);
            long desplazamiento = ChronoUnit.DAYS.between(REFERENCIA, hoy);
            Object[][] filas = {
                {"CPR-000377", 72L, "CP-2026-0072", 2L, 1L, "Altos del Maipo", "Cuota pie departamento", "13/36", 310000, 8500, LocalDate.of(2026, 9, 2), LocalTime.of(21, 14), "Agente conversacional", false},
                {"CPR-000398", 89L, "CP-2026-0089", 1L, 8L, "Plaza Oriente", "Arriendo departamento septiembre", "9/12", 520000, 0, LocalDate.of(2026, 9, 9), LocalTime.of(13, 5), "Portal de pagos", true},
                {"CPR-000402", 90L, "CP-2026-0090", 1L, 6L, "Club Los Halcones", "Cuota social septiembre", "9/12", 15000, 2000, LocalDate.of(2026, 9, 7), LocalTime.of(18, 40), "Portal de pagos", false},
                {"CPR-000405", 91L, "CP-2026-0091", 1L, 5L, "Manos Abiertas", "Aporte mensual septiembre", "-", 10000, 0, LocalDate.of(2026, 9, 15), LocalTime.of(8, 22), "Agente conversacional", true},
                {"CPR-000451", 4L, "CP-2026-0104", 1L, 3L, "Crédito Andino", "Crédito de consumo", "7/24", 86400, 0, LocalDate.of(2026, 9, 24), LocalTime.of(10, 42), "Portal de pagos", true},
                {"CPR-000452", 8L, "CP-2026-0108", 3L, 4L, "Horizonte Sur", "Seguro de hogar octubre", "10/12", 18900, 0, LocalDate.of(2026, 9, 30), LocalTime.of(19, 5), "Agente conversacional", true},
                {"CPR-000453", 12L, "CP-2026-0112", 7L, 6L, "Club Los Halcones", "Cuota social octubre", "10/12", 15000, 0, LocalDate.of(2026, 10, 4), LocalTime.of(8, 15), "App", true},
                {"CPR-000447", 98L, "CP-2026-0098", 5L, 2L, "Colegio Bosque Nativo", "Mensualidad septiembre", "8/10", 185000, 0, LocalDate.of(2026, 9, 10), LocalTime.of(22, 31), "Portal de pagos", true}
            };
            for (Object[] f : filas) {
                Pago p = new Pago();
                p.setComprobante((String) f[0]);
                p.setCompromisoId((Long) f[1]);
                p.setFolio((String) f[2]);
                p.setUsuarioId((Long) f[3]);
                p.setEmpresaId((Long) f[4]);
                p.setEmpresaNombre((String) f[5]);
                p.setConcepto((String) f[6]);
                p.setCuota((String) f[7]);
                p.setMonto((Integer) f[8]);
                p.setRecargo((Integer) f[9]);
                p.setMontoPagado((Integer) f[8] + (Integer) f[9]);
                p.setMedioPago("Pago simulado");
                p.setCanal((String) f[12]);
                p.setFechaHora(LocalDateTime.of(((LocalDate) f[10]).plusDays(desplazamiento), (LocalTime) f[11]));
                p.setATiempo((Boolean) f[13]);
                pagoRepository.save(p);
            }
            for (int k = 0; k < 32; k++) {
                boolean tarde = k == 7 || k == 19;
                Pago p = new Pago();
                p.setComprobante(String.format("CPR-%06d", 100 + k));
                p.setCompromisoId(1000L + k);
                p.setFolio(String.format("CP-2025-%04d", 1 + k));
                p.setUsuarioId(1L);
                p.setEmpresaId(6L);
                p.setEmpresaNombre("Club Los Halcones");
                p.setConcepto("Cuota social");
                p.setCuota("-");
                p.setMonto(15000);
                p.setRecargo(tarde ? 2000 : 0);
                p.setMontoPagado(tarde ? 17000 : 15000);
                p.setMedioPago("Pago simulado");
                p.setCanal("Portal de pagos");
                p.setFechaHora(hoy.minusDays(40 + k * 10L).atTime(12, 0));
                p.setATiempo(!tarde);
                pagoRepository.save(p);
            }
        };
    }
}
