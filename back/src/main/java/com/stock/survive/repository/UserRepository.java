package com.stock.survive.repository;

import com.stock.survive.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Integer> {
    Optional<User> findBySocialEmail(String email);
}