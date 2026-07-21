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
- Place recommendation strategy is separated from place mode. For the current
  MVP creation flow, the server always stores `MIDDLE_POINT` when place mode is
  `RECOMMEND`; clients do not send a strategy. The stored strategy and enum are
  retained so a later product decision can add another strategy without changing
  the meeting schema.
- Meeting creation creates the host `meeting_participants` row and saves every
  host input required by the selected planning flow in the same transaction.
- Meeting creation receives `scheduleCandidateDates` for schedule coordination,
  `scheduleResponse.availableTimeRanges` for `DATE_AND_TIME`, and `departure` for
  place coordination. `SCHEDULE_ONLY` and `SCHEDULE_AND_PLACE` also receive the explicit
  `scheduleInputType` selected in CRT rather than inferring it from nullable time
  fields.
- `PLACE_ONLY` omits `scheduleInputType` and the common time range.
  `SCHEDULE_ONLY` and `SCHEDULE_AND_PLACE` require either `DATE_ONLY` or
  `DATE_AND_TIME`. `DATE_ONLY` omits the common time range; `DATE_AND_TIME`
  requires both `availableStartTime` and `availableEndTime`.
- A participant whose coordinate pair is omitted counts as having submitted a departure snapshot, but is excluded from the straight-line middle-point preview. If no submitted departure has coordinates, the place view returns `COORDINATES_PENDING` with no recommendations.
- Meeting creation receives `deadlineMinutes`; the server calculates and stores
  `deadlineAt`.
- The meeting-creation success response returns `meetingId`, `inviteCode`, and
  `invitePath` so the client can move directly to the link-sharing screen.
- The `meetup.app` domain shown in the CRT-08 design is an illustration only;
  it is not a configured Moyeo domain or an API contract. The frontend composes
  the final share URL from its deployed domain and `invitePath`.
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
  same `meetingId`, `inviteCode`, and `invitePath` as JSON creation. The server does
  not create temporary upload objects.
- Temporary MVP policy: the host may later replace or delete the cover through a
  dedicated authenticated API. The invite-link meeting view may read the cover.
  If the selected cover cannot be stored, the final meeting creation fails.
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
- Schedule voting candidate dates are stored as separate rows during meeting
  creation. The candidate date range and count limit are deferred until product
  policy is confirmed; the current meeting creation request does
  not impose a date-count limit.
- `DATE_AND_TIME` applies the same available time range to every selected
  candidate date. Its common range and participant ranges are currently accepted
  in 1-hour units.
- `DATE_ONLY` stores participant selections in
  `meeting_participant_schedule_date_availabilities`; it does not create artificial
  start/end times.
- For schedule-coordination meetings, only the host selects the meeting's candidate
  dates. For `DATE_ONLY`, those candidate dates are also the host's available dates.
  For `DATE_AND_TIME`, the host selects their own available ranges within those
  dates and the common time range established during creation. Ordinary
  participants cannot add or replace candidate dates.

## Host Participation During Creation

- The authenticated creator submits the meeting settings and their own
  participation input in one final creation request.
- Schedule-coordination creation always receives
  `scheduleCandidateDates`. `DATE_ONLY` omits `scheduleResponse`, because the
  candidate dates are stored as the host's available dates.
  `DATE_AND_TIME` additionally receives
  `scheduleResponse.availableTimeRanges`. Place-coordination creation
  receives the same departure snapshot shape used by other participants.
- Meeting, host participant, candidate dates, host schedule availability, and host
  departure are saved in one transaction. Invalid host input must leave none of
  those rows behind.
- There is no separate post-creation host participation API. A successful creation
  returns `meetingId`, `inviteCode`, and `invitePath` for the link-sharing screen.

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

- TODO (invite-code collision): The current 10-character invite code uses a
  large random space and the database unique constraint remains the final
  safeguard. Before collision probability becomes operationally relevant,
  define a bounded retry policy for meeting creation when that constraint is
  violated, so a rare collision does not fail the user-visible creation flow.
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
- Member and guest join requests include a nested `scheduleResponse` for
  `SCHEDULE_ONLY` and `SCHEDULE_AND_PLACE`. It contains `availableDates` for
  `DATE_ONLY` or `availableTimeRanges` for `DATE_AND_TIME`, never both. They also
  include the departure snapshot and transportation mode for `PLACE_ONLY` and
  `SCHEDULE_AND_PLACE`.
- Guest join stores the participant password as a hash on the
  `meeting_participants` row. Guest password verification for later re-entry or
  modification remains deferred until its policy is confirmed.
- Guest join does not issue an Access JWT or a guest JWT. Authentication for a
  later guest re-entry or modification flow remains deferred until that flow's
  policy is confirmed.
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
  together with INV-01 member or guest join. The host submits the corresponding
  inputs as part of meeting creation.
- Join requests save schedule availability for `SCHEDULE_ONLY` and
  `SCHEDULE_AND_PLACE` meetings.
- `DATE_ONLY` participation stores one selection per available host candidate date.
  `DATE_AND_TIME` participation stores selected availability ranges within the
  host's candidate dates and common available time range. Each range start and end
  time must be in 1-hour units, and multiple ranges may be saved.
- A join request creates the participant and their initial availability slots
  atomically.
- Join requests save departure and transportation mode for
  `PLACE_ONLY` and `SCHEDULE_AND_PLACE` meetings.
- Place participation stores the participant departure name, address, latitude, longitude, and transportation mode snapshot on `meeting_participants`. A client using departure-place search sends the selected candidate's WGS84 coordinate pair. A legacy request may omit both coordinates; one without the other is invalid.
- Join rejects mismatched input, such as departure input for schedule-only
  meetings or schedule availability input for place-only meetings.

## Pre-confirmation Meeting View

- VIEW-01 meeting status, schedule status, and place status are read-only
  pre-confirmation views.
- VIEW-01 APIs may be opened from an invite link without login. Authentication
  is not required for the current read-only status APIs.
- VIEW-01 status values are calculated at read time from the current meeting,
  participant, schedule availability, and departure snapshot rows.
- Meeting creation and participant join save every input required by the meeting
  mode in the same transaction. A participant row is not retained when required
  schedule or departure input is invalid, so status views do not expose separate
  response-progress counts, rates, or per-participant completion flags.
- For `DATE_ONLY`, VIEW-01-A aggregates availability by date and returns null
  `startTime`/`endTime`. For `DATE_AND_TIME`, it expands saved ranges into 1-hour
  units before calculating availability, then merges consecutive units with the
  same available participant set. The first P0 implementation returns up to three
  candidates sorted by availability count/longest-meeting preference or
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

## Departure Place Search

- Departure input uses server-side integrated search APIs backed by Kakao Local.
  The Kakao REST API key remains server-side.
- Logged-in members and web guests use the same search endpoint. A valid Access
  JWT authorizes member access. Only when the `Authorization` header is absent,
  a valid invite code authorizes guest access; the server validates that the
  invite code belongs to an existing meeting before calling Kakao Local. A
  present invalid token never falls back to invite-code access, and a valid
  token takes precedence when both credentials are supplied.
- The integrated search API accepts a single keyword and returns unified
  `STATION`, `ADDRESS`, or `PLACE` candidates with a display name, representative
  address, road-name address, and lot-number address. It does not expose provider
  response shapes or provider-specific result IDs.
- The candidate `displayName` is only for the search list. The final departure
  snapshot `name` remains a client-provided user label subject to its existing
  30-character limit.
- A keyword ending exactly in `역` first uses Kakao keyword search with the
  `SW8` subway-station category. Keep only results whose place name is the
  requested station name or starts with it followed by whitespace or an opening
  parenthesis. If that filtered list is empty, use unfiltered Kakao keyword
  search as a fallback.
- A conservative road-name or lot-number address pattern first uses Kakao address
  search with similar matching. Strong lot-number input recognizes `동`, `리`, or
  numbered `가` followed by a lot number, with optional `산`, sub-number, and
  `번지`. Region-only terms, road names without a building number, underground
  road addresses, and incomplete `읍` or `면` input use Kakao keyword search.
  Address search keeps only complete `REGION_ADDR` or `ROAD_ADDR` documents;
  partial `REGION` or `ROAD` documents do not count as final candidates. If a
  successful address search returns no complete candidates, use Kakao keyword
  search as a fallback. All other keywords use Kakao keyword search directly.
- A fallback is allowed only after a successful search with no final candidates.
  Provider configuration, authorization, quota, network, or response failures
  return `DEPARTURE_PLACE_SEARCH_UNAVAILABLE` and do not trigger a fallback.
- Search candidates include the provider document's top-level WGS84 `y` and `x`
  values as `latitude` and `longitude`. The client passes the selected pair into
  the existing departure snapshot fields and must not geocode the address again
  when saving. The existing coordinate-pair validation remains unchanged.
- Persist a search only after the provider search completes successfully. Do not
  create search-history rows for request validation, authentication, invitation
  validation, provider configuration, authorization, quota, network, or response
  failures.
- A persisted search stores the normalized keyword sent to Kakao Local, the
  provider, the actual primary/fallback execution path, and only the final unified
  candidates returned to the client. Do not retain discarded primary documents,
  raw provider response JSON, provider credentials, request URIs, or
  provider-specific result IDs.
- A successful final search with no candidates stores the search execution with
  an empty candidate collection. Candidate positions preserve the client response
  order starting at 1.
- Search-history persistence is supplementary to the user-facing search. If the
  provider search succeeds but history persistence fails, return the successful
  search response and write an internal error log without exposing persistence
  details to the client.
- Member searches are linked to the authenticated service user. Invite-code guest
  searches are linked to the validated meeting because a guest participant may
  not exist yet; do not store the invite code as search history.
- TODO: Confirm the search-history retention/deletion period and Kakao Local data
  retention requirements before retaining real-user search history beyond MVP
  development.

## Deferred Policies

- TODO: After the MVP creation flow is stable, decide whether to remove the
  remaining fixed schedule/place fields and enum values or reintroduce a fixed
  direct-input flow.
- TODO: Host departure modification after initial meeting creation remains
  deferred until the modification policy is confirmed.
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
