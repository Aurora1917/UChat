package com.example.UChat.service;


import com.example.UChat.model.AuthenticationRequest;
import com.example.UChat.model.AuthenticationResponse;
import com.example.UChat.model.RegisterRequest;
import com.example.UChat.model.User;
import com.example.UChat.repository.UserRepository;
import com.example.UChat.security.JwtUtil;
import com.example.UChat.security.MyUserDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Random;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final MyUserDetailsService userDetailsService;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    @Autowired
    public AuthService(AuthenticationManager authenticationManager,
                       MyUserDetailsService userDetailsService,
                       JwtUtil jwtUtil, UserRepository userRepository,BCryptPasswordEncoder passwordEncoder) {
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public ResponseEntity<AuthenticationResponse> login(AuthenticationRequest authRequest) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        authRequest.getUsername(), authRequest.getPassword()
                )
        );

        UserDetails userDetails = userDetailsService.loadUserByUsername(authRequest.getUsername());


        User user = (User) userDetails;
        String token = jwtUtil.generateToken(user.getId());
        return ResponseEntity.ok(new AuthenticationResponse(token));
    }

    public ResponseEntity<AuthenticationResponse> register(RegisterRequest registerRequest) {


        if (userRepository.findByUsername(registerRequest.getUsername()).isPresent()) {
            return ResponseEntity.badRequest().body(new AuthenticationResponse("Username already taken"));
        }

        String userTag = generateUniqueUserTag();
        // Kullanıcı oluştur
        User newUser = new User();
        newUser.setUsername(registerRequest.getUsername());
        newUser.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
        newUser.setEmail(registerRequest.getEmail());
        newUser.setRole(User.Role.USER);
        newUser.setUserTag(userTag);

        userRepository.save(newUser);

        // Kullanıcıya JWT token oluştur
        String token = jwtUtil.generateToken(newUser.getId());

        return ResponseEntity.ok(new AuthenticationResponse(token));
    }

    public String generateUniqueUserTag() {
        Random random = new Random();
        String userTag;
        do {
            userTag = "U" + (1000 + random.nextInt(9000)); // U1000 - U9999 arasında rastgele bir ID
        } while (userRepository.findByUserTag(userTag).isPresent()); // Aynısı varsa tekrar üret
        return userTag;
    }
}