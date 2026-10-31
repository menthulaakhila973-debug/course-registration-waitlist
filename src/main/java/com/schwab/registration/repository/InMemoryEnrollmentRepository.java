package com.schwab.registration.repository;

import com.schwab.registration.model.Learner;

import java.util.*;

/**
 * In-memory state for one course offering. The service owns synchronization;
 * callers should access this repository only through EnrollmentService.
 */
public final class InMemoryEnrollmentRepository {
    private final Map<String, Learner> learners = new HashMap<>();
    private final LinkedHashSet<String> enrolledLearnerIds = new LinkedHashSet<>();
    private final LinkedHashSet<String> waitlistedLearnerIds = new LinkedHashSet<>();

    public void addLearner(Learner learner) {
        Objects.requireNonNull(learner, "learner must not be null");
        learners.put(learner.learnerId(), learner);
    }

    public Optional<Learner> findLearner(String learnerId) {
        return Optional.ofNullable(learners.get(learnerId));
    }

    public void enroll(String learnerId) {
        enrolledLearnerIds.add(learnerId);
    }

    public boolean removeEnrolled(String learnerId) {
        return enrolledLearnerIds.remove(learnerId);
    }

    public boolean isEnrolled(String learnerId) {
        return enrolledLearnerIds.contains(learnerId);
    }

    public boolean isWaitlisted(String learnerId) {
        return waitlistedLearnerIds.contains(learnerId);
    }

    public void addToWaitlist(String learnerId) {
        waitlistedLearnerIds.add(learnerId);
    }

    public void removeFromWaitlist(String learnerId) {
        waitlistedLearnerIds.remove(learnerId);
    }

    public List<String> waitlistInOrder() {
        return List.copyOf(waitlistedLearnerIds);
    }

    public List<String> enrolledInOrder() {
        return List.copyOf(enrolledLearnerIds);
    }

    public int enrolledCount() {
        return enrolledLearnerIds.size();
    }

    public int waitlistCount() {
        return waitlistedLearnerIds.size();
    }
}
