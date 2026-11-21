package com.fsc.supply_chain_common_operations.securities;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.security.config.Customizer;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity
public class SecurityConfiguration {

    private final JwtFilter jwtFilter;

    @Autowired
    public SecurityConfiguration(JwtFilter jwtFilter) {
        this.jwtFilter = jwtFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http) throws Exception {

        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())


                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/v1/common/emp/login",
                                "/api/v1/common/emp/activate",
                                "/api/v1/slo/company/register",
                                "/api/procurement-companies/register",
                                "/error"
                        ).permitAll()
                        .anyRequest().authenticated()
                )

                .exceptionHandling(exception -> exception

                        // No token or unauthenticated request
                        .authenticationEntryPoint((request, response, ex) -> {
                            response.setStatus(
                                    HttpStatus.UNAUTHORIZED.value()
                            );
                            response.setContentType("application/json");
                            response.getWriter().write(
                                    "{\"status\":401,\"error\":\"Unauthorized\",\"message\":\"Authentication is required.\"}"
                            );
                        })

                        // Valid authentication but insufficient permission
                        .accessDeniedHandler((request, response, ex) -> {
                            response.setStatus(
                                    HttpStatus.FORBIDDEN.value()
                            );
                            response.setContentType("application/json");
                            response.getWriter().write(
                                    "{\"status\":403,\"error\":\"Forbidden\",\"message\":\"You do not have permission to access this resource.\"}"
                            );
                        })
                )

                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                .addFilterBefore(
                        jwtFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOrigins(
                List.of("https://procure-axis.vercel.app",
                        "https://localhost:5173")
        );

        configuration.setAllowedMethods(
                List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
        );

        configuration.setAllowedHeaders(
                List.of("Content-Type", "token", "Accept")
        );

        configuration.setAllowCredentials(false);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}
