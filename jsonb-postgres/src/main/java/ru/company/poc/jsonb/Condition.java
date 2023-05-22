package ru.company.poc.jsonb;

public enum Condition {
    INCLUDE,
    EQUAL,
    LIKE;

    public boolean isEqual() {
        return this == EQUAL || this == INCLUDE;
    }
}
