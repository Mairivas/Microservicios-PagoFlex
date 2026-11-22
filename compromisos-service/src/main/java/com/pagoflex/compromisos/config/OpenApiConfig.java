package com.pagoflex.compromisos.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("PagoFlex - Compromisos Service")
                        .version("1.0.0")
                        .description("Compromisos de pago, recargos, anulación y resumen de cobranza"));
    }
}
