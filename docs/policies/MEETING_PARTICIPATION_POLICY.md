# Meeting Participation Policy

> Purpose: Canonical domain policy for the current meeting creation, invite, guest
> join, and participation input flows.

DOMAIN-001: Undocumented domain behavior must not be implemented as product
policy.

If implementation or review requires a product/domain decision that is not backed
by human-defined policy, classify it as `POLICY_UNDEFINED`. Do not promote Codex
general best practice into domain policy.

## Meeting Creation

- For the first MVP, meeting creation is completed with one final API request after
  the creation steps are filled out.
- Draft or step-by-step meeting creation should be added later as a separate draft
  flow if product policy requires it.
- Meeting creation is selected by `planningType`: `SCHEDULE_ONLY`, `PLACE_ONLY`, or
  `SCHEDULE_AND_PLACE`.
- Schedule mode is currently stored as `VOTE`, `FIXED`, or `NONE`, but current
  meeting creation derives it from `planningType` and does not accept fixed schedule
  input.
- Place mode is currently stored as `FIXED`, `RECOMMEND`, or `NONE`, but current
  meeting creation derives it from `planningType` and does not accept fixed place
  input.
- Fixed schedule/place direct input is excluded from the current MVP creation
  flow and may be reconsidered in a later product discussion.
- Place recommendation strategy is separated from place mode. The first MVP
  keeps the meeting creation strategy fixed after creation; later place
  recommendation/finalization flow may revisit switching between middle-point and
  random recommendations.
- Middle-point meeting creation stores the host departure name, address, and transportation mode snapshot on the host `meeting_participants` row. Until the coordinate API key is approved, latitude and longitude may both be omitted as a temporary development policy; fabricated coordinates must not be stored.
- A participant whose coordinate pair is omitted counts as having submitted a departure snapshot, but is excluded from the straight-line middle-point preview. If no submitted departure has coordinates, the place view returns `COORDINATES_PENDING` with no recommendations.
- Meeting creation receives `deadlineMinutes`; the server calculates and returns
  `deadlineAt`.
- The meeting-creation success response is intentionally minimal for the CRT-08
  link-sharing flow: it returns only `meetingId`, `inviteCode`, and the existing
  `invitePath`. Meeting details, cover URL, and derived planning data are read
  through the invite-code lookup API after creation when needed.
- `deadlineMinutes` is currently accepted in 10-minute units from 10 minutes up
  to 72 hours. A zero-minute deadline is not allowed.
- `deadlineAt` is calculated from the server processing time of the final meeting
  creation request. Any client-side expected end time is only a preview and may
  differ if the user stays on the screen before submitting.
- TODO: For CRT-06, keep the current duration-only selection UX until product
  confirmation. If an absolute deadline preview or second-accurate countdown is
  later required, decide whether the API should provide a server-time reference;
  the server-calculated `deadlineAt` remains authoritative after creation.
- A meeting cover image is optional and is used only as a meeting-home thumbnail.
  The server stores one resized cover derivative in durable object storage; the
  uploaded original is not retained. The resized derivative is part of the
  long-term meeting record.
- Temporary MVP policy: the client keeps a selected cover file locally until the
  final meeting-creation request. That request may use multipart form data with
  the existing meeting JSON and an optional cover file; the response returns the
  created invite code. The server does not create temporary upload objects.
- Temporary MVP policy: the host may later replace or delete the cover through a
  dedicated authenticated API. The invite-link meeting view may read the cover.
  If the selected cover cannot be stored, the final meeting creation fails and
  no invite code is returned.
- The cover-image response URL includes a value derived from the current stored
  object key only to distinguish browser cache entries. It is not a new database
  field or object-storage path. When the host replaces the cover, clients use
  the returned new URL; the former S3 object is deleted only after the database
  transaction commits successfully.
- Temporary technical settings: JPEG/PNG input, 5 MB maximum upload size, a
  1280x720 bounding box, and JPEG quality 0.85. They are configuration values,
  not final product policy; revisit format, size, crop, compression, visibility,
  and deletion retention after MVP feedback.
- TODO: Before broadening cover-image upload use, add a configuration-backed
  maximum decoded input width/height or pixel count. The current 5 MB encoded
  file-size limit and 1280x720 output bound do not limit image decode memory.
- TODO: Add negative-path cover-image tests: non-host modification rejection,
  invalid/oversized file rejection, storage-unavailable response, and S3 cleanup
  after a transaction rollback.
- Schedule voting candidate dates are stored as separate rows and are temporarily
  limited to 21 dates by request validation until the final 3-week policy is
  confirmed. Keep the limit isolated in the meeting creation constraints so it can
  be changed without reshaping the API.
- Schedule voting applies the same available time range to every selected
  candidate date.
- Schedule voting time ranges are currently accepted in 1-hour units.
- For schedule-coordination meetings, the candidate dates and common available
  time range selected by the host at creation are also the host's schedule
  availability. The server saves one host availability row per candidate date in
  the same creation transaction.

## Meeting Participant Identity

- Meeting participant nickname duplication is checked only among guest
  participants inside the same meeting. Host/member nicknames may overlap with
  guest nicknames and with each other.
- A service user should not be linked to the same meeting more than once; enforce
  this with a meeting-scoped uniqueness rule such as `unique(meeting_id, user_id)`.
- Guest participants currently have nullable `user_id`, so multiple guest
  participants remain allowed.
- Current participant behavior is defined for `HOST`, `MEMBER`, and `GUEST`.

## Invite and Guest Join

- INV-01 invite entry is currently implemented through invite-code lookup. It
  returns the current participation availability status for the entry screen.
- If both the deadline and participant limit block joining, the deadline-passed
  status takes priority in the entry response.
- Web invite-link entry supports both logged-in member join and guest join.
- App invite-link entry supports logged-in member join only; guest join is not
  exposed in the app entry flow.
- The client decides which entry options to expose by platform. The server keeps
  separate member and guest join APIs.
- INV-01 member join creates a participant row for the current authenticated
  service user from `@CurrentMember` and saves the required participation
  details in the same request and transaction.
- Member join accepts a meeting-scoped nickname; the nickname may differ from the
  user's default nickname. The authenticated member is identified by the Bearer
  Access Token and does not submit a participant password.
- A service user can participate in the same meeting only once. The host
  participant row also counts as that user's meeting participation.
- INV-01 guest join creates the participant row and saves the required
  participation details in the same request and transaction.
- Member and guest join requests include schedule availability for
  `SCHEDULE_ONLY` and `SCHEDULE_AND_PLACE`, and include the departure snapshot
  and transportation mode for `PLACE_ONLY` and `SCHEDULE_AND_PLACE`.
- Guest join stores the participant password as a hash on the
  `meeting_participants` row. Guest password verification for later re-entry or
  modification remains deferred until its policy is confirmed.
- A repeated guest join attempt with the same nickname as an existing guest in
  the same meeting should continue to return a duplicate nickname conflict, even if
  the same password is provided.
- Member and guest participation is rejected after the meeting `deadlineAt`.
- Member and guest participation checks the current participant count before
  saving.
- To prevent concurrent joins from exceeding `maxParticipants`, participant
  participation may acquire a pessimistic write lock on the target meeting row
  during the join transaction.
- Keep this lock limited to the meeting join path; ordinary invite-code lookup
  should remain read-only.

## Participation Input

- The first meeting participation expansion covers the 2026-07-06 P0 INV-02 data:
  schedule availability for schedule-coordination meetings and participant
  departure snapshots for place-coordination meetings. These details are submitted
  together with INV-01 member or guest join; there is no separate P0
  participation-save API.
- Join requests save schedule availability for `SCHEDULE_ONLY` and
  `SCHEDULE_AND_PLACE` meetings.
- Schedule participation stores selected availability ranges within the meeting
  host's candidate dates and common available time range. Each range start and
  end time must be in 1-hour units, and multiple ranges may be saved.
- A join request creates the participant and their initial availability slots
  atomically.
- Join requests save departure and transportation mode for
  `PLACE_ONLY` and `SCHEDULE_AND_PLACE` meetings.
- Place participation stores the participant departure name, address, latitude, longitude, and transportation mode snapshot on `meeting_participants`. Until the coordinate API key is approved, latitude and longitude may both be omitted; one without the other is invalid.
- Join rejects mismatched input, such as departure input for schedule-only
  meetings or schedule availability input for place-only meetings.

## Pre-confirmation Meeting View

- VIEW-01 meeting status, schedule status, and place status are read-only
  pre-confirmation views.
- VIEW-01 APIs may be opened from an invite link without login. Authentication
  is not required for the current read-only status APIs.
- VIEW-01 status values are calculated at read time from the current meeting,
  participant, schedule availability, and departure snapshot rows.
- A participant is counted as response-complete only when all inputs required by
  the meeting mode are present. Schedule coordination requires at least one saved
  schedule availability. Place coordination requires a saved departure snapshot.
- VIEW-01-A expands saved availability ranges into 1-hour units before
  calculating availability. Consecutive units with the same available
  participant set are merged into one candidate. The first P0 implementation
  returns up to five candidates sorted by longest-meeting preference or
  earliest-date preference.
- VIEW-01-A accepts only `LONGEST_MEETING` and `EARLIEST_DATE` as the schedule
  sorting value. Unsupported values return the common invalid-request error.
- VIEW-01-B place recommendations before final confirmation do not call
  external travel-time APIs.
- Before final confirmation, middle-point place recommendation is a preview
  based on simple latitude/longitude distance from saved participant departure
  snapshots.
- Before final confirmation, commercial-area candidates may use a temporary
  in-memory catalog derived from the Seoul commercial-area CSV. Building a
  persistent commercial-area table and import pipeline is deferred until the
  place data policy is confirmed.
- The `RANDOM` place recommendation strategy shuffles the temporary catalog for
  each view request before selecting up to five candidates.
- Actual travel-time based reranking and final place result storage should be
  handled in the later final-confirmation flow, not on every pre-confirmation
  status view request.

## Deferred Policies

- TODO: After the MVP creation flow is stable, decide whether to remove the
  remaining fixed schedule/place fields and enum values or reintroduce a fixed
  direct-input flow.
- TODO: Host departure address modification is out of the first MVP creation
  scope and should be handled with the later participation/modification flow.
- Address search uses the road-name address search API through the server. The API key remains server-side, and the search response intentionally contains no coordinates until the separately requested coordinate API key is approved.
- TODO: Evaluate adding a server-side POI/place-name search provider for departure
  input (for example, Kakao Local keyword search). Do not expose a provider key
  to the client or alter the current road-name address API contract before the
  provider, quota/cost policy, and unified result contract are confirmed.
- GPS/current-location lookup, saved departure lists, and member departure CRUD are P1 or later client/domain work.
- Guest re-entry remains deferred until its policy is confirmed.
- Guest modification remains deferred until its policy is confirmed.
- Participant password verification for re-entry or modification remains
  deferred until its policy is confirmed.
- Member invitation beyond direct invite-link join remains deferred until its
  policy is confirmed.
- Group invitation remains deferred until its policy is confirmed.
- Final schedule decision APIs remain separate follow-up work.
- Actual travel-time based middle-point calculation remains deferred.
- Persistent commercial-area data import, external travel-time API integration,
  and final place confirmation policy remain deferred.
