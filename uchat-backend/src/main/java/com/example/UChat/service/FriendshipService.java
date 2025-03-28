package com.example.UChat.service;

import com.example.UChat.model.Friendship;
import com.example.UChat.model.User;
import com.example.UChat.repository.FriendshipRepository;
import com.example.UChat.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class FriendshipService {

    private final FriendshipRepository friendshipRepository;
    private final UserRepository userRepository;

    public FriendshipService(FriendshipRepository friendshipRepository, UserRepository userRepository) {
        this.friendshipRepository = friendshipRepository;
        this.userRepository = userRepository;
    }

    public List<User> getFriends(Long userId) {
        List<Long> friendIds = friendshipRepository.findByUserId(userId)
                .stream()
                .map(Friendship::getFriendId)
                .collect(Collectors.toList());

        return userRepository.findAllById(friendIds);
    }

    @Transactional
    public void addFriend(Long userId, Long friendId) {
        if (userId.equals(friendId)) {
            throw new IllegalArgumentException("User cannot be friend with themselves.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        User friend = userRepository.findById(friendId)
                .orElseThrow(() -> new RuntimeException("Friend not found"));

        if (friendshipRepository.existsByUserIdAndFriendId(userId, friendId)) {
            throw new IllegalStateException("You're already friends!.");
        }

        friendshipRepository.save(new Friendship(userId, friendId));
        friendshipRepository.save(new Friendship(friendId, userId));
    }

    @Transactional
    public void removeFriend(Long userId, Long friendId) {
        friendshipRepository.deleteByUserIdAndFriendId(userId, friendId);
        friendshipRepository.deleteByUserIdAndFriendId(friendId, userId);


    }
}
