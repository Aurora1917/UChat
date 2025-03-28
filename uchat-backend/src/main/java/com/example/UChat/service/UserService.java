package com.example.UChat.service;


import com.example.UChat.model.User;
import com.example.UChat.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User registerUser(User user)
    {
        return userRepository.save(user);
    }

    public Optional<User> findUserByTag(String tag)
    {
        return userRepository.findByUserTag(tag);
    }

    public String getUserTagById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return user.getUserTag();
    }

    public Optional<User> getUserById(Long userId) {
        return userRepository.findById(userId);
    }

    public Long getUserIdByTag(String userTag) {
        User user = userRepository.findByUserTag(userTag.toUpperCase())
                .orElseThrow(() -> new RuntimeException("User not found with tag: " + userTag));
        return user.getId();
    }
}

