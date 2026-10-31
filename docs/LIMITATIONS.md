# Limitations and Follow-up Risks

1. **No persistence:** all learners and registration state are in memory and disappear when the process exits.
2. **Single JVM only:** synchronized methods coordinate threads using one service instance. They do not coordinate multiple instances or processes.
3. **One offering:** the service intentionally models one course offering, not a catalog or multi-term registration system.
4. **Single prerequisite:** eligibility checks one prerequisite course ID. Complex prerequisite expressions are out of scope.
5. **No waitlist cancellation:** learners can drop only after enrollment; explicit withdrawal from the waitlist is not implemented.
6. **No authentication/authorization:** learner identity is supplied as an ID because identity integration is outside scope.
7. **No audit history:** the prototype stores current state only and does not retain registration event history.
8. **Ineligible waitlist entries remain:** an ineligible learner is skipped and retained. The seat may remain vacant if no eligible learner is found. A production policy might need expiry, notification, or revalidation rules.
9. **No external API layer:** methods are directly callable Java service operations; REST endpoints are intentionally omitted.
10. **Operational concerns:** production use would require durable transactions, uniqueness constraints, observability, and recovery behavior.
