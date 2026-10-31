package com.schwab.registration.model;

import java.util.Objects;

/** One course offering with a fixed enrollment capacity and one prerequisite. */
public record Course(String courseId, String title, int capacity, String prerequisiteCourseId) {
    public Course {
        courseId = requireText(courseId, "courseId");
        title = requireText(title, "title");
        if (capacity < 1) {
            throw new IllegalArgumentException("capacity must be at least 1");
        }
        prerequisiteCourseId = requireText(prerequisiteCourseId, "prerequisiteCourseId");
    }

    private static String requireText(String value, String field) {
        Objects.requireNonNull(value, field + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value;
    }
}
