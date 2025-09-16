package com.stock.survive.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Table(name = "users")
public class User {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_no")
    private Integer id;

    @Column(name = "social_email", length = 254, nullable = false, unique = true)
    private String socialEmail;

    @Builder.Default
    @Column(nullable = false)
    private boolean cancel = false;

    @Column(name = "nickname",length = 30, nullable = false)
    private String nickname;

    @Builder.Default
    @Column(name = "total_reward", nullable = false)
    private Integer totalReward = 10_000_000;

    @Builder.Default
    @Column(name = "cash",nullable = false)
    private Integer cash = 10_000_000;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        // createdAt/updatedAt 자동 세팅
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (updatedAt == null) updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
