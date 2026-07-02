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
import org.hibernate.annotations.Comment;

import java.time.LocalDateTime;

@Entity
@Comment("모임 참여자")
@Table(
        name = "room_participants",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_room_participants_room_nickname", columnNames = {"room_id", "nickname"}),
                @UniqueConstraint(name = "uk_room_participants_room_user", columnNames = {"room_id", "user_id"})
        }
)
public class RoomParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("모임 참여자 ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false, foreignKey = @ForeignKey(name = "fk_room_participants_room"))
    @Comment("참여한 모임 ID")
    private Room room;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", foreignKey = @ForeignKey(name = "fk_room_participants_user"))
    @Comment("연결된 서비스 사용자 ID. 게스트는 null")
    private User user;

    @Column(nullable = false, length = 30)
    @Comment("모임 안에서 표시할 닉네임")
    private String nickname;

    @Column(name = "password_hash", length = 100)
    @Comment("게스트 참여 비밀번호 해시. 회원/방장은 null")
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Comment("참여자 타입: HOST/GUEST")
    private ParticipantType participantType;

    @Column(length = 255)
    @Comment("방장 또는 참여자의 출발지 주소. 중간지점 추천에서 사용")
    private String departureAddress;

    @Column(nullable = false)
    @Comment("참여 생성 일시")
    private LocalDateTime createdAt;

    protected RoomParticipant() {
    }

    private RoomParticipant(
            Room room,
            User user,
            String nickname,
            String passwordHash,
            ParticipantType participantType,
            String departureAddress
    ) {
        this.room = room;
        this.user = user;
        this.nickname = nickname;
        this.passwordHash = passwordHash;
        this.participantType = participantType;
        this.departureAddress = departureAddress;
    }

    public static RoomParticipant host(Room room, User user) {
        return host(room, user, null);
    }

    public static RoomParticipant host(Room room, User user, String departureAddress) {
        return new RoomParticipant(room, user, user.getNickname(), null, ParticipantType.HOST, departureAddress);
    }

    public static RoomParticipant guest(Room room, String nickname, String passwordHash) {
        return new RoomParticipant(room, null, nickname, passwordHash, ParticipantType.GUEST, null);
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

    public String getDepartureAddress() {
        return departureAddress;
    }
}
