package ru.company.poc.eav;

public enum Condition {
    EQUAL,
    LIKE,
    IN;

    public boolean isEqual() {
        return this == EQUAL || this == IN;
    }

    public String format(String path, String value) {
        return switch (this) {
            case EQUAL -> "%s = '%s'".formatted(path, value);
            case LIKE -> path + " like '%" + value + "%'";
            case IN -> value;
        };
    }
}
