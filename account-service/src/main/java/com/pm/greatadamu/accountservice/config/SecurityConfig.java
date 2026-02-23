package com.pm.greatadamu.accountservice.config;

import com.pm.greatadamu.accountservice.filter.TrustedHeaderAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {
    private final TrustedHeaderAuthFilter trustedHeaderAuthFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Allow actuator health checks (if you have them)
                        .requestMatchers("/actuator/health/**").permitAll()

                        // All /api/accounts/** endpoints require authentication
                        .requestMatchers("/api/accounts/**").authenticated()

                        // Everything else requires authentication
                        .anyRequest().authenticated()
                )
                // Add our custom filter to read headers
                .addFilterBefore(trustedHeaderAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}