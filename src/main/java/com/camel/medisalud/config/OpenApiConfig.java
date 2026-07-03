package com.camel.medisalud.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI medisaludOpenAPI(@Value("${spring.application.name}") String appName) {
        return new OpenAPI()
                .info(new Info()
                        .title("Medisalud API")
                        .description("API for the medical appointment scheduling system (" + appName + ").")
                        .version("v1")
                        .contact(new Contact()
                                .name("Camel - General Software")
                                .email("canegrete@generalsoftware.com.co"))
                        .license(new License()
                                .name("Proprietary")));
    }
}
