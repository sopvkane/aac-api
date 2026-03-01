package com.sophie.aac.auth.security;

import com.sophie.aac.auth.repository.AuthSessionRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http, AuthSessionRepository sessions) throws Exception {
    http
        .csrf(csrf -> csrf.disable()) // for a uni project + API-only; discuss in AT4 as a tradeoff
        .cors(Customizer.withDefaults())
        .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .addFilterBefore(new CookieSessionAuthFilter(sessions), org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class)
        .authorizeHttpRequests(auth -> auth
            .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

            // public / user-facing endpoints
            .requestMatchers("/api/auth/**").permitAll()
            .requestMatchers("/api/tts/**").permitAll()
            .requestMatchers("/api/speech/**").permitAll()
            .requestMatchers("/api/dialogue/**").permitAll()
            .requestMatchers("/api/phrases/**").permitAll()
            .requestMatchers("/api/events/**").permitAll()
            .requestMatchers("/api/wellbeing/**").permitAll()
            .requestMatchers("/actuator/**").permitAll()

            // caregiver + clinician protected
            .requestMatchers("/api/carer/**").hasAnyRole("PARENT", "CARER")
            .requestMatchers("/api/clinician/**").hasAnyRole("PARENT", "CLINICIAN")

            // anything else: deny by default
            .anyRequest().denyAll()
        );

    return http.build();
  }
}