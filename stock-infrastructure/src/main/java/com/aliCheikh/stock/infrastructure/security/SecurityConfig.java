package com.aliCheikh.stock.infrastructure.security;

import com.aliCheikh.stock.application.usecase.GetUserUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity,
                                                   JwtService jwtService,
                                                   GetUserUseCase getUserUseCase) throws Exception {
        JwtAuthenticationFilter jwtAuthenticationFilter = new JwtAuthenticationFilter(jwtService, getUserUseCase);
        TemporaryPasswordFilter temporaryPasswordFilter = new TemporaryPasswordFilter();
        httpSecurity.csrf(csrf -> csrf.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex.authenticationEntryPoint(
                        new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET, "/api/v1/health").permitAll()
                        .requestMatchers(HttpMethod.POST,
                                "/api/v1/auth/login",
                                "/api/v1/auth/refresh",
                                "/api/v1/auth/logout").permitAll()
                        .requestMatchers(HttpMethod.PUT, "/api/v1/products/**").hasRole("OWNER")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/products/**").hasRole("OWNER")
                        .requestMatchers("/api/v1/users", "/api/v1/users/**").hasRole("OWNER")
                        // Category administration and debt views require the owner role. Customer
                        // endpoints remain authenticated.
                        .requestMatchers(HttpMethod.POST, "/api/v1/categories").hasRole("OWNER")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/categories/**").hasRole("OWNER")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/categories/**").hasRole("OWNER")
                        .requestMatchers(HttpMethod.GET, "/api/v1/dashboard/**").hasRole("OWNER")
                        .requestMatchers(HttpMethod.GET, "/api/v1/debts/**").hasRole("OWNER")
                        .requestMatchers(HttpMethod.GET, "/api/v1/customers/*/debts").hasRole("OWNER")
                        .requestMatchers(HttpMethod.GET, "/api/v1/customers/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/v1/**").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/v1/**").authenticated()
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/**").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/**").authenticated()
                        // New routes require authentication unless explicitly made public.
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(temporaryPasswordFilter, JwtAuthenticationFilter.class);
        return httpSecurity.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
