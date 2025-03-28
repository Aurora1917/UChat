package com.example.UChat.repository;


import com.example.UChat.model.FriendRequest;
import com.example.UChat.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface FriendRequestRepository extends JpaRepository<FriendRequest, Long> {

    List<FriendRequest> findByReceiverAndStatus(User receiver, FriendRequest.RequestStatus status);

    boolean existsBySenderAndReceiver(User sender, User receiver);





}


