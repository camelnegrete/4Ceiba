package com.camel.medisalud.specification;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;

public final class SpecificationUtils {

    private SpecificationUtils() {
    }

    public static Predicate containsIgnoreCase(CriteriaBuilder cb, Expression<String> path, String value) {
        return cb.like(cb.lower(path), "%" + value.toLowerCase() + "%");
    }

    public static Predicate equalsIgnoreCase(CriteriaBuilder cb, Expression<String> path, String value) {
        return cb.equal(cb.lower(path), value.toLowerCase());
    }

    public static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
