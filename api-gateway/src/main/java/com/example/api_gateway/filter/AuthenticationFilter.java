package com.example.api_gateway.filter;

import com.example.api_gateway.util.JwtUtil;
import io.jsonwebtoken.Claims;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class AuthenticationFilter extends AbstractGatewayFilterFactory<AuthenticationFilter.Config> {

    private final RouteValidator validator;
    private final JwtUtil jwtUtil;

    public AuthenticationFilter(RouteValidator validator, JwtUtil jwtUtil) {
        super(Config.class);
        this.validator = validator;
        this.jwtUtil = jwtUtil;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return ((exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();

            // 1. Verificăm dacă ruta necesită securitate
            if (validator.isSecured.test(request)) {

                // 2. Verificăm existența header-ului de autorizare
                if (!request.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
                    throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing authorization header");
                }

                String authHeader = request.getHeaders().get(HttpHeaders.AUTHORIZATION).get(0);
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    authHeader = authHeader.substring(7); // Scoatem "Bearer "
                }

                try {
                    // 3. Validăm token-ul (Verifică semnătura și expirarea)
                    jwtUtil.validateToken(authHeader);

                    // 4. Extragem user info și îl trimitem mai departe (OPȚIONAL dar RECOMANDAT)
                    Claims claims = jwtUtil.getClaims(authHeader);

                    // Putem adăuga informațiile în header pentru ca microserviciile din spate să știe cine e userul
                    // Fără să mai valideze ele token-ul!
                    request = exchange.getRequest().mutate()
                            .header("userId", claims.get("userId", String.class))
                            .header("role", claims.get("role", String.class))
                            .build();

                } catch (Exception e) {
                    System.out.println("Invalid access...!");
                    throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized access to application");
                }
            }
            return chain.filter(exchange.mutate().request(request).build());
        });
    }

    public static class Config {
        // Putem pune configurări aici dacă e nevoie
    }
}