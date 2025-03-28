package com.example.UChat.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.SignatureException;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class JwtUtil {

    private final String SECRET_KEY = "ak26qbTvn";

    public String generateToken(Long id) {
        long expirationTimeMillis = 1000 * 60 * 60; // 1 hour in milliseconds
        return Jwts.builder()
                .setSubject(String.valueOf(id))
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expirationTimeMillis))
                .signWith(SignatureAlgorithm.HS256, SECRET_KEY)
                .compact();
    }

    public Long extractUserId(String token) {
        if (token == null || token.trim().isEmpty()) {
            return null;
        }

        try {
            return Long.parseLong(extractAllClaims(token).getSubject());
        } catch (MalformedJwtException e) {
            System.out.println("Invalid JWT token format: " + e.getMessage());
            return null;
        } catch (ExpiredJwtException e) {
            System.out.println("JWT token is expired: " + e.getMessage());
            return null;
        } catch (SignatureException e) {
            System.out.println("Invalid JWT signature: " + e.getMessage());
            return null;
        } catch (Exception e) {
            System.out.println("Error extracting userId from token: " + e.getMessage());
            return null;
        }
    }

    public Date extractExpiration(String token) {
        if (token == null || token.trim().isEmpty()) {
            return null;
        }

        try {
            return extractAllClaims(token).getExpiration();
        } catch (Exception e) {
            System.out.println("Error extracting expiration from token: " + e.getMessage());
            return null;
        }
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .setSigningKey(SECRET_KEY)
                .parseClaimsJws(token)
                .getBody();
    }

    public boolean isTokenValid(String token, Long userId) {
        if (token == null || userId == null) {
            return false;
        }

        try {
            Long extractedUserId = extractUserId(token);
            return (extractedUserId != null && extractedUserId.equals(userId) && !isTokenExpired(token));
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isTokenExpired(String token) {
        Date expiration = extractExpiration(token);
        return expiration != null && expiration.before(new Date());
    }
}