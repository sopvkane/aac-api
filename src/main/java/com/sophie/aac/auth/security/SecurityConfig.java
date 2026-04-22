package com.sophie.aac.auth.security;

import com.sophie.aac.auth.repository.AuthSessionRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http, AuthSessionRepository sessions) throws Exception {
    http
        .csrf(csrf -> csrf.disable())
        .cors(Customizer.withDefaults())
        .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .addFilterBefore(new CookieSessionAuthFilter(sessions), UsernamePasswordAuthenticationFilter.class)
        .authorizeHttpRequests(auth -> auth
            // public endpoints
            .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
            .requestMatchers(HttpMethod.POST, "/api/auth/register").permitAll()
            .requestMatchers(HttpMethod.POST, "/api/auth/logout").permitAll()
            .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
            .requestMatchers(HttpMethod.POST, "/api/auth/select-profile")
            .hasAnyRole("PARENT", "CARER", "CLINICIAN", "SCHOOL_ADMIN", "SCHOOL_TEACHER")
            .requestMatchers("/actuator/health", "/actuator/info").permitAll()
            .requestMatchers(HttpMethod.GET, "/api/health/**").permitAll()
            // caregiver profile is restricted to caregiver-style roles
            .requestMatchers("/api/auth/me").hasAnyRole("PARENT", "CARER", "CLINICIAN", "SCHOOL_ADMIN", "SCHOOL_TEACHER")
            .requestMatchers("/api/carer/**").hasAnyRole("PARENT", "CARER", "CLINICIAN", "SCHOOL_ADMIN", "SCHOOL_TEACHER")
            .requestMatchers("/api/phrases/**").hasAnyRole("PARENT", "CARER", "CLINICIAN", "SCHOOL_ADMIN", "SCHOOL_TEACHER")
            // secure-by-default for API surface
            .requestMatchers("/api/**").hasAnyRole("PARENT", "CARER", "CLINICIAN", "SCHOOL_ADMIN", "SCHOOL_TEACHER")
            // non-API endpoints can stay open (e.g. health probes/docs assets)
            .anyRequest().permitAll()
        )
        .exceptionHandling(ex -> ex
            // For this app we treat missing/invalid auth on protected endpoints as 403
            .authenticationEntryPoint((request, response, authException) ->
                response.sendError(HttpStatus.FORBIDDEN.value()))
        );

    return http.build();
  }
}
