package com.schwab.registration.service;

import com.schwab.registration.model.*;
import com.schwab.registration.repository.InMemoryEnrollmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

class EnrollmentServiceTest {
    private static final String PREREQUISITE = "JAVA-101";
    private InMemoryEnrollmentRepository repository;
    private EnrollmentService service;

    @BeforeEach
    void setUp() {
        repository = new InMemoryEnrollmentRepository();
        service = new EnrollmentService(
                new Course("JAVA-201", "Core Java", 2, PREREQUISITE), repository
        );
        addEligible("L1");
        addEligible("L2");
        addEligible("L3");
        addEligible("L4");
        addEligible("L5");
    }

    private void addEligible(String id) {
        repository.addLearner(new Learner(id, id, Set.of(PREREQUISITE)));
    }

    @Test
    void enrollsWhenCapacityIsAvailable() {
        RegistrationResult result = service.enroll("L1");

        assertEquals(RegistrationStatus.ENROLLED, result.status());
        assertTrue(service.isEnrolled("L1"));
        assertEquals(1, service.getEnrolledCount());
    }

    @Test
    void addsEligibleLearnerToWaitlistWhenCourseIsFull() {
        service.enroll("L1");
        service.enroll("L2");

        RegistrationResult result = service.enroll("L3");

        assertEquals(RegistrationStatus.WAITLISTED, result.status());
        assertTrue(service.isWaitlisted("L3"));
        assertEquals(2, service.getEnrolledCount());
        assertEquals(1, service.getWaitlistCount());
    }

    @Test
    void duplicateEnrolledAndWaitlistedRequestsDoNotChangeState() {
        service.enroll("L1");
        service.enroll("L2");
        service.enroll("L3");

        assertEquals(RegistrationStatus.DUPLICATE_REQUEST, service.enroll("L1").status());
        assertEquals(RegistrationStatus.DUPLICATE_REQUEST, service.enroll("L3").status());
        assertEquals(2, service.getEnrolledCount());
        assertEquals(List.of("L3"), service.getWaitlistLearnerIds());
    }

    @Test
    void promotesWaitlistInFifoOrder() {
        service.enroll("L1");
        service.enroll("L2");
        service.enroll("L3");
        service.enroll("L4");

        RegistrationResult result = service.drop("L1");

        assertEquals(RegistrationStatus.DROPPED, result.status());
        assertEquals("L3", result.promotedLearnerId().orElseThrow());
        assertTrue(service.isEnrolled("L3"));
        assertTrue(service.isWaitlisted("L4"));
        assertEquals(2, service.getEnrolledCount());
    }

    @Test
    void rejectsLearnerWhoHasNotCompletedPrerequisite() {
        repository.addLearner(new Learner("NO-PREREQ", "No Prerequisite", Set.of()));

        RegistrationResult result = service.enroll("NO-PREREQ");

        assertEquals(RegistrationStatus.PREREQUISITE_NOT_MET, result.status());
        assertFalse(service.isEnrolled("NO-PREREQ"));
        assertFalse(service.isWaitlisted("NO-PREREQ"));
    }

    @Test
    void skipsButRetainsIneligibleLearnerAtFrontOfWaitlist() {
        service.enroll("L1");
        service.enroll("L2");
        service.enroll("L3");
        service.enroll("L4");

        // Simulate a prerequisite status change after L3 joined the waitlist.
        service.updateLearner(new Learner("L3", "L3", Set.of()));
        RegistrationResult result = service.drop("L1");

        assertEquals("L4", result.promotedLearnerId().orElseThrow());
        assertTrue(service.isWaitlisted("L3"));
        assertTrue(service.isEnrolled("L4"));
        assertEquals(List.of("L3"), service.getWaitlistLearnerIds());
    }

    @Test
    void invalidLearnerRequestIsRejected() {
        assertEquals(RegistrationStatus.INVALID_LEARNER, service.enroll("UNKNOWN").status());
        assertEquals(RegistrationStatus.INVALID_LEARNER, service.enroll(" ").status());
        assertEquals(0, service.getEnrolledCount());
        assertEquals(0, service.getWaitlistCount());
    }

    @Test
    void droppingLearnerWhoIsNotEnrolledDoesNotChangeState() {
        service.enroll("L1");

        assertEquals(RegistrationStatus.NOT_ENROLLED, service.drop("L2").status());
        assertEquals(1, service.getEnrolledCount());
        assertTrue(service.isEnrolled("L1"));
    }

    @Test
    void repeatedDropIsRejectedWithoutCorruptingCapacity() {
        service.enroll("L1");

        assertEquals(RegistrationStatus.DROPPED, service.drop("L1").status());
        assertEquals(RegistrationStatus.NOT_ENROLLED, service.drop("L1").status());
        assertEquals(0, service.getEnrolledCount());
    }

    @Test
    void leavesSeatVacantWhenAllWaitlistedLearnersAreIneligible() {
        service.enroll("L1");
        service.enroll("L2");
        service.enroll("L3");
        service.updateLearner(new Learner("L3", "L3", Set.of()));

        RegistrationResult result = service.drop("L1");

        assertTrue(result.promotedLearnerId().isEmpty());
        assertEquals(1, service.getEnrolledCount());
        assertTrue(service.isWaitlisted("L3"));
    }

    @Test
    void concurrentEnrollmentNeverExceedsCapacity() throws Exception {
        int capacity = 5;
        int learnerCount = 30;
        InMemoryEnrollmentRepository concurrentRepository = new InMemoryEnrollmentRepository();
        EnrollmentService concurrentService = new EnrollmentService(
                new Course("JAVA-201", "Core Java", capacity, PREREQUISITE),
                concurrentRepository
        );
        for (int i = 0; i < learnerCount; i++) {
            String id = "C" + i;
            concurrentRepository.addLearner(new Learner(id, id, Set.of(PREREQUISITE)));
        }

        ExecutorService pool = Executors.newFixedThreadPool(10);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<RegistrationResult>> futures = new ArrayList<>();
        try {
            for (int i = 0; i < learnerCount; i++) {
                String id = "C" + i;
                futures.add(pool.submit(() -> {
                    start.await();
                    return concurrentService.enroll(id);
                }));
            }
            start.countDown();
            for (Future<RegistrationResult> future : futures) {
                future.get(5, TimeUnit.SECONDS);
            }
        } finally {
            pool.shutdownNow();
        }

        assertEquals(capacity, concurrentService.getEnrolledCount());
        assertEquals(learnerCount - capacity, concurrentService.getWaitlistCount());
        assertTrue(concurrentService.getEnrolledCount() <= capacity);
    }
}
