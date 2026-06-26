package com.spentra.backend.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.spentra.backend.security.JwtSecurityFilter;

import jakarta.servlet.http.HttpServletResponse;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtSecurityFilter jwtAuthFilter; // Your middleware

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 1. Disable CSRF (Node equivalent: not needed for stateless APIs)
                .csrf(csrf -> csrf.disable())

                .cors(cors -> cors.configure(http))

                // 2. Configure Route Access (Node equivalent: router.get('/public', ...))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll() // Open to everyone
                        .anyRequest().authenticated() // Everything else needs a token
                )

                // 3. Make it Stateless (Node equivalent: no express-session)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, authException) -> {
                            // Set headers
                            response.setContentType("application/json");
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

                            // Manually construct the JSON string to match your ApiExceptionResponse DTO
                            // We use ZonedDateTime.now() to match your Global Handler's timestamp
                            String jsonResponse = String.format(
                                    "{\"message\": \"Unauthorized: Token is missing or invalid\", " +
                                            "\"statusCode\": %d, " +
                                            "\"timestamp\": \"%s\"}",
                                    HttpServletResponse.SC_UNAUTHORIZED,
                                    java.time.ZonedDateTime.now());

                            response.getWriter().write(jsonResponse);
                        }))

                // 4. Add your JWT Middleware (Node equivalent: app.use(myJwtMiddleware))
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}