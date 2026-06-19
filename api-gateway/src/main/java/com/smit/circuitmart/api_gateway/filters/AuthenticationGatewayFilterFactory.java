package com.smit.circuitmart.api_gateway.filters;

import com.smit.circuitmart.api_gateway.service.JwtService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class AuthenticationGatewayFilterFactory
        extends AbstractGatewayFilterFactory<AuthenticationGatewayFilterFactory.Config> {

    private final JwtService jwtService;

    public AuthenticationGatewayFilterFactory(JwtService jwtService) {
        super(Config.class);
        this.jwtService = jwtService;
    }

    @Override
    public GatewayFilter apply(Config config) {

        if (!config.isEnabled()) {
            return (exchange, chain) -> chain.filter(exchange);
        }

        return (exchange, chain) -> {

            String authorizationHeader =
                    exchange.getRequest().getHeaders().getFirst("Authorization");

            if (authorizationHeader == null ||
                    !authorizationHeader.startsWith("Bearer ")) {

                log.warn("Missing or Invalid Authorization Header");

                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }

            try {

                String token = authorizationHeader.substring(7);

                String subject = jwtService.getSubjectFromToken(token);

                log.info("JWT Subject: {}", subject);

                var mutatedRequest = exchange.getRequest()
                        .mutate()
                        .header("X-User-Id", subject)
                        .build();

                var mutatedExchange = exchange.mutate()
                        .request(mutatedRequest)
                        .build();

                return chain.filter(mutatedExchange);

            } catch (Exception ex) {

                log.error("JWT Validation Failed", ex);

                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }
        };
    }

    @Data
    public static class Config {
        private boolean isEnabled = true;
    }
}