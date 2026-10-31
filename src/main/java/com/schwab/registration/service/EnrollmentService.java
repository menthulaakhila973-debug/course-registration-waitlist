package com.schwab.registration.service;

import com.schwab.registration.model.*;
import com.schwab.registration.repository.InMemoryEnrollmentRepository;

import java.util.Iterator;
import java.util.List;
import java.util.Objects;

/**
 * Application service for a single course offering.
 *
 * All state-changing operations are synchronized on this service instance.
 * This is appropriate for this in-memory prototype, not a multi-process deployment.
 */
public final class EnrollmentService {
    private final Course course;
    private final InMemoryEnrollmentRepository repository;

    public EnrollmentService(Course course, InMemoryEnrollmentRepository repository) {
        this.course = Objects.requireNonNull(course, "course must not be null");
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
    }

    public synchronized RegistrationResult enroll(String learnerId) {
        if (learnerId == null || learnerId.isBlank()
                || repository.findLearner(learnerId).isEmpty()) {
            return RegistrationResult.of(
                    RegistrationStatus.INVALID_LEARNER, learnerId, "Learner does not exist."
            );
        }

        if (repository.isEnrolled(learnerId) || repository.isWaitlisted(learnerId)) {
            return RegistrationResult.of(
                    RegistrationStatus.DUPLICATE_REQUEST, learnerId,
                    "Learner is already enrolled or waitlisted."
            );
        }

        if (!isEligible(learnerId)) {
            return RegistrationResult.of(
                    RegistrationStatus.PREREQUISITE_NOT_MET, learnerId,
                    "Required prerequisite has not been completed."
            );
        }

        if (repository.enrolledCount() < course.capacity()) {
            repository.enroll(learnerId);
            return RegistrationResult.of(
                    RegistrationStatus.ENROLLED, learnerId, "Enrollment successful."
            );
        }

        repository.addToWaitlist(learnerId);
        return RegistrationResult.of(
                RegistrationStatus.WAITLISTED, learnerId, "Course is full; learner added to waitlist."
        );
    }

    public synchronized RegistrationResult drop(String learnerId) {
        if (learnerId == null || learnerId.isBlank() || !repository.isEnrolled(learnerId)) {
            return RegistrationResult.of(
                    RegistrationStatus.NOT_ENROLLED, learnerId,
                    "Learner is not currently enrolled."
            );
        }

        repository.removeEnrolled(learnerId);
        String promotedLearnerId = promoteNextEligibleLearner();
        return RegistrationResult.dropped(learnerId, promotedLearnerId);
    }

    /**
     * Updates a learner profile, allowing prerequisite completion to change while
     * a learner is waiting. Eligibility is re-evaluated at promotion time.
     */
    public synchronized void updateLearner(Learner learner) {
        repository.addLearner(learner);
    }

    public synchronized boolean isEnrolled(String learnerId) {
        return repository.isEnrolled(learnerId);
    }

    public synchronized boolean isWaitlisted(String learnerId) {
        return repository.isWaitlisted(learnerId);
    }

    public synchronized int getEnrolledCount() {
        return repository.enrolledCount();
    }

    public synchronized int getWaitlistCount() {
        return repository.waitlistCount();
    }

    public synchronized List<String> getEnrolledLearnerIds() {
        return repository.enrolledInOrder();
    }

    public synchronized List<String> getWaitlistLearnerIds() {
        return repository.waitlistInOrder();
    }

    public Course getCourse() {
        return course;
    }

    private boolean isEligible(String learnerId) {
        return repository.findLearner(learnerId)
                .map(learner -> learner.hasCompleted(course.prerequisiteCourseId()))
                .orElse(false);
    }

    /**
     * Scans in FIFO order. Ineligible entries are skipped for this promotion
     * attempt but remain in their original relative queue order.
     */
    private String promoteNextEligibleLearner() {
        Iterator<String> iterator = repository.waitlistInOrder().iterator();
        while (iterator.hasNext()) {
            String candidateId = iterator.next();
            if (isEligible(candidateId)) {
                repository.removeFromWaitlist(candidateId);
                repository.enroll(candidateId);
                return candidateId;
            }
        }
        return null;
    }
}
