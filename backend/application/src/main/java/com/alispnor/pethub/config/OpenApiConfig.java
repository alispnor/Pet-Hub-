package com.alispnor.pethub.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    private static final String BEARER_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI petHubOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Pet Hub API")
                        .description("API do e-commerce Pet Hub — pet tech com IA. Autentique-se em POST /api/v1/auth/login " +
                                "e clique em Authorize colando o accessToken para testar endpoints protegidos.")
                        .version("v1")
                        .contact(new Contact().name("Ali Salahi").email("ali.salahi@guep.com.br"))
                        .license(new License().name("MIT")))
                .servers(List.of(new Server().url("http://localhost:8080").description("Dev local")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME))
                .components(new Components().addSecuritySchemes(BEARER_SCHEME,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Cole apenas o accessToken (sem o prefixo Bearer).")));
    }
}
