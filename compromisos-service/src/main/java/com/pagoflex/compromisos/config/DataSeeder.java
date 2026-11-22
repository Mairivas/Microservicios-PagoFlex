package com.pagoflex.compromisos.config;

import com.pagoflex.compromisos.entity.Compromiso;
import com.pagoflex.compromisos.entity.EmpresaCliente;
import com.pagoflex.compromisos.repository.CompromisoRepository;
import com.pagoflex.compromisos.repository.EmpresaClienteRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataSeeder {

    private static final LocalDate REFERENCIA = LocalDate.of(2026, 10, 6);

    @Bean
    public CommandLineRunner seedCompromisos(
            EmpresaClienteRepository empresaRepository,
            CompromisoRepository compromisoRepository,
            Clock clock) {
        return args -> {
            if (empresaRepository.count() > 0) {
                return;
            }
            String[] nombres = {"Altos del Maipo", "Colegio Bosque Nativo", "Crédito Andino", "Horizonte Sur",
                "Manos Abiertas", "Club Los Halcones", "Instituto Cordillera", "Plaza Oriente"};
            int[] recargos = {8500, 5000, 12000, 3000, 0, 2000, 6000, 7500};
            for (int i = 0; i < nombres.length; i++) {
                EmpresaCliente e = new EmpresaCliente();
                e.setNombre(nombres[i]);
                e.setRecargo(recargos[i]);
                empresaRepository.save(e);
            }
            long desplazamiento = ChronoUnit.DAYS.between(REFERENCIA, LocalDate.now(clock));
            LocalDateTime ahora = LocalDateTime.now(clock);
            Object[][] filas = {
                {"CP-2026-0101", 1L, 8L, "Arriendo departamento octubre", 10, 12, 520000, LocalDate.of(2026, 10, 10), "PENDIENTE", 20L},
                {"CP-2026-0102", 1L, 6L, "Cuota social octubre", 10, 12, 15000, LocalDate.of(2026, 10, 5), "PENDIENTE", 18L},
                {"CP-2026-0103", 1L, 5L, "Aporte mensual octubre", null, null, 10000, LocalDate.of(2026, 10, 15), "PENDIENTE", null},
                {"CP-2026-0104", 1L, 3L, "Crédito de consumo", 7, 24, 86400, LocalDate.of(2026, 9, 25), "PAGADO", 16L},
                {"CP-2026-0105", 1L, 3L, "Crédito de consumo", 8, 24, 86400, LocalDate.of(2026, 10, 25), "PENDIENTE", 16L},
                {"CP-2026-0106", 2L, 1L, "Cuota pie departamento", 14, 36, 310000, LocalDate.of(2026, 9, 30), "PENDIENTE", 14L},
                {"CP-2026-0107", 2L, 1L, "Cuota pie departamento", 15, 36, 310000, LocalDate.of(2026, 10, 30), "PENDIENTE", 14L},
                {"CP-2026-0108", 3L, 4L, "Seguro de hogar octubre", 10, 12, 18900, LocalDate.of(2026, 10, 1), "PAGADO", null},
                {"CP-2026-0109", 4L, 7L, "Arancel octubre", 8, 10, 142000, LocalDate.of(2026, 10, 5), "EN_REVISION", 19L},
                {"CP-2026-0110", 5L, 2L, "Mensualidad octubre", 8, 10, 185000, LocalDate.of(2026, 10, 10), "PENDIENTE", 15L},
                {"CP-2026-0111", 6L, 3L, "Crédito de consumo", 19, 36, 54200, LocalDate.of(2026, 9, 20), "ANULADO", 17L},
                {"CP-2026-0112", 7L, 6L, "Cuota social octubre", 10, 12, 15000, LocalDate.of(2026, 10, 5), "PAGADO", 18L}
            };
            for (Object[] f : filas) {
                Compromiso c = new Compromiso();
                c.setFolio((String) f[0]);
                c.setUsuarioId((Long) f[1]);
                c.setEmpresaId((Long) f[2]);
                c.setConcepto((String) f[3]);
                c.setCuotaNumero((Integer) f[4]);
                c.setCuotaTotal((Integer) f[5]);
                c.setMonto((Integer) f[6]);
                c.setVencimiento(((LocalDate) f[7]).plusDays(desplazamiento));
                c.setEstado((String) f[8]);
                c.setRegistradoPorId((Long) f[9]);
                if ("PAGADO".equals(c.getEstado())) {
                    c.setRecargoAplicado(0);
                    c.setFechaPago(ahora.minusDays(20));
                }
                if ("ANULADO".equals(c.getEstado())) {
                    c.setMotivoAnulacion("Repactación");
                    c.setAnuladoPorId(17L);
                    c.setFechaAnulacion(ahora.minusDays(30));
                }
                compromisoRepository.save(c);
            }
        };
    }
}
