package com.tabookey.bizpoc.api;

import java.util.Collections;
import java.util.List;

public class BitgoUser {

    enum Perm { Viewer, Spender, Admin }

    final public String id, email, name;
    final public List<Perm> permissions;
    final public boolean isEnterpriseAdmin;

    public boolean hasPerm(Perm perm) {
        return permissions!=null && permissions.contains(perm);
    }

    public BitgoUser(String id, String email, String name) {
        this(id,email,name,false,Collections.emptyList());
    }

    //copy a user, just set wallet-permissions
    public BitgoUser(BitgoUser from, List<Perm> permissions) {
        this.id = from.id;
        this.email = from.email;
        this.name = from.name;
        this.isEnterpriseAdmin = from.isEnterpriseAdmin;
        this.permissions = Collections.unmodifiableList(permissions);
    }

    public BitgoUser(String id, String email, String name, boolean isEnterpriseAdmin, List<Perm> permissions) {
        this.id = id;
        this.email = email;
        this.name = name;
        this.isEnterpriseAdmin = isEnterpriseAdmin;
        this.permissions = Collections.unmodifiableList(permissions);
    }
}
