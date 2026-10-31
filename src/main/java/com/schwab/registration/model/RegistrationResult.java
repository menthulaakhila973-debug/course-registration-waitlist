package com.schwab.registration.model;

import java.util.Optional;

/** Result of an enrollment or drop command. promotedLearnerId is populated on promotion. */
public record RegistrationResult(RegistrationStatus status, String learnerId,
                                 Optional<String> promotedLearnerId, String message) {
    public RegistrationResult {
        promotedLearnerId = promotedLearnerId == null ? Optional.empty() : promotedLearnerId;
    }

    public static RegistrationResult of(RegistrationStatus status, String learnerId, String message) {
        return new RegistrationResult(status, learnerId, Optional.empty(), message);
    }

    public static RegistrationResult dropped(String learnerId, String promotedId) {
        return new RegistrationResult(
                RegistrationStatus.DROPPED,
                learnerId,
                Optional.ofNullable(promotedId),
                promotedId == null
                        ? "Learner dropped; no eligible waitlisted learner was available."
                        : "Learner dropped; " + promotedId + " was promoted."
        );
    }
}
