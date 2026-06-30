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
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.Comment;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Comment("모임 방")
@Table(
        name = "rooms",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_rooms_invite_code", columnNames = "invite_code")
        }
)
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("모임 ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "host_user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_rooms_host_user"))
    @Comment("모임을 만든 방장 사용자 ID")
    private User hostUser;

    @Column(nullable = false, length = 15)
    @Comment("모임 이름")
    private String name;

    @Column(length = 100)
    @Comment("모임 설명")
    private String description;

    @Column(nullable = false)
    @Comment("최대 참여 인원. 방장 포함")
    private Integer maxParticipants;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Comment("일정 설정 방식: VOTE/FIXED/NONE")
    private ScheduleMode scheduleMode;

    @Comment("확정 일정. schedule_mode가 FIXED일 때 사용")
    private LocalDateTime fixedScheduleAt;

    @Comment("일정 투표 공통 시작 시간. schedule_mode가 VOTE일 때 사용")
    private LocalTime availableStartTime;

    @Comment("일정 투표 공통 종료 시간. schedule_mode가 VOTE일 때 사용")
    private LocalTime availableEndTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Comment("장소 설정 방식: FIXED/RECOMMEND/NONE")
    private PlaceMode placeMode;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    @Comment("장소 추천 방식. place_mode가 RECOMMEND일 때 사용")
    private PlaceRecommendationStrategy placeRecommendationStrategy;

    @Column(length = 100)
    @Comment("확정 장소 이름. place_mode가 FIXED일 때 사용")
    private String fixedPlaceName;

    @Column(length = 255)
    @Comment("확정 장소 주소. place_mode가 FIXED일 때 사용")
    private String fixedPlaceAddress;

    @Column(nullable = false)
    @Comment("모임 참여/응답 마감 일시")
    private LocalDateTime deadlineAt;

    @Column(name = "invite_code", nullable = false, length = 20)
    @Comment("초대 링크에 사용하는 고유 코드")
    private String inviteCode;

    @Column(nullable = false)
    @Comment("모임 생성 일시")
    private LocalDateTime createdAt;

    @Column(nullable = false)
    @Comment("모임 수정 일시")
    private LocalDateTime updatedAt;

    protected Room() {
    }

    public Room(
            User hostUser,
            String name,
            String description,
            Integer maxParticipants,
            ScheduleMode scheduleMode,
            LocalDateTime fixedScheduleAt,
            LocalTime availableStartTime,
            LocalTime availableEndTime,
            PlaceMode placeMode,
            PlaceRecommendationStrategy placeRecommendationStrategy,
            String fixedPlaceName,
            String fixedPlaceAddress,
            LocalDateTime deadlineAt,
            String inviteCode
    ) {
        this.hostUser = hostUser;
        this.name = name;
        this.description = description;
        this.maxParticipants = maxParticipants;
        this.scheduleMode = scheduleMode;
        this.fixedScheduleAt = fixedScheduleAt;
        this.availableStartTime = availableStartTime;
        this.availableEndTime = availableEndTime;
        this.placeMode = placeMode;
        this.placeRecommendationStrategy = placeRecommendationStrategy;
        this.fixedPlaceName = fixedPlaceName;
        this.fixedPlaceAddress = fixedPlaceAddress;
        this.deadlineAt = deadlineAt;
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

    public ScheduleMode getScheduleMode() {
        return scheduleMode;
    }

    public LocalDateTime getFixedScheduleAt() {
        return fixedScheduleAt;
    }

    public LocalTime getAvailableStartTime() {
        return availableStartTime;
    }

    public LocalTime getAvailableEndTime() {
        return availableEndTime;
    }

    public PlaceMode getPlaceMode() {
        return placeMode;
    }

    public PlaceRecommendationStrategy getPlaceRecommendationStrategy() {
        return placeRecommendationStrategy;
    }

    public String getFixedPlaceName() {
        return fixedPlaceName;
    }

    public String getFixedPlaceAddress() {
        return fixedPlaceAddress;
    }

    public LocalDateTime getDeadlineAt() {
        return deadlineAt;
    }

    public String getInviteCode() {
        return inviteCode;
    }
}
