# Backend AI Code Review

## Purpose

AI review is a pre-filter.
The human developer owns the final decision.

The project is currently in an initial MVP phase but is intended for continued
development and operation.

Review must avoid both:

- speculative future architecture concerns
- concrete disposable MVP shortcuts that create unnecessary debt against
  confirmed product direction

## Review Scope

Review:

- changed code
- directly affected code
- applicable project policies

Do not review the entire repository unless explicitly requested.

## Review Categories

Use:

- `POLICY_VIOLATION`
- `POLICY_UNDEFINED`
- `CONCRETE_RISK`
- `DOCUMENTATION_MISMATCH`
- `TEST_GAP`

Definitions:

`POLICY_VIOLATION`: 실제 documented policy와 code가 충돌하는 경우만 사용.

`POLICY_UNDEFINED`: code가 product/domain behavior를 결정하지만 이를
뒷받침하는 human-defined policy가 없는 경우.

`CONCRETE_RISK`: explicit policy violation은 아니지만 current code, current
policy, or confirmed product direction에서 실제 failure/debt risk가 확인되는
경우.

`DOCUMENTATION_MISMATCH`: Swagger, DBML, policy document와 code가 실제로
불일치하는 경우.

`TEST_GAP`: high-risk behavior가 변경되었지만 해당 위험을 검증하는 test가
없는 경우.

## MVP and Post-MVP Review Principle

Do not report:

- hypothetical high traffic concerns
- speculative future scale
- technology recommendations only for later growth
- "this may need Redis/Kafka/microservices later" style suggestions

Future possibility alone is not evidence.

However, `CONCRETE_RISK` may be reported when:

- implementation creates an obviously disposable data model that conflicts with
  confirmed product direction
- API contract becomes unnecessarily difficult to evolve based on already
  confirmed flows
- a temporary MVP behavior creates migration or operational debt but is not
  documented as temporary
- dev-only behavior is being relied on as if it were a permanent runtime policy
- current implementation creates a known migration problem based on an already
  documented roadmap or policy

A finding must include evidence from current code, current policy, or confirmed
product direction.

Do not suggest architecture improvement only because the project may continue
after MVP.

## Review Priority

HIGH:

- authentication or authorization bypass
- data corruption
- duplicate processing
- broken API contract
- irreversible schema risk
- participant limit concurrency failure

MEDIUM:

- transaction boundary risk
- validation gap
- error contract inconsistency
- undocumented domain behavior
- relationship or unique constraint integrity risk
- Swagger or DBML mismatch affecting collaboration
- concrete temporary implementation debt against confirmed product direction

LOW:

- maintainability risk only when concrete evidence exists

## Review Heuristics

Heuristic is not policy.

Review perspectives:

- network/external calls inside transactions
- duplicate processing risk
- concurrency around participant limit
- nullable relation handling
- enum branch completeness
- magic numbers acting as product policy
- undocumented scoring or threshold behavior
- request replacement semantics
- planningType-dependent validation
- API/error contract consistency
- Swagger mismatch
- JPA/DBML mismatch
- undocumented temporary MVP behavior
- disposable implementation conflicting with confirmed product direction

Do not treat a heuristic match itself as `POLICY_VIOLATION`.

## Ignore Rules

Do not report a finding without concrete risk or explicit policy support:

- personal naming preference
- speculative abstraction
- alternative design pattern suggestions
- generic best practices
- cosmetic refactoring
- formatting issues
- hypothetical scale concerns
- future high-traffic concerns
- Redis/Kafka/event architecture recommendations
- microservice recommendations
- infrastructure recommendations based only on post-MVP possibility

If there are no actionable findings, do not invent suggestions. Report:

```text
No actionable findings.
```

## Finding Format

Every actionable finding must use:

```text
Severity:
Category:
File:
Location:
Policy:
Evidence:
Concrete Risk:
Confidence:
```

If no documented policy exists, use:

```text
Policy: POLICY_UNDEFINED
```

For findings that are not explicit policy violations, do not invent a policy ID.

Confidence values:

- HIGH
- MEDIUM
- LOW

Do not report a LOW confidence finding as HIGH severity without clear evidence.

## Human Decision

Each finding is later judged by the human developer as one of:

- ACCEPTED
- REJECTED
- DEFERRED

AI does not automatically mark its own findings as ACCEPTED.

## Pre-Commit Self Review

Before creating a commit, run this review against the staged and unstaged diff:

```text
Act as a careful backend code reviewer for the Moyeo project before commit.

Review only the current diff, directly affected code, and applicable project
policies. First classify the touched change surfaces: API contract, error
contract, schema contract, auth/security contract, deployment/runtime contract,
and domain policy. Read the applicable canonical policy documents before making
findings.

Prioritize concrete bugs, regressions, policy violations, missing tests, and
documentation mismatches. Check especially for API/error contract drift,
Swagger/OpenAPI mismatch, JPA/DBML mismatch, auth/security behavior changes,
transaction or concurrency risks, duplicate processing, nullable relation
handling, enum branch completeness, request replacement semantics, and
planningType-dependent validation.

Do not report personal preference, cosmetic refactoring, speculative
architecture, hypothetical scale concerns, Redis/Kafka/microservice
recommendations, or generic best practices without concrete project evidence.
Do not invent product/domain policy. If a finding depends on missing
human-defined policy, classify it as POLICY_UNDEFINED.

For each actionable finding, use the Finding Format from this document. If no
actionable findings exist, report exactly:

No actionable findings.
```

## Policy, Heuristic, and Known Risk

`POLICY`: Human-defined project, product, or technical rule.

`REVIEW HEURISTIC`: AI review perspective. A heuristic match is not
automatically a violation.

`KNOWN RISK`: A risk actually observed in this project or clearly supported by
current code evidence.

Do not create historical incidents as `KNOWN RISK` when they are not present in
the current documentation.

Do not promote Codex general best practice or personal preference into `POLICY`.

Do not treat the fact that Moyeo is intended for continued operation after MVP as
a `KNOWN RISK` by itself.

## Manual Harness Health Check

Use this manually after significant harness, policy, or review-rule changes:

```text
Run a Moyeo harness health check.

Read AGENTS.md, docs/00-project-setup.md, docs/policies/*, docs/ai/*, and
docs/01-dbdiagram.md only for schema-reference consistency. Do not modify
application code. Report only actionable routing drift, duplicated policy,
policy mismatch, REVIEW_LOG-worthy feedback, or over-automation risk. Do not
invent product/domain policy, fake review findings, fake metrics, or fake known
risks. If there are no actionable findings, report exactly:

No actionable findings.
```
