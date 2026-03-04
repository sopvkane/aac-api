package com.sophie.aac.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

  @Bean
  public OpenAPI aacOpenAPI() {
    return new OpenAPI()
        .info(new Info()
            .title("AAC API")
            .description("AAC (Augmentative and Alternative Communication) API. " +
                "Multi-tenant by communicator/profile: caregivers sign in and switch between linked profiles.")
            .version("0.0.1")
            .contact(new Contact().name("AAC")));
  }
}
