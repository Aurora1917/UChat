package com.example.UChat.controller;

import com.example.UChat.model.FriendRequest;
import com.example.UChat.model.Friendship;
import com.example.UChat.model.User;
import com.example.UChat.repository.UserRepository;
import com.example.UChat.security.JwtRequestFilter;
import com.example.UChat.service.FriendRequestService;
import com.example.UChat.service.FriendshipService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("friends")
@CrossOrigin(origins = "http://localhost:3000",allowCredentials = "true")
public class FriendshipController {

    private final FriendRequestService friendRequestService;
    private final UserRepository userRepository;
    private final JwtRequestFilter jwtRequestFilter;
    private final FriendshipService friendshipService;


    public FriendshipController(FriendRequestService friendRequestService, UserRepository userRepository, JwtRequestFilter jwtRequestFilter, FriendshipService friendshipService) {
        this.friendRequestService = friendRequestService;
        this.userRepository = userRepository;
        this.jwtRequestFilter = jwtRequestFilter;
        this.friendshipService = friendshipService;
    }

    @PostMapping
    public ResponseEntity<String> sendFriendRequest(@RequestParam String receiverTag, HttpServletRequest request) {
        if (receiverTag == null || receiverTag.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Receiver tag cannot be empty!");
        }

        Long senderId = jwtRequestFilter.extractUserIdFromToken(request);
        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new RuntimeException("Sender not found"));

        friendRequestService.sendFriendRequest(sender.getUserTag(), receiverTag);
        return ResponseEntity.ok("Friend request sent!");
    }

    @GetMapping("/requests")
    public ResponseEntity<List<FriendRequest>> getFriendRequests(HttpServletRequest request) {
        Long userId = jwtRequestFilter.extractUserIdFromToken(request);
        System.out.println(userId);
        List<FriendRequest> requests = friendRequestService.getFriendRequestsByUserId(userId);
        return ResponseEntity.ok(requests);
    }

    @PostMapping("/accept/{requestId}")
    public ResponseEntity<String> acceptFriendRequest(@PathVariable Long requestId, HttpServletRequest request) {
        Long userId = jwtRequestFilter.extractUserIdFromToken(request);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        friendRequestService.acceptFriendRequest(requestId);
        return ResponseEntity.ok("Friend request accepted!");
    }

    @PostMapping("/reject/{requestId}")
    public ResponseEntity<String> rejectFriendRequest(@PathVariable Long requestId, HttpServletRequest request) {
        Long userId = jwtRequestFilter.extractUserIdFromToken(request);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));


        friendRequestService.rejectFriendRequest(requestId);

        return ResponseEntity.ok("Friend request rejected!");
    }


    @GetMapping
    public ResponseEntity<List<User>> getFriends(HttpServletRequest request) {
        Long userId = jwtRequestFilter.extractUserIdFromToken(request);
        System.out.println(userId);

        List<User> friends = friendshipService.getFriends(userId);

        return ResponseEntity.ok(friends);
    }

    @PostMapping("/delete")
    public ResponseEntity<String> deleteFriend(HttpServletRequest request,@RequestParam Long friendId)
    {
        Long userId = jwtRequestFilter.extractUserIdFromToken(request);

        friendshipService.removeFriend(userId,friendId);

        return ResponseEntity.ok("Friend has been deleted!");
    }
}