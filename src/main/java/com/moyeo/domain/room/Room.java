package com.moyeo.domain.room;

import com.moyeo.domain.member.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "rooms",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_rooms_invite_code", columnNames = "invite_code")
        }
)
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "host_user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_rooms_host_user"))
    private User hostUser;

    @Column(nullable = false, length = 15)
    private String name;

    @Column(length = 100)
    private String description;

    @Column(nullable = false)
    private Integer maxParticipants;

    @Column(name = "invite_code", nullable = false, length = 20)
    private String inviteCode;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    protected Room() {
    }

    public Room(User hostUser, String name, String description, Integer maxParticipants, String inviteCode) {
        this.hostUser = hostUser;
        this.name = name;
        this.description = description;
        this.maxParticipants = maxParticipants;
        this.inviteCode = inviteCode;
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

    public User getHostUser() {
        return hostUser;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Integer getMaxParticipants() {
        return maxParticipants;
    }

    public String getInviteCode() {
        return inviteCode;
    }
}
