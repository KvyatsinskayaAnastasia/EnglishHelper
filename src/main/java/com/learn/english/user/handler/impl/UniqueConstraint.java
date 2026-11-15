package com.learn.english.user.handler.impl;

import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DuplicateKeyException;

import java.util.Locale;

final class UniqueConstraint {
    private static final String UNIQUE_VIOLATION = "23505";
    private static final String WORD_PAIR = "original_translation";

    private UniqueConstraint() {
    }

    static boolean isViolation(Throwable error) {
        if (error instanceof DuplicateKeyException) {
            return true;
        }
        Throwable current = error;
        while (current != null) {
            if (current instanceof ConstraintViolationException constraint) {
                if (UNIQUE_VIOLATION.equals(constraint.getSQLState())) {
                    return true;
                }
                String name = constraint.getConstraintName();
                return name != null && name.toLowerCase(Locale.ROOT).contains(WORD_PAIR);
            }
            current = current.getCause();
        }
        return false;
    }
}
