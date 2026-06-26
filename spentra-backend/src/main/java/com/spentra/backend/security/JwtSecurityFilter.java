package com.spentra.backend.security;

import jakarta.annotation.Nullable;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtSecurityFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(
            @Nullable HttpServletRequest request,
            @Nullable HttpServletResponse response,
            @Nullable FilterChain filterChain) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String userId;

        // 1. Check if the header exists and starts with "Bearer "
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response); // Like calling next() in Express
            return;
        }

        // 2. Extract the token (substring 7 removes "Bearer ")
        jwt = authHeader.substring(7);

        try {
            userId = jwtService.extractUserId(jwt);

            // 3. If we have a userId and the user isn't "logged in" yet for this request
            if (userId != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                // 4. Validate the token against the userId
                if (jwtService.isTokenValid(jwt, userId)) {

                    // This is the "Magic" part: We create an Authentication object
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userId,
                            null,
                            null // We can pass user authorities/roles here later
                    );

                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    // 5. Update the Security Context (This "logs in" the user for this specific
                    // request)
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (Exception e) {
            // If token is expired or tampered, we just let it fall through
            // and Spring Security will return a 403 Forbidden automatically
        }

        filterChain.doFilter(request, response);
    }
}