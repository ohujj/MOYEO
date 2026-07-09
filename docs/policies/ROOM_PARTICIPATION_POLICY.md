# Room Participation Policy

> Purpose: Canonical domain policy for the current room creation, invite, guest
> join, and participation input flows.

DOMAIN-001: Undocumented domain behavior must not be implemented as product
policy.

If implementation or review requires a product/domain decision that is not backed
by human-defined policy, classify it as `POLICY_UNDEFINED`. Do not promote Codex
general best practice into domain policy.

## Room Creation

- For the first MVP, room creation is completed with one final API request after
  the creation steps are filled out.
- Draft or step-by-step room creation should be added later as a separate draft
  flow if product policy requires it.
- Room creation is selected by `planningType`: `SCHEDULE_ONLY`, `PLACE_ONLY`, or
  `SCHEDULE_AND_PLACE`.
- Schedule mode is currently stored as `VOTE`, `FIXED`, or `NONE`, but current
  room creation derives it from `planningType` and does not accept fixed schedule
  input.
- Place mode is currently stored as `FIXED`, `RECOMMEND`, or `NONE`, but current
  room creation derives it from `planningType` and does not accept fixed place
  input.
- Fixed schedule/place direct input is excluded from the current MVP creation
  flow and may be reconsidered in a later product discussion.
- Place recommendation strategy is separated from place mode. The first MVP
  keeps the room creation strategy fixed after creation; later place
  recommendation/finalization flow may revisit switching between middle-point and
  random recommendations.
- Middle-point room creation stores the host departure name, address, latitude,
  longitude, and transportation mode snapshot on the host `room_participants`
  row. Actual middle-point calculation remains deferred.
- Room creation receives `deadlineMinutes`; the server calculates and returns
  `deadlineAt`.
- `deadlineMinutes` is currently accepted in 10-minute units from 10 minutes up
  to 72 hours. A zero-minute deadline is not allowed.
- `deadlineAt` is calculated from the server processing time of the final room
  creation request. Any client-side expected end time is only a preview and may
  differ if the user stays on the screen before submitting.
- Schedule voting candidate dates are stored as separate rows and are temporarily
  limited to 21 dates by request validation until the final 3-week policy is
  confirmed. Keep the limit isolated in the room creation constraints so it can
  be changed without reshaping the API.
- Schedule voting applies the same available time range to every selected
  candidate date.
- Schedule voting time ranges are currently accepted in 1-hour units.

## Room Participant Identity

- Room participant nicknames are unique only inside each room.
- A service user should not be linked to the same room more than once; enforce
  this with a room-scoped uniqueness rule such as `unique(room_id, user_id)`.
- Guest participants currently have nullable `user_id`, so multiple guest
  participants remain allowed.
- Current participant behavior is defined for `HOST` and `GUEST`.

## Invite and Guest Join

- INV-01 invite entry is currently implemented through invite-code lookup. It
  returns the current participation availability status for the entry screen.
- If both the deadline and participant limit block joining, the deadline-passed
  status takes priority in the entry response.
- INV-01 guest join currently creates only the participant row with nickname and
  password.
- Guest join currently accepts only nickname and password. Guest departure
  address, coordinates, and transportation mode are saved later through the
  INV-02 participation input API when the room includes place coordination.
- A repeated guest join attempt with the same nickname should continue to return
  a duplicate nickname conflict, even if the same password is provided.
- Guest participation is rejected after the room `deadlineAt`.
- Guest participation checks the current participant count before saving.
- To prevent concurrent guest joins from exceeding `maxParticipants`, guest
  participation may acquire a pessimistic write lock on the target room row
  during the join transaction.
- Keep this lock limited to the room join path; ordinary invite-code lookup
  should remain read-only.

## Participation Input

- The first room participation expansion covers the 2026-07-06 P0 INV-02 flow:
  schedule availability input for schedule-coordination rooms and participant
  departure snapshot input for place-coordination rooms.
- INV-02 participation input saves schedule availability for `SCHEDULE_ONLY` and
  `SCHEDULE_AND_PLACE` rooms.
- Schedule participation stores selected 1-hour availability slots within the
  room host's candidate dates and common available time range.
- A participation save request replaces the participant's previous schedule
  availability slots for that room.
- INV-02 participation input saves departure and transportation mode for
  `PLACE_ONLY` and `SCHEDULE_AND_PLACE` rooms.
- Place participation stores the participant departure name, address, latitude,
  longitude, and transportation mode snapshot on `room_participants`.
- INV-02 rejects mismatched input, such as departure input for schedule-only
  rooms or schedule availability input for place-only rooms.

## Deferred Policies

- TODO: After the MVP creation flow is stable, decide whether to remove the
  remaining fixed schedule/place fields and enum values or reintroduce a fixed
  direct-input flow.
- TODO: Host departure address modification is out of the first MVP creation
  scope and should be handled with the later participation/modification flow.
- Address search, GPS/current-location lookup, saved departure lists, and member
  departure CRUD are P1 or later client/domain work; the server only stores the
  selected departure snapshot in this flow.
- Guest re-entry remains deferred until its policy is confirmed.
- Guest modification remains deferred until its policy is confirmed.
- Participant password verification remains deferred until its policy is
  confirmed.
- Member invitation remains deferred until its policy is confirmed.
- Group invitation remains deferred until its policy is confirmed.
- Schedule result logic is not implemented yet. Intersection calculation,
  longest-meeting-time sorting, earliest-date sorting, result recommendation,
  and final decision APIs remain separate follow-up work.
- Actual middle-point calculation remains deferred.
