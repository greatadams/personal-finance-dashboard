package com.pm.greatadamu.apigateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpHeaders;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    private static final String X_FORWARDED_FOR = "X-Forwarded-For";

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(
            ServerHttpSecurity http,
            ReactiveJwtAuthenticationConverter jwtAuthConverter
    ) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(ex -> ex

                        // CORS preflight (browser requirement)
                        .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // permit ONLY health checks publicly
                        .pathMatchers("/actuator/health/**").permitAll()

                        // fallback routes must be reachable when downstream is down
                        .pathMatchers("/fallback/**").permitAll()

                        //auth endpoints must be public (login/register/jwks if routed here)
                        .pathMatchers("/api/auth/**").permitAll()

                        //// protected business endpoints FOR ACCOUNT
                        .pathMatchers("/api/accounts/**").hasAnyRole("USER", "ADMIN","CUSTOMER")

                        /// PROTECTED BUSINESS ENDPOINT FOR CUSTOMER
                        .pathMatchers("/api/customers/**").hasAnyRole("ADMIN","CUSTOMER")

                        /// PROTECTED BUSINESS ENDPOINT FOR TRANSACTION
                        .pathMatchers("/api/transactions/**").hasAnyRole("USER", "ADMIN", "CUSTOMER")

                        //PROTECTED BUSINESS ENDPOINT FOR ANALYTICS
                        .pathMatchers("/api/analytics/**").hasAnyRole("USER", "ADMIN", "CUSTOMER")

                        // default policy: valid JWT required
                        .anyExchange().authenticated()
                )
                .oauth2ResourceServer(oauth -> oauth
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthConverter))
                )
                .build();
    }

    @Bean
    public ReactiveJwtAuthenticationConverter jwtAuthConverter() {
        var converter = new ReactiveJwtAuthenticationConverter();

        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            List<String> roles = jwt.getClaimAsStringList("roles");
            if (roles == null) roles = List.of();

            return Flux.fromIterable(
                    roles.stream()
                            .filter(r -> r != null && !r.isBlank())
                            .map(r -> r.toUpperCase(Locale.ROOT))
                            .map(r -> r.startsWith("ROLE_") ? r.substring(5) : r)
                            .map(r -> "ROLE_" + r)
                            .map(SimpleGrantedAuthority::new)
                            .collect(Collectors.toList())
            );
        });

        return converter;
    }

    //IP based key prefer X-FORWARDED-FOR
    @Bean
    @Primary
    public KeyResolver ipKeyResolver() {
        return exchange -> {
            String ip = resolveClientIp(
                    exchange.getRequest().getHeaders(),
                    exchange.getRequest().getRemoteAddress()

            );
            return   Mono.just("ip:" + ip);
        };

    }

    //USER-BASED when logged in, else fallback to IP resolver
    @Bean
    public KeyResolver userOrIpKeyResolver(KeyResolver ipKeyResolver) {
        return exchange -> exchange.getPrincipal()
                .map(p -> "user:" +  p.getName())
                .switchIfEmpty(ipKeyResolver.resolve(exchange));

    }


    private String resolveClientIp(HttpHeaders headers, InetSocketAddress remoteAddress) {
        // Prefer X-Forwarded-For: "clientIp, proxy1, proxy2"
        String xff = headers.getFirst(X_FORWARDED_FOR);
        if (xff != null && !xff.isBlank()) {
            String first = xff.split(",")[0].trim();
            if (!first.isBlank()) return first;
        }

        // Fallback to remote address (works in local dev)
        if (remoteAddress != null && remoteAddress.getAddress() != null) {
            return remoteAddress.getAddress().getHostAddress();
        }

        return "unknown";

    }
}