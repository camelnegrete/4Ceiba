package com.camel.medisalud.specification;

import com.camel.medisalud.domain.model.Patient;
import java.time.LocalDate;
import org.springframework.data.jpa.domain.Specification;

public final class PatientSpecification {

    private static final String FULL_NAME = "fullName";
    private static final String DOCUMENT = "document";
    private static final String EMAIL = "email";
    private static final String BIRTH_DATE = "birthDate";

    private PatientSpecification() {
    }

    public static Specification<Patient> fullNameContains(String term) {
        return (root, query, cb) -> SpecificationUtils.isBlank(term)
                ? null
                : SpecificationUtils.containsIgnoreCase(cb, root.get(FULL_NAME), term);
    }

    public static Specification<Patient> hasDocument(String document) {
        return (root, query, cb) -> SpecificationUtils.isBlank(document)
                ? null
                : cb.equal(root.get(DOCUMENT), document);
    }

    public static Specification<Patient> hasEmail(String email) {
        return (root, query, cb) -> SpecificationUtils.isBlank(email)
                ? null
                : SpecificationUtils.equalsIgnoreCase(cb, root.get(EMAIL), email);
    }

    public static Specification<Patient> bornBetween(LocalDate from, LocalDate to) {
        return (root, query, cb) -> {
            if (from != null && to != null) {
                return cb.between(root.<LocalDate>get(BIRTH_DATE), from, to);
            }
            if (from != null) {
                return cb.greaterThanOrEqualTo(root.<LocalDate>get(BIRTH_DATE), from);
            }
            if (to != null) {
                return cb.lessThanOrEqualTo(root.<LocalDate>get(BIRTH_DATE), to);
            }
            return null;
        };
    }
}
