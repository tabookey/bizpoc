package com.tabookey.bizpoc;

public class Approval {
    String name;
    State state;

    public enum State {
        WAITING,
        // So far, incoming transfers are also "approved"
        APPROVED,
        REJECTED,
        CANCELLED
    }

    public Approval(String name, State state) {
        this.name = name;
        this.state = state;
    }
}
