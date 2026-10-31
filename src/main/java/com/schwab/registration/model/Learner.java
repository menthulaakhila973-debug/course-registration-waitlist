package com.schwab.registration.model;

import java.util.Objects;
import java.util.Set;

/** Immutable learner profile. Completed course IDs are used for prerequisite checks. */
public record Learner(String learnerId, String name, Set<String> completedCourseIds) {
    public Learner {
        learnerId = requireText(learnerId, "learnerId");
        name = requireText(name, "name");
        Objects.requireNonNull(completedCourseIds, "completedCourseIds must not be null");
        completedCourseIds = Set.copyOf(completedCourseIds);
    }

    public boolean hasCompleted(String courseId) {
        return completedCourseIds.contains(courseId);
    }

    private static String requireText(String value, String field) {
        Objects.requireNonNull(value, field + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value;
    }
}
