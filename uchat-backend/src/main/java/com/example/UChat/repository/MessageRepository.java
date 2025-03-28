package com.example.UChat.repository;


import com.example.UChat.model.Message;
import com.example.UChat.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    List<Message> findMessagesBySenderIdAndReceiverIdOrSenderIdAndReceiverId(
            Long senderId1, Long receiverId1, Long senderId2, Long receiverId2);

    void deleteById(Long messageId);
}
