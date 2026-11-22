package com.pagoflex.pagos.config;

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
                        .title("PagoFlex - Pagos Service")
                        .version("1.0.0")
                        .description("Pagos simulados, comprobantes, historial e indicador de cumplimiento"));
    }
}
