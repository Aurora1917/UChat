package com.example.UChat.repository;


import com.example.UChat.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User,Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByUserTag(String userTag);
    Optional<User> findById(Long id);

    @Query("SELECT u.userTag FROM User u WHERE u.id = :id")
    String getUserTagById(@Param("id") Long id);

    Long getIdByUserTag(String userTag);
}
