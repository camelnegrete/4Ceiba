package com.camel.medisalud.specification;

import com.camel.medisalud.domain.model.Doctor;
import org.springframework.data.jpa.domain.Specification;

public final class DoctorSpecification {

    private static final String FULL_NAME = "fullName";
    private static final String SPECIALTY = "specialty";
    private static final String EMAIL = "email";

    private DoctorSpecification() {
    }

    public static Specification<Doctor> fullNameContains(String term) {
        return (root, query, cb) -> SpecificationUtils.isBlank(term)
                ? null
                : SpecificationUtils.containsIgnoreCase(cb, root.get(FULL_NAME), term);
    }

    public static Specification<Doctor> hasSpecialty(String specialty) {
        return (root, query, cb) -> SpecificationUtils.isBlank(specialty)
                ? null
                : SpecificationUtils.equalsIgnoreCase(cb, root.get(SPECIALTY), specialty);
    }

    public static Specification<Doctor> hasEmail(String email) {
        return (root, query, cb) -> SpecificationUtils.isBlank(email)
                ? null
                : SpecificationUtils.equalsIgnoreCase(cb, root.get(EMAIL), email);
    }
}
