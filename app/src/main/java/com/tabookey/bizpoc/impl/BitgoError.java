package com.tabookey.bizpoc.impl;

public class BitgoError extends RuntimeException {
    private final String name, description;

    public BitgoError(String name, String description) {
        this(name,description,null);
    }
    public BitgoError(String error, String description, Throwable cause) {
        super(description, cause);
        this.name = error;
        this.description = description;
    }

    String getName() { return name; }
    String getDescription() { return description; }

}
