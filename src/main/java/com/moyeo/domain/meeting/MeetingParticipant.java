package com.moyeo.domain.meeting;

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

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Comment("모임 참여자")
@Table(
        name = "meeting_participants",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_meeting_participants_meeting_user", columnNames = {"meeting_id", "user_id"})
        }
)
public class MeetingParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("모임 참여자 ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "meeting_id", nullable = false, foreignKey = @ForeignKey(name = "fk_meeting_participants_meeting"))
    @Comment("참여한 모임 ID")
    private Meeting meeting;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", foreignKey = @ForeignKey(name = "fk_meeting_participants_user"))
    @Comment("연결된 서비스 사용자 ID. 게스트는 null")
    private User user;

    @Column(nullable = false, length = 30)
    @Comment("모임 안에서 표시할 닉네임")
    private String nickname;

    @Column(name = "password_hash", length = 100)
    @Comment("게스트 참여 비밀번호 해시. 방장과 로그인 회원은 null")
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Comment("참여자 타입: HOST/MEMBER/GUEST")
    private ParticipantType participantType;

    @Column(name = "departure_name", length = 30)
    @Comment("방장 또는 참여자의 출발지 이름. 중간지점 추천에서 사용")
    private String departureName;

    @Column(name = "departure_address", length = 255)
    @Comment("방장 또는 참여자의 출발지 주소. 중간지점 추천에서 사용")
    private String departureAddress;

    @Column(name = "departure_latitude", precision = 10, scale = 7)
    @Comment("방장 또는 참여자의 출발지 위도. 중간지점 추천에서 사용")
    private BigDecimal departureLatitude;

    @Column(name = "departure_longitude", precision = 10, scale = 7)
    @Comment("방장 또는 참여자의 출발지 경도. 중간지점 추천에서 사용")
    private BigDecimal departureLongitude;

    @Enumerated(EnumType.STRING)
    @Column(name = "transportation_mode", length = 20)
    @Comment("중간지점 추천에 사용할 이동수단: PUBLIC_TRANSIT/CAR")
    private TransportationMode transportationMode;

    @Column(nullable = false)
    @Comment("참여 생성 일시")
    private LocalDateTime createdAt;

    protected MeetingParticipant() {
    }

    private MeetingParticipant(
            Meeting meeting,
            User user,
            String nickname,
            String passwordHash,
            ParticipantType participantType,
            String departureName,
            String departureAddress,
            BigDecimal departureLatitude,
            BigDecimal departureLongitude,
            TransportationMode transportationMode
    ) {
        this.meeting = meeting;
        this.user = user;
        this.nickname = nickname;
        this.passwordHash = passwordHash;
        this.participantType = participantType;
        this.departureName = departureName;
        this.departureAddress = departureAddress;
        this.departureLatitude = departureLatitude;
        this.departureLongitude = departureLongitude;
        this.transportationMode = transportationMode;
    }

    public static MeetingParticipant host(Meeting meeting, User user) {
        return host(meeting, user, null, null, null, null, null);
    }

    public static MeetingParticipant host(
            Meeting meeting,
            User user,
            String departureName,
            String departureAddress,
            BigDecimal departureLatitude,
            BigDecimal departureLongitude,
            TransportationMode transportationMode
    ) {
        return new MeetingParticipant(
                meeting,
                user,
                user.getNickname(),
                null,
                ParticipantType.HOST,
                departureName,
                departureAddress,
                departureLatitude,
                departureLongitude,
                transportationMode
        );
    }

    public static MeetingParticipant guest(Meeting meeting, String nickname, String passwordHash) {
        return new MeetingParticipant(meeting, null, nickname, passwordHash, ParticipantType.GUEST, null, null, null, null, null);
    }

    public static MeetingParticipant member(Meeting meeting, User user, String nickname) {
        return new MeetingParticipant(meeting, user, nickname, null, ParticipantType.MEMBER, null, null, null, null, null);
    }

    public void updateDeparture(
            String departureName,
            String departureAddress,
            BigDecimal departureLatitude,
            BigDecimal departureLongitude,
            TransportationMode transportationMode
    ) {
        this.departureName = departureName;
        this.departureAddress = departureAddress;
        this.departureLatitude = departureLatitude;
        this.departureLongitude = departureLongitude;
        this.transportationMode = transportationMode;
    }

    @PrePersist
    void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Meeting getMeeting() {
        return meeting;
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

    public String getDepartureName() {
        return departureName;
    }

    public String getDepartureAddress() {
        return departureAddress;
    }

    public BigDecimal getDepartureLatitude() {
        return departureLatitude;
    }

    public BigDecimal getDepartureLongitude() {
        return departureLongitude;
    }

    public TransportationMode getTransportationMode() {
        return transportationMode;
    }
}
