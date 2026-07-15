# DB Diagram

> Purpose: Keep the current JPA entity/table structure copy-paste ready for [dbdiagram.io](https://dbdiagram.io).
> Update trigger: JPA entity, table name, column, index, unique constraint, or relationship changes.

## DBML

```dbml
Table users {
  id bigint [pk, increment, note: "서비스 사용자 ID"]
  nickname varchar(30) [not null, note: "사용자 기본 닉네임. 전역 고유값이 아님"]
  created_at datetime [not null, note: "사용자 생성 일시"]
  updated_at datetime [not null, note: "사용자 정보 수정 일시"]
  deleted_at datetime [note: "사용자 탈퇴/삭제 일시. null이면 활성 상태"]
}

Table login_accounts {
  id bigint [pk, increment, note: "로컬 로그인 계정 ID"]
  user_id bigint [not null, unique, note: "연결된 서비스 사용자 ID"]
  login_id varchar(50) [not null, unique, note: "로그인에 사용하는 고유 ID"]
  password_hash varchar(100) [not null, note: "BCrypt로 해시한 비밀번호"]
  created_at datetime [not null, note: "로컬 로그인 계정 생성 일시"]

  indexes {
    login_id [unique, name: "uk_login_accounts_login_id"]
    user_id [unique, name: "uk_login_accounts_user_id"]
  }
}

Table social_accounts {
  id bigint [pk, increment, note: "소셜 계정 연결 ID"]
  user_id bigint [not null, note: "연결된 서비스 사용자 ID"]
  provider varchar(20) [not null, note: "소셜 로그인 제공자"]
  provider_user_id varchar(191) [not null, note: "소셜 제공자가 발급한 사용자 식별자"]
  email varchar(255) [note: "소셜 제공자로부터 받은 이메일"]
  created_at datetime [not null, note: "소셜 계정 연결 생성 일시"]

  indexes {
    (provider, provider_user_id) [unique, name: "uk_social_accounts_provider_user"]
    (user_id, provider) [unique, name: "uk_social_accounts_user_provider"]
  }
}

Table meetings {
  id bigint [pk, increment, note: "모임 ID"]
  host_user_id bigint [not null, note: "모임을 만든 방장 사용자 ID"]
  name varchar(15) [not null, note: "모임 이름"]
  description varchar(100) [note: "모임 설명"]
  max_participants int [not null, note: "최대 참여 인원. 방장 포함"]
  planning_type varchar(30) [not null, note: "모임 생성 유형: SCHEDULE_ONLY/PLACE_ONLY/SCHEDULE_AND_PLACE"]
  schedule_mode varchar(20) [not null, note: "일정 설정 방식: VOTE/FIXED/NONE"]
  fixed_schedule_at datetime [note: "확정 일정. schedule_mode가 FIXED일 때 사용"]
  available_start_time time [note: "일정 투표 공통 시작 시간. schedule_mode가 VOTE일 때 사용"]
  available_end_time time [note: "일정 투표 공통 종료 시간. schedule_mode가 VOTE일 때 사용"]
  place_mode varchar(20) [not null, note: "장소 설정 방식: FIXED/RECOMMEND/NONE"]
  place_recommendation_strategy varchar(30) [note: "생성 시 선택한 장소 추천 방식. place_mode가 RECOMMEND일 때 사용하며 1차 MVP에서는 생성 후 변경하지 않음"]
  fixed_place_name varchar(100) [note: "확정 장소 이름. place_mode가 FIXED일 때 사용"]
  fixed_place_address varchar(255) [note: "확정 장소 주소. place_mode가 FIXED일 때 사용"]
  deadline_at datetime [not null, note: "모임 참여/응답 마감 일시"]
  invite_code varchar(20) [not null, unique, note: "초대 링크에 사용하는 고유 코드"]
  created_at datetime [not null, note: "모임 생성 일시"]
  updated_at datetime [not null, note: "모임 수정 일시"]

  indexes {
    invite_code [unique, name: "uk_meetings_invite_code"]
  }
}

Table meeting_schedule_candidates {
  id bigint [pk, increment, note: "일정 후보 ID"]
  meeting_id bigint [not null, note: "일정 후보가 속한 모임 ID"]
  candidate_date date [not null, note: "일정 투표 후보 날짜"]

  indexes {
    (meeting_id, candidate_date) [unique, name: "uk_meeting_schedule_candidates_meeting_date"]
  }
}

Table meeting_participant_schedule_availabilities {
  id bigint [pk, increment, note: "참여자 일정 가능 시간 ID"]
  participant_id bigint [not null, note: "일정 가능 시간을 입력한 참여자 ID"]
  schedule_candidate_id bigint [not null, note: "일정 후보 날짜 ID"]
  start_time time [not null, note: "가능 시간 시작"]
  end_time time [not null, note: "가능 시간 종료"]
  created_at datetime [not null, note: "일정 가능 시간 생성 일시"]

  indexes {
    (participant_id, schedule_candidate_id, start_time, end_time) [unique, name: "uk_meeting_participant_schedule_availabilities_slot"]
  }
}

Table meeting_participants {
  id bigint [pk, increment, note: "모임 참여자 ID"]
  meeting_id bigint [not null, note: "참여한 모임 ID"]
  user_id bigint [note: "연결된 서비스 사용자 ID. 게스트는 null"]
  nickname varchar(30) [not null, note: "모임 안에서 표시할 닉네임"]
  password_hash varchar(100) [note: "참여 비밀번호 해시. 방장은 null"]
  participant_type varchar(20) [not null, note: "참여자 타입: HOST/MEMBER/GUEST"]
  departure_name varchar(30) [note: "방장 또는 참여자 출발지 이름. 중간지점 추천에서 사용"]
  departure_address varchar(255) [note: "방장 또는 참여자 출발지 주소. 중간지점 추천에서 사용"]
  departure_latitude decimal(10,7) [note: "방장 또는 참여자 출발지 위도. 중간지점 추천에서 사용"]
  departure_longitude decimal(10,7) [note: "방장 또는 참여자 출발지 경도. 중간지점 추천에서 사용"]
  transportation_mode varchar(20) [note: "중간지점 추천에 사용할 이동수단: PUBLIC_TRANSIT/CAR"]
  created_at datetime [not null, note: "참여 생성 일시"]

  indexes {
    (meeting_id, user_id) [unique, name: "uk_meeting_participants_meeting_user"]
  }
}

Ref fk_login_accounts_user: login_accounts.user_id - users.id
Ref fk_social_accounts_user: social_accounts.user_id > users.id
Ref fk_meetings_host_user: meetings.host_user_id > users.id
Ref fk_meeting_schedule_candidates_meeting: meeting_schedule_candidates.meeting_id > meetings.id
Ref fk_meeting_participants_meeting: meeting_participants.meeting_id > meetings.id
Ref fk_meeting_participants_user: meeting_participants.user_id > users.id
Ref fk_meeting_participant_schedule_availabilities_participant: meeting_participant_schedule_availabilities.participant_id > meeting_participants.id
Ref fk_meeting_participant_schedule_availabilities_candidate: meeting_participant_schedule_availabilities.schedule_candidate_id > meeting_schedule_candidates.id
```

## Notes

- `users` is the service user table.
- `login_accounts` stores local login credentials separately from the user profile.
- `social_accounts` stores provider identity for Kakao/Apple-style social login.
- `social_accounts.provider_user_id` is the provider-issued user identifier, not CI/DI.
- `meetings` stores the first milestone meeting creation and invite code base.
- `meetings.planning_type` stores the FAB-selected creation type: `SCHEDULE_ONLY`, `PLACE_ONLY`, or `SCHEDULE_AND_PLACE`.
- `meetings.schedule_mode` supports `VOTE`, `FIXED`, and `NONE`.
- `meetings.place_mode` supports `FIXED`, `RECOMMEND`, and `NONE`.
- `meetings.place_recommendation_strategy` stores the selected recommendation strategy when `place_mode` is `RECOMMEND`; the first MVP does not change it after creation.
- `meetings.deadline_at` is calculated by the server from request `deadlineMinutes`, which is currently accepted in 10-minute units up to 72 hours.
- `meetings.available_start_time` and `meetings.available_end_time` are shared by all schedule voting candidate dates and are currently accepted in 1-hour units.
- `meeting_schedule_candidates` stores variable-length date candidates for schedule voting.
- `meeting_participant_schedule_availabilities` stores INV-02 participant-selected availability slots.
- `meeting_participants` stores host, logged-in member, and guest participants.
- `meeting_participants.departure_name`, `departure_address`, `departure_latitude`, `departure_longitude`, and `transportation_mode` store host and participant departure snapshots for place coordination.
- Guest `meeting_participants.nickname` duplication is rejected only against other guests in the same meeting by the join application logic; the table does not keep a general nickname unique constraint.
- `meeting_participants.user_id` is unique only inside a meeting when a participant is linked to a service user.
- Logged-in member participants use `users.id`; guest participants keep `meeting_participants.user_id` null.
