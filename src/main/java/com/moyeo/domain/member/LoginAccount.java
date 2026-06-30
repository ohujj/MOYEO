package com.moyeo.domain.member;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.Comment;

import java.time.LocalDateTime;

@Entity
@Comment("로컬 ID/PW 로그인 계정")
@Table(
        name = "login_accounts",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_login_accounts_login_id", columnNames = "login_id"),
                @UniqueConstraint(name = "uk_login_accounts_user_id", columnNames = "user_id")
        }
)
public class LoginAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("로컬 로그인 계정 ID")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_login_accounts_user"))
    @Comment("연결된 서비스 사용자 ID")
    private User user;

    @Column(name = "login_id", nullable = false, length = 50)
    @Comment("로그인에 사용하는 고유 ID")
    private String loginId;

    @Column(name = "password_hash", nullable = false, length = 100)
    @Comment("BCrypt로 해시한 비밀번호")
    private String passwordHash;

    @Column(nullable = false)
    @Comment("로컬 로그인 계정 생성 일시")
    private LocalDateTime createdAt;

    protected LoginAccount() {
    }

    public LoginAccount(User user, String loginId, String passwordHash) {
        this.user = user;
        this.loginId = loginId;
        this.passwordHash = passwordHash;
    }

    @PrePersist
    void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public String getLoginId() {
        return loginId;
    }

    public String getPasswordHash() {
        return passwordHash;
    }
}
