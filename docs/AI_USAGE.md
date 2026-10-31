# AI Prompt and Use Record

## Purpose and transparency

ChatGPT was used as an engineering assistant in this assessment conversation. The actual user prompts are recorded below. The generated implementation and documentation are AI-assisted work; the candidate must review every file, run the tests locally, and be prepared to explain and defend the decisions.

## Actual prompts used in this work

### Prompt 1 — Requirement explanation and implementation plan

**User prompt (verbatim):**

> I am a 3 years experience candidate attemtpting charles swap AI assessment. give me proper explainationa nd resolution for above

**AI contribution:** Decomposed the assignment into enrollment, drop, prerequisite, duplicate prevention, waitlist policy, concurrency, tests, design documentation, and AI-use evidence. Proposed Java 17, Maven, JUnit 5, and an in-memory service as a minimal fallback.

**Decision: Accepted with a documented policy choice.** The design uses the assessment-approved in-memory fallback and explicitly chooses skip-and-retain for ineligible waitlisted learners.

### Prompt 2 — Complete implementation request

**User prompt (verbatim):**

> My assessment details: Starter project: not-supplied. Language: Java. Assistance: complete. Help me complete the Charles Schwab AI-assisted course waitlist assessment.

**AI contribution:** Generated the Java project structure, models, in-memory repository, synchronized service operations, JUnit 5 tests, README, design specification, limitations, and this AI-use record.

**Decision: Accepted as a first implementation, subject to candidate review and local verification.** The code was then compiled with Java 17 compatibility and the demonstration application was run. Maven was unavailable in the generation environment, so the JUnit suite has not been executed here.

## Key AI-assisted decisions and candidate review required

| Suggestion / implementation | Decision | Rationale and verification |
|---|---|---|
| Use an insertion-ordered collection for FIFO waitlist uniqueness | Accepted | `LinkedHashSet` retains insertion order and prevents duplicate IDs. Inspect `InMemoryEnrollmentRepository.java`. |
| Recheck prerequisite at promotion | Accepted | Learner eligibility can change while waiting. `skipsButRetainsIneligibleLearnerAtFrontOfWaitlist` exercises this behavior. |
| Skip an ineligible learner but retain their waitlist entry | Accepted | Allows the earliest currently eligible learner to be promoted without deleting the skipped request. This is documented in `DESIGN.md`. |
| Synchronize service operations | Accepted with limitation | Protects operations through one service instance in one JVM only; it is not a distributed lock. |
| Use a minimal in-memory design rather than Spring/database infrastructure | Accepted | Matches the no-starter fallback and keeps scope bounded. |

## Verification evidence

### Initial artifact-generation environment

- Maven was unavailable in the initial artifact-generation environment.
- The JUnit test suite was therefore not executed in that environment.

### Local verification performed by the candidate

After setting up Java and Maven locally, I executed the following commands in the project directory:

| Command | Result |
|---|---|
| `mvn clean test` | BUILD SUCCESS |
| JUnit test execution | 11 tests run, 0 failures, 0 errors, 0 skipped |
| `mvn clean package` | BUILD SUCCESS |
| Demo application execution | Successful |

The demonstration confirmed the following workflow:

1. Two learners were successfully enrolled.
2. A third learner was added to the waitlist because the course was full.
3. An enrolled learner dropped the course.
4. The waitlisted learner was successfully promoted.
5. The final enrolled list contained two learners, and the waitlist was empty.

The candidate reviewed the test results and demonstration output locally.

**Note:** These results verify the current implementation against the supplied tests and demonstration. They do not establish production readiness or distributed concurrency safety.
## Candidate responsibility

AI produced a starting implementation and documentation. The candidate should independently inspect the code, verify all acceptance criteria, execute the automated tests, and be able to explain the algorithms, invariants, concurrency boundary, and limitations. This record must not be presented as evidence that the candidate authored every line without AI assistance.
