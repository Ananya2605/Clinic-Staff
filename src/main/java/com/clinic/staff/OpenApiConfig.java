package com.clinic.staff;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Adds the Authorize button (HTTP Basic) to Swagger UI. */
@Configuration
class OpenApiConfig {
    @Bean
    OpenAPI openApi() {
        return new OpenAPI()
            .components(new Components().addSecuritySchemes("basic",
                new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("basic")))
            .addSecurityItem(new SecurityRequirement().addList("basic"));
    }
}
