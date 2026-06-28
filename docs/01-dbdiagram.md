# DB Diagram

> Purpose: Keep the current JPA entity/table structure copy-paste ready for [dbdiagram.io](https://dbdiagram.io).
> Update trigger: JPA entity, table name, column, index, unique constraint, or relationship changes.

## DBML

```dbml
Table users {
  id bigint [pk, increment]
  nickname varchar(30) [not null]
  created_at datetime [not null]
  updated_at datetime [not null]
  deleted_at datetime
}

Table login_accounts {
  id bigint [pk, increment]
  user_id bigint [not null, unique]
  login_id varchar(50) [not null, unique]
  password_hash varchar(100) [not null]
  created_at datetime [not null]

  indexes {
    login_id [unique, name: "uk_login_accounts_login_id"]
    user_id [unique, name: "uk_login_accounts_user_id"]
  }
}

Table social_accounts {
  id bigint [pk, increment]
  user_id bigint [not null]
  provider varchar(20) [not null]
  provider_user_id varchar(191) [not null]
  email varchar(255)
  created_at datetime [not null]

  indexes {
    (provider, provider_user_id) [unique, name: "uk_social_accounts_provider_user"]
    (user_id, provider) [unique, name: "uk_social_accounts_user_provider"]
  }
}

Table rooms {
  id bigint [pk, increment]
  host_user_id bigint [not null]
  name varchar(15) [not null]
  description varchar(100)
  max_participants int [not null]
  invite_code varchar(20) [not null, unique]
  created_at datetime [not null]
  updated_at datetime [not null]

  indexes {
    invite_code [unique, name: "uk_rooms_invite_code"]
  }
}

Table room_participants {
  id bigint [pk, increment]
  room_id bigint [not null]
  user_id bigint
  nickname varchar(30) [not null]
  password_hash varchar(100)
  participant_type varchar(20) [not null]
  created_at datetime [not null]

  indexes {
    (room_id, nickname) [unique, name: "uk_room_participants_room_nickname"]
  }
}

Ref fk_login_accounts_user: login_accounts.user_id - users.id
Ref fk_social_accounts_user: social_accounts.user_id > users.id
Ref fk_rooms_host_user: rooms.host_user_id > users.id
Ref fk_room_participants_room: room_participants.room_id > rooms.id
Ref fk_room_participants_user: room_participants.user_id > users.id
```

## Notes

- `users` is the service user table.
- `login_accounts` stores local login credentials separately from the user profile.
- `social_accounts` stores provider identity for Kakao/Apple-style social login.
- `social_accounts.provider_user_id` is the provider-issued user identifier, not CI/DI.
- `rooms` stores the first milestone room creation and invite code base.
- `room_participants` stores host and guest participants.
- `room_participants.nickname` is unique only inside a room.
- Guest participants do not use `users.id` yet; `room_participants.user_id` is nullable for guest participation.
