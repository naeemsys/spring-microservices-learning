package com.example.gateway.filter;

import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
public class JwtHeaderFilter implements GlobalFilter {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        return ReactiveSecurityContextHolder.getContext()
                .map(ctx -> ctx.getAuthentication())
                .cast(Authentication.class)
                .flatMap(auth -> {

                    if (auth.getPrincipal() instanceof Jwt jwt) {
                        String sub = jwt.getSubject();
                        List<String> roles =
                                jwt.getClaimAsStringList("roles") != null ?
                                        jwt.getClaimAsStringList("roles") :
                                        List.of();

                        var mutatedRequest = exchange.getRequest()
                                .mutate()
                                .header("X-User-Sub", sub)
                                .header("X-User-Roles", String.join(",", roles))
                                .build();

                        return chain.filter(exchange.mutate().request(mutatedRequest).build());
                    }
                    return chain.filter(exchange);
                })
                .switchIfEmpty(chain.filter(exchange));
    }
}
