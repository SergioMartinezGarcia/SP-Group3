package com.unipd.dei.sp.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/*
 * Web configuration for cross-origin requests.
 * Allows the frontend to communicate with the backend API.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    // Enables cross-origin requests from any domain to the API endpoints
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .maxAge(3600);
    }
}