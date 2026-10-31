package com.schwab.registration;

import com.schwab.registration.model.*;
import com.schwab.registration.repository.InMemoryEnrollmentRepository;
import com.schwab.registration.service.EnrollmentService;

import java.util.Set;

/** Small runnable demonstration of the one-offering workflow. */
public final class Application {
    private Application() { }

    public static void main(String[] args) {
        Course course = new Course("JAVA-201", "Core Java", 2, "JAVA-101");
        InMemoryEnrollmentRepository repository = new InMemoryEnrollmentRepository();
        repository.addLearner(new Learner("L-101", "Asha", Set.of("JAVA-101")));
        repository.addLearner(new Learner("L-102", "Ravi", Set.of("JAVA-101")));
        repository.addLearner(new Learner("L-103", "Maya", Set.of("JAVA-101")));

        EnrollmentService service = new EnrollmentService(course, repository);
        System.out.println(service.enroll("L-101"));
        System.out.println(service.enroll("L-102"));
        System.out.println(service.enroll("L-103"));
        System.out.println(service.drop("L-101"));
        System.out.println("Enrolled: " + service.getEnrolledLearnerIds());
        System.out.println("Waitlist: " + service.getWaitlistLearnerIds());
    }
}
