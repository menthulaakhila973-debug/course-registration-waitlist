# Course Registration: Enrollment and FIFO Waitlist

A deliberately small Java 17/Maven implementation of enrollment, dropping, and waitlist promotion for **one course offering**. It uses an in-memory repository and has no frontend, database, payment, identity, or deployment components.

## Requirements

- JDK 17 or newer
- Maven 3.8+

Check your environment:

```bash
java -version
mvn -version
```

## Build, test, and run

From the project root:

```bash
mvn clean test
mvn clean package
java -cp target/classes com.schwab.registration.Application
```

The `Application` class is a small command-line demonstration. The automated tests are the primary acceptance evidence.

## Main behavior

- An eligible learner is enrolled if a seat is available.
- If full, an eligible learner is added once to the FIFO waitlist.
- A learner already enrolled or waiting cannot create another active request.
- A drop is accepted only for an enrolled learner.
- After a drop, the service scans the waitlist from oldest to newest and promotes the first learner who still satisfies the prerequisite.
- Ineligible learners are skipped for that promotion attempt but remain in their original relative queue order.
- Enrollment and drop operations are synchronized on the service instance.

## Assumptions

1. One service instance represents one course offering.
2. The course has one required prerequisite course ID.
3. A learner is eligible when their completed-course set contains that prerequisite ID.
4. Learner profiles may be updated while someone is waiting; eligibility is rechecked at promotion time.
5. A learner may have only one active state for this offering: enrolled, waitlisted, or neither.
6. Dropping removes an enrolled registration. There is no separate waitlist-cancellation operation because it is outside the requested slice.
7. An ineligible waitlisted learner is skipped but retained. The service does not silently delete their request.
8. In-memory state is lost when the process stops.

## Project layout

```text
src/main/java/com/schwab/registration/
  Application.java
  model/
    Course.java
    Learner.java
    RegistrationResult.java
    RegistrationStatus.java
  repository/
    InMemoryEnrollmentRepository.java
  service/
    EnrollmentService.java
src/test/java/com/schwab/registration/service/
  EnrollmentServiceTest.java
docs/
  DESIGN.md
  AI_USAGE.md
  LIMITATIONS.md
```

## API surface

- `EnrollmentService.enroll(learnerId)`
- `EnrollmentService.drop(learnerId)`
- `EnrollmentService.updateLearner(learner)` (profile update used to model changed prerequisite completion)
- Read-only state queries for enrolled and waitlisted learners.

`RegistrationResult` returns a clear status and message. On a successful drop, it also reports the promoted learner ID, if any.

## Verification checklist

Run `mvn clean test` and confirm all tests pass. Tests cover available capacity, full capacity, duplicates, FIFO promotion, prerequisite rejection, changed eligibility at promotion, invalid learners, invalid/repeated drops, no eligible promotion, and concurrent enrollment.
