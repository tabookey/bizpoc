package com.tabookey.bizpoc;

public class BitgoUser {
    enum Role { Viewer, Spender, Admin }
    public String id, email, name;
    public Role role;
    public boolean isEnterpriseAdmin;
}
