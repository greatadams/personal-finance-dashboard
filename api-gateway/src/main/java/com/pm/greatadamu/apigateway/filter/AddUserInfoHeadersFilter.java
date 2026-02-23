package com.pm.greatadamu.apigateway.filter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
@Slf4j
public class AddUserInfoHeadersFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().toString();

        if (path.startsWith("/api/auth") || path.startsWith("/actuator") || path.startsWith("/fallback")) {
            return chain.filter(exchange);
        }
        log.info("🔍 Filter executing for path: {}", path);

        return exchange.getPrincipal()
                .cast(Authentication.class)
                .flatMap(authentication -> {
                    log.info("✅ Authentication found: {}", authentication.getName());

                    Object principal = authentication.getPrincipal();
                    log.info("📦 Principal type: {}", principal.getClass().getSimpleName());

                    if (!(principal instanceof Jwt jwt)) {
                        log.warn("⚠️ Principal is NOT a JWT: {}", principal.getClass().getName());
                        return chain.filter(exchange);
                    }

                    // At this point, 'jwt' variable exists and is guaranteed to be a Jwt
                    String userId = jwt.getClaimAsString("sub");
                    String email = jwt.getClaimAsString("email");
                    Object customerIdObj = jwt.getClaim("customerId");
                    String customerId = customerIdObj != null ? customerIdObj.toString() : null;

                    List<String> roles = jwt.getClaimAsStringList("roles");
                    String rolesHeader = (roles == null || roles.isEmpty()) ? null : String.join(",", roles);

                    log.info("🎯 Adding headers - userId: {}, customerId: {}, email: {}, roles: {}",
                            userId, customerId, email, rolesHeader);  // Fixed: 'role' → 'rolesHeader'

                    ServerWebExchange mutatedExchange = exchange.mutate()
                            .request(r -> r
                                    .header("X-User-Id", userId)
                                    .header("X-Customer-Id", customerId)
                                    .header("X-User-Email", email)
                                    .header("X-User-Role", rolesHeader)
                            )
                            .build();

                    return chain.filter(mutatedExchange);
                })
                .switchIfEmpty(Mono.defer(() -> {
                    log.warn("⚠️ No principal found - request is unauthenticated");
                    return chain.filter(exchange);
                }));
    }


    @Override
    public int getOrder() {
        return -1;
    }
}