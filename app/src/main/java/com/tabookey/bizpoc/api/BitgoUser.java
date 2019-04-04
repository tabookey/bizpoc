package com.tabookey.bizpoc.api;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import androidx.annotation.Nullable;

public class BitgoUser {

    public enum Perm {view, spend, admin }
    public enum OtpType { yubikey, totp }

    final public String id;
    final public String email;
    public String name;
    final public List<Perm> permissions;

    public List<OtpType> otpTypes;
    final public boolean isEnterpriseAdmin;


    public boolean hasPerm(Perm perm) {
        return permissions!=null && permissions.contains(perm);
    }

    public boolean hasOtp(OtpType type) { return otpTypes.contains(type); }

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
        this.permissions = Collections.unmodifiableList(permissions==null ? Collections.emptyList() : permissions);
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if ( !(obj instanceof BitgoUser))
            return false;

        BitgoUser other = (BitgoUser) obj;
        return Objects.equals(id, other.id);
    }
}
