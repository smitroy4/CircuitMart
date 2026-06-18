package com.smit.circuitmart.api_gateway;

import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class ApiGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }

    @Bean
    ApplicationRunner printRoutes(RouteLocator routeLocator) {
        return args -> routeLocator.getRoutes()
                .subscribe(route ->
                        System.out.println("ROUTE FOUND => " + route.getId()));
    }
}