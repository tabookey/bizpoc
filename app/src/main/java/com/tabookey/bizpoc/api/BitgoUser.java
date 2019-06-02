package com.tabookey.bizpoc.api;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import androidx.annotation.Nullable;

public class BitgoUser implements Serializable {

    public enum Perm {view, spend, admin }
    public enum OtpType { yubikey, totp }

    final public String id;
    final public String email;
    public String name;
    final public ArrayList<Perm> permissions;

    public ArrayList<OtpType> otpTypes;

    public boolean hasPerm(Perm perm) {
        return permissions!=null && permissions.contains(perm);
    }

    public boolean hasOtp(OtpType type) { return otpTypes.contains(type); }

    public BitgoUser(String id, String email, String name) {
        this(id,email,name,Collections.emptyList());
    }

    //copy a user, just set wallet-permissions
    public BitgoUser(BitgoUser from, List<Perm> permissions) {
        this.id = from.id;
        this.email = from.email;
        this.name = from.name;
        this.permissions = new ArrayList<>(permissions);
    }

    public BitgoUser(String id, String email, String name, List<Perm> permissions) {
        this.id = id;
        this.email = email;
        this.name = name;
        this.permissions = new ArrayList<>(permissions==null ? new ArrayList<>() : permissions);
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if ( !(obj instanceof BitgoUser))
            return false;

        BitgoUser other = (BitgoUser) obj;
        return Objects.equals(id, other.id);
    }
}
