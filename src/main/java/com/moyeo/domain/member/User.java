package com.moyeo.domain.member;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import org.hibernate.annotations.Comment;

import java.time.LocalDateTime;

@Entity
@Comment("서비스 사용자")
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("서비스 사용자 ID")
    private Long id;

    @Column(nullable = false, length = 30)
    @Comment("사용자 기본 닉네임. 전역 고유값이 아님")
    private String nickname;

    @Column(nullable = false)
    @Comment("사용자 생성 일시")
    private LocalDateTime createdAt;

    @Column(nullable = false)
    @Comment("사용자 정보 수정 일시")
    private LocalDateTime updatedAt;

    @Comment("사용자 탈퇴/삭제 일시. null이면 활성 상태")
    private LocalDateTime deletedAt;

    protected User() {
    }

    public User(String nickname) {
        this.nickname = nickname;
    }

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getNickname() {
        return nickname;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }
}
