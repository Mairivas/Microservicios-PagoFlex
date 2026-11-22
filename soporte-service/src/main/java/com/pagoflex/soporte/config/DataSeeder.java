package com.pagoflex.soporte.config;

import com.pagoflex.soporte.entity.Reporte;
import com.pagoflex.soporte.repository.ReporteRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataSeeder {

    private static final LocalDate REFERENCIA = LocalDate.of(2026, 10, 6);

    @Bean
    public CommandLineRunner seedReportes(ReporteRepository reporteRepository, Clock clock) {
        return args -> {
            if (reporteRepository.count() > 0) {
                return;
            }
            long desplazamiento = ChronoUnit.DAYS.between(REFERENCIA, LocalDate.now(clock));
            Object[][] filas = {
                {"REP-0005", 64L, "CP-2026-0064", 1L, "Otro", "El concepto dice agosto y es la cuota de julio.", LocalDate.of(2026, 7, 28), "RESUELTO", 9L, "Se corrigió el concepto; el monto era correcto."},
                {"REP-0007", 81L, "CP-2026-0081", 5L, "No reconozco este cobro", "Me aparece una matrícula que ya pagué en marzo.", LocalDate.of(2026, 9, 2), "RESUELTO", 10L, "Cobro duplicado anulado por el colegio."},
                {"REP-0009", 11L, "CP-2026-0111", 6L, "El monto no corresponde", "Repacté el crédito y me siguen cobrando la cuota antigua.", LocalDate.of(2026, 9, 18), "RESUELTO", 9L, "La empresa anuló la cuota por repactación."},
                {"REP-0010", 95L, "CP-2026-0095", 8L, "No reconozco este cobro", "Me llegó un cobro de un club al que no pertenezco.", LocalDate.of(2026, 9, 30), "ABIERTO", null, null},
                {"REP-0011", 9L, "CP-2026-0109", 4L, "Ya pagué por otro medio", "Pagué por transferencia directa al instituto.", LocalDate.of(2026, 10, 3), "EN_GESTION", 10L, null}
            };
            for (Object[] f : filas) {
                Reporte r = new Reporte();
                r.setNumero((String) f[0]);
                r.setCompromisoId((Long) f[1]);
                r.setFolio((String) f[2]);
                r.setUsuarioId((Long) f[3]);
                r.setMotivo((String) f[4]);
                r.setComentario((String) f[5]);
                r.setFecha(((LocalDate) f[6]).plusDays(desplazamiento).atTime(LocalTime.of(10, 0)));
                r.setEstado((String) f[7]);
                r.setAgenteId((Long) f[8]);
                r.setRespuesta((String) f[9]);
                if ("RESUELTO".equals(r.getEstado())) {
                    r.setFechaResolucion(r.getFecha().plusDays(3));
                }
                reporteRepository.save(r);
            }
        };
    }
}
