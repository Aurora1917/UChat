package com.example.UChat.service;

import com.example.UChat.model.FriendRequest;
import com.example.UChat.model.Friendship;
import com.example.UChat.model.User;
import com.example.UChat.repository.FriendRequestRepository;
import com.example.UChat.repository.FriendshipRepository;
import com.example.UChat.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
public class FriendRequestService {

    private final FriendRequestRepository friendRequestRepository;
    private final UserRepository userRepository;
    private final FriendshipRepository friendshipRepository;

    public FriendRequestService(FriendRequestRepository friendRequestRepository, UserRepository userRepository, FriendshipRepository friendshipRepository) {
        this.friendRequestRepository = friendRequestRepository;
        this.userRepository = userRepository;
        this.friendshipRepository = friendshipRepository;
    }

    public void sendFriendRequest(String senderTag, String receiverTag) {
        User sender = userRepository.findByUserTag(senderTag)
                .orElseThrow(() -> new RuntimeException("Sender not found"));
        User receiver = userRepository.findByUserTag(receiverTag)
                .orElseThrow(() -> new RuntimeException("Receiver not found"));

        if (friendshipRepository.existsByUserIdAndFriendId(sender.getId(), receiver.getId())) {
            throw new RuntimeException("You are already friends!");
        }

        if (friendRequestRepository.existsBySenderAndReceiver(sender, receiver)) {
            throw new RuntimeException("Friend request already sent!");
        }

        if(Objects.equals(receiverTag, senderTag))
        {
            throw new RuntimeException("You can't send friend request to yourself!");
        }

        FriendRequest friendRequest = new FriendRequest();
        friendRequest.setSender(sender);
        friendRequest.setReceiver(receiver);
        friendRequest.setStatus(FriendRequest.RequestStatus.PENDING);
        friendRequestRepository.save(friendRequest);
    }

    public void acceptFriendRequest(Long requestId) {
        FriendRequest request = friendRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Request not found"));

        if (request.getStatus() != FriendRequest.RequestStatus.PENDING) {
            throw new RuntimeException("Request is not pending!");
        }

        request.setStatus(FriendRequest.RequestStatus.ACCEPTED);
        friendRequestRepository.save(request);

        Long senderId = request.getSender().getId();
        Long receiverId = request.getReceiver().getId();

        if (!friendshipRepository.existsByUserIdAndFriendId(senderId, receiverId)) {
            friendshipRepository.save(new Friendship(senderId, receiverId));
            friendshipRepository.save(new Friendship(receiverId, senderId));
            friendRequestRepository.deleteById(requestId);
        }


    }

    public void rejectFriendRequest(Long requestId) {
        FriendRequest request = friendRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Request not found"));

        if (request.getStatus() != FriendRequest.RequestStatus.PENDING) {
            throw new RuntimeException("Request is not pending!");
        }

        request.setStatus(FriendRequest.RequestStatus.REJECTED);
        friendRequestRepository.save(request);
        friendRequestRepository.deleteById(requestId);
    }

    public List<FriendRequest> getFriendRequestsByUserId(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return friendRequestRepository.findByReceiverAndStatus(user, FriendRequest.RequestStatus.PENDING);
    }
}
