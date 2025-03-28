package com.example.UChat.controller;


import com.example.UChat.model.AuthenticationRequest;
import com.example.UChat.model.AuthenticationResponse;
import com.example.UChat.model.RegisterRequest;
import com.example.UChat.model.User;
import com.example.UChat.repository.UserRepository;
import com.example.UChat.security.JwtUtil;
import com.example.UChat.security.MyUserDetailsService;
import com.example.UChat.service.AuthService;
import com.example.UChat.service.UserService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
public class AuthController {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private MyUserDetailsService myUserDetailsService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BCryptPasswordEncoder bCryptPasswordEncoder;

    private final AuthService authService;
    private final UserService userService;

    public AuthController(AuthService authService, UserService userService) {
        this.authService = authService;
        this.userService = userService;
    }


    @PostMapping("/login")
    public ResponseEntity<AuthenticationResponse> login(
            @RequestBody AuthenticationRequest authenticationRequest,
            HttpServletResponse response
    ) {

        UserDetails userDetails = myUserDetailsService.loadUserByUsername(authenticationRequest.getUsername());


        if (bCryptPasswordEncoder.matches(authenticationRequest.getPassword(), userDetails.getPassword())) {

            User user = userRepository.findByUsername(authenticationRequest.getUsername())
                    .orElseThrow(() -> new UsernameNotFoundException("User not found"));


            String token = jwtUtil.generateToken(user.getId());


            Cookie cookie = new Cookie("jwtToken", token);
            cookie.setHttpOnly(true);
            cookie.setSecure(true);
            cookie.setPath("/");

            response.addCookie(cookie);

            return ResponseEntity.ok(new AuthenticationResponse(token));
        }


        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }


    @PostMapping("/register")
    public ResponseEntity<AuthenticationResponse> register(@RequestBody RegisterRequest registerRequest)
    {
        return authService.register(registerRequest);
    }


    @GetMapping("/status")
    public ResponseEntity<?> checkAuthStatus(@CookieValue(name = "jwtToken", required = false) String token) {
        if (token == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Not authenticated");
        }


        Long userId = jwtUtil.extractUserId(token);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid token");
        }


        return ResponseEntity.ok().body(userService.getUserById(userId));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletResponse response) {

        Cookie cookie = new Cookie("jwtToken", "");
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);

        response.addCookie(cookie);

        return ResponseEntity.ok().body("Logged out successfully");
    }
}
