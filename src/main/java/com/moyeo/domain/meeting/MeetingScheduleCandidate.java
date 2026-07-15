package com.moyeo.domain.meeting;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.Comment;

import java.time.LocalDate;

@Entity
@Comment("모임 일정 투표 후보 날짜")
@Table(
        name = "meeting_schedule_candidates",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_meeting_schedule_candidates_meeting_date",
                        columnNames = {"meeting_id", "candidate_date"}
                )
        }
)
public class MeetingScheduleCandidate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("일정 후보 ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "meeting_id", nullable = false, foreignKey = @ForeignKey(name = "fk_meeting_schedule_candidates_meeting"))
    @Comment("일정 후보가 속한 모임 ID")
    private Meeting meeting;

    @Column(name = "candidate_date", nullable = false)
    @Comment("일정 투표 후보 날짜")
    private LocalDate candidateDate;

    protected MeetingScheduleCandidate() {
    }

    public MeetingScheduleCandidate(Meeting meeting, LocalDate candidateDate) {
        this.meeting = meeting;
        this.candidateDate = candidateDate;
    }

    public Long getId() {
        return id;
    }

    public Meeting getMeeting() {
        return meeting;
    }

    public LocalDate getCandidateDate() {
        return candidateDate;
    }
}
