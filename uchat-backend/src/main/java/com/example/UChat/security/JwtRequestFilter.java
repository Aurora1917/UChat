package com.example.UChat.security;


import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtRequestFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private MyUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        try {
            String authorizationHeader = request.getHeader("Authorization");

            String token = null;
            Long userId = null;

            // Try to get token from Authorization header
            if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
                token = authorizationHeader.substring(7);
            } else {
                // If not in header, try cookies
                token = extractJwtFromCookies(request);
            }

            // Only try to extract user ID if token exists
            if (token != null && !token.trim().isEmpty()) {
                userId = jwtUtil.extractUserId(token);

                // Only authenticate if we got a valid user ID
                if (userId != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    UserDetails userDetails = userDetailsService.loadUserByUserId(userId);

                    if (userDetails != null && jwtUtil.isTokenValid(token, userId)) {
                        UsernamePasswordAuthenticationToken authToken =
                                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

                        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                        SecurityContextHolder.getContext().setAuthentication(authToken);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Cannot set user authentication: {}", e);
        }

        chain.doFilter(request, response);
    }
    private String extractJwtFromCookies(HttpServletRequest request) {
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("jwtToken".equals(cookie.getName())) { // Cookie ismi "jwtToken" olarak varsayıldı
                    return cookie.getValue(); // Token'ı döndür
                }
            }
        }
        return null; // Cookie'de token bulunmazsa null döndür
    }

    // JwtRequestFilter sınıfına ekleyin
    public Long extractUserIdFromToken(HttpServletRequest request) {
        String jwt = null;
        Long userId = null;

        // 1. Authorization Header'ı kontrol et
        final String authorizationHeader = request.getHeader("Authorization");
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            jwt = authorizationHeader.substring(7);
            System.out.println("Extracted token: " + jwt);// "Bearer " kısmını atla
        } else {
            // 2. Eğer Authorization header yoksa, Cookie'den token'ı al
            jwt = extractJwtFromCookies(request);
        }

        // 3. Eğer JWT varsa işlem yap, yoksa hata verme
        if (jwt != null && !jwt.trim().isEmpty()) {
            try {
                userId = jwtUtil.extractUserId(jwt);
            } catch (Exception e) {
                System.out.println("JWT Hatası: " + e.getMessage()); // Hata logla
            }
        } else {
            System.out.println("JWT bulunamadı veya boş!"); // Debug için log
        }

        return userId; // Eğer UserID varsa döndür, yoksa null
    }


}
