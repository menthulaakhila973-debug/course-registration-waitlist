# Design Specification

## 1. Objective and scope

Implement the enrollment, drop, and FIFO waitlist-promotion workflow for one course offering. This is a bounded service slice, not a complete registration platform. No UI, catalog, payments, identity provider, multi-term support, database, or deployment is included.

## 2. Service boundary

`EnrollmentService` owns the business rules for one `Course`. It delegates in-memory state storage to `InMemoryEnrollmentRepository`. The model package contains immutable value records and explicit result statuses.

The public commands are:
- `enroll(learnerId)`
- `drop(learnerId)`
- `updateLearner(learner)` to reflect changed learner prerequisite data
- Read-only queries for current enrollment and waitlist state.

## 3. Data/API design

- `Course`: course ID, title, positive capacity, required prerequisite course ID.
- `Learner`: learner ID, name, immutable set of completed course IDs.
- Repository: learner lookup, insertion-ordered enrolled IDs, insertion-ordered waitlisted IDs.
- `RegistrationResult`: status, learner ID, optional promoted learner ID, human-readable message.
- `RegistrationStatus`: `ENROLLED`, `WAITLISTED`, `DUPLICATE_REQUEST`, `INVALID_LEARNER`, `PREREQUISITE_NOT_MET`, `DROPPED`, `NOT_ENROLLED`.

The in-memory repository uses a `HashMap` for learner lookup and `LinkedHashSet` for enrolled and waitlisted IDs. The latter preserves insertion order and prevents duplicate IDs.

## 4. Assumptions and eligibility policy

1. One `EnrollmentService` instance represents exactly one offering.
2. The course has exactly one required prerequisite course ID.
3. A learner is eligible when their current profile lists that prerequisite as completed.
4. Eligibility is checked when enrolling or joining the waitlist and checked again immediately before promotion.
5. If an ineligible learner is encountered during promotion, the learner is skipped for that seat and retained in the waitlist in the same relative position. The scan continues to the next learner.
6. A learner can be enrolled or waitlisted at most once at a time.
7. Dropping is supported only for enrolled learners. Waitlist cancellation is outside scope.
8. An unknown or blank learner ID is invalid.
9. There is no automatic re-enrollment of a previously dropped learner; a new explicit request is required.

## 5. Business invariants

- `0 <= enrolledCount <= course.capacity`.
- A learner ID cannot be present in both enrolled and waitlisted collections.
- A learner ID appears at most once in either collection.
- Only learners satisfying the prerequisite may be enrolled.
- A newly accepted waitlist entry was eligible at the time of the request.
- Promotion chooses the earliest currently eligible learner in FIFO order.
- A successful drop removes exactly one enrolled learner.
- A rejected request does not mutate enrollment or waitlist state.

## 6. Enrollment algorithm

1. Reject null, blank, or unknown learner IDs.
2. Reject if learner is already enrolled or waitlisted.
3. Reject if prerequisite is not satisfied.
4. If enrolled count is below capacity, enroll.
5. Otherwise append learner to waitlist.

## 7. Drop and promotion algorithm

1. Reject if learner is not currently enrolled.
2. Remove the learner from enrolled state.
3. Scan a snapshot of waitlist IDs in insertion order.
4. For each candidate, re-read current learner data and check prerequisite.
5. Skip candidates who are no longer eligible without removing them.
6. Remove and enroll the first eligible candidate; stop scanning.
7. If no candidate is eligible, leave the seat vacant.

The scan uses a snapshot so that removal from the repository does not invalidate the iterator.

## 8. Concurrency and persistence

All service operations that read or mutate registration state are synchronized on the same service instance. This serializes enrollment, drop, profile updates, and state reads within one JVM and prevents two requests from claiming the same final seat.

This is intentionally an in-memory prototype. `synchronized` does not coordinate multiple service instances or application processes. A persistent implementation would need a transaction and an appropriate row lock/optimistic concurrency strategy, plus database uniqueness constraints for active learner/offering registrations. A concurrency test checks simultaneous enrollment against one service instance.

## 9. Trade-offs

- In-memory state is simple and matches the fallback requirement, but is not durable.
- One prerequisite keeps the policy explicit and proportionate; no configurable rules engine is introduced.
- Skipping and retaining ineligible waitlisted learners avoids blocking eligible learners while preserving the original request. The seat remains vacant if nobody is currently eligible.
- `LinkedHashSet` provides uniqueness and insertion order for this single-process prototype.
