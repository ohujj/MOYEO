package com.moyeo.repository.meeting;

import com.moyeo.domain.meeting.Meeting;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface MeetingRepository extends JpaRepository<Meeting, Long> {

    Optional<Meeting> findByInviteCode(String inviteCode);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Meeting r where r.inviteCode = :inviteCode")
    Optional<Meeting> findByInviteCodeForUpdate(@Param("inviteCode") String inviteCode);
}
