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

Table rooms {
  id bigint [pk, increment, note: "모임 ID"]
  host_user_id bigint [not null, note: "모임을 만든 방장 사용자 ID"]
  name varchar(15) [not null, note: "모임 이름"]
  description varchar(100) [note: "모임 설명"]
  max_participants int [not null, note: "최대 참여 인원. 방장 포함"]
  schedule_mode varchar(20) [not null, note: "일정 설정 방식: VOTE/FIXED/NONE"]
  fixed_schedule_at datetime [note: "확정 일정. schedule_mode가 FIXED일 때 사용"]
  available_start_time time [note: "일정 투표 공통 시작 시간. schedule_mode가 VOTE일 때 사용"]
  available_end_time time [note: "일정 투표 공통 종료 시간. schedule_mode가 VOTE일 때 사용"]
  place_mode varchar(20) [not null, note: "장소 설정 방식: FIXED/RECOMMEND/NONE"]
  place_recommendation_strategy varchar(30) [note: "장소 추천 방식. place_mode가 RECOMMEND일 때 사용"]
  fixed_place_name varchar(100) [note: "확정 장소 이름. place_mode가 FIXED일 때 사용"]
  fixed_place_address varchar(255) [note: "확정 장소 주소. place_mode가 FIXED일 때 사용"]
  deadline_at datetime [not null, note: "모임 참여/응답 마감 일시"]
  invite_code varchar(20) [not null, unique, note: "초대 링크에 사용하는 고유 코드"]
  created_at datetime [not null, note: "모임 생성 일시"]
  updated_at datetime [not null, note: "모임 수정 일시"]

  indexes {
    invite_code [unique, name: "uk_rooms_invite_code"]
  }
}

Table room_schedule_candidates {
  id bigint [pk, increment, note: "일정 후보 ID"]
  room_id bigint [not null, note: "일정 후보가 속한 모임 ID"]
  candidate_date date [not null, note: "일정 투표 후보 날짜"]

  indexes {
    (room_id, candidate_date) [unique, name: "uk_room_schedule_candidates_room_date"]
  }
}

Table room_participants {
  id bigint [pk, increment, note: "모임 참여자 ID"]
  room_id bigint [not null, note: "참여한 모임 ID"]
  user_id bigint [note: "연결된 서비스 사용자 ID. 게스트는 null"]
  nickname varchar(30) [not null, note: "모임 안에서 표시할 닉네임"]
  password_hash varchar(100) [note: "게스트 참여 비밀번호 해시. 회원/방장은 null"]
  participant_type varchar(20) [not null, note: "참여자 타입: HOST/GUEST"]
  created_at datetime [not null, note: "참여 생성 일시"]

  indexes {
    (room_id, nickname) [unique, name: "uk_room_participants_room_nickname"]
    (room_id, user_id) [unique, name: "uk_room_participants_room_user"]
  }
}

Ref fk_login_accounts_user: login_accounts.user_id - users.id
Ref fk_social_accounts_user: social_accounts.user_id > users.id
Ref fk_rooms_host_user: rooms.host_user_id > users.id
Ref fk_room_schedule_candidates_room: room_schedule_candidates.room_id > rooms.id
Ref fk_room_participants_room: room_participants.room_id > rooms.id
Ref fk_room_participants_user: room_participants.user_id > users.id
```

## Notes

- `users` is the service user table.
- `login_accounts` stores local login credentials separately from the user profile.
- `social_accounts` stores provider identity for Kakao/Apple-style social login.
- `social_accounts.provider_user_id` is the provider-issued user identifier, not CI/DI.
- `rooms` stores the first milestone room creation and invite code base.
- `rooms.schedule_mode` supports `VOTE`, `FIXED`, and `NONE`.
- `rooms.place_mode` supports `FIXED`, `RECOMMEND`, and `NONE`.
- `rooms.place_recommendation_strategy` is used only when `place_mode` is `RECOMMEND`.
- `rooms.deadline_at` is calculated by the server from request `deadlineMinutes`, which is currently accepted in 10-minute units up to 72 hours.
- `rooms.available_start_time` and `rooms.available_end_time` are shared by all schedule voting candidate dates and are currently accepted in 1-hour units.
- `room_schedule_candidates` stores variable-length date candidates for schedule voting.
- `room_participants` stores host and guest participants.
- `room_participants.nickname` is unique only inside a room.
- `room_participants.user_id` is unique only inside a room when a participant is linked to a service user.
- Guest participants do not use `users.id` yet; `room_participants.user_id` is nullable for guest participation.
