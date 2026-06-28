package com.moyeo.domain.room;

import com.moyeo.domain.member.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "room_participants",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_room_participants_room_nickname", columnNames = {"room_id", "nickname"})
        }
)
public class RoomParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false, foreignKey = @ForeignKey(name = "fk_room_participants_room"))
    private Room room;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", foreignKey = @ForeignKey(name = "fk_room_participants_user"))
    private User user;

    @Column(nullable = false, length = 30)
    private String nickname;

    @Column(name = "password_hash", length = 100)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ParticipantType participantType;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    protected RoomParticipant() {
    }

    private RoomParticipant(
            Room room,
            User user,
            String nickname,
            String passwordHash,
            ParticipantType participantType
    ) {
        this.room = room;
        this.user = user;
        this.nickname = nickname;
        this.passwordHash = passwordHash;
        this.participantType = participantType;
    }

    public static RoomParticipant host(Room room, User user) {
        return new RoomParticipant(room, user, user.getNickname(), null, ParticipantType.HOST);
    }

    public static RoomParticipant guest(Room room, String nickname, String passwordHash) {
        return new RoomParticipant(room, null, nickname, passwordHash, ParticipantType.GUEST);
    }

    @PrePersist
    void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Room getRoom() {
        return room;
    }

    public User getUser() {
        return user;
    }

    public String getNickname() {
        return nickname;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public ParticipantType getParticipantType() {
        return participantType;
    }
}
