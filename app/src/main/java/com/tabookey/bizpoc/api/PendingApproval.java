package com.tabookey.bizpoc.api;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.tabookey.bizpoc.Approval;
import com.tabookey.bizpoc.ApprovalState;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class PendingApproval implements Serializable {
    public String id, recipientAddr, comment, coin, amount;
    public Date createDate;
    public ArrayList<BitgoUser> approvedByUsers;    //users who already approved (e.g sender himself)
    public BitgoUser creator;
    public TokenInfo token;

    public PendingApproval() {

    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (!(obj instanceof PendingApproval))
            return false;
        PendingApproval other = (PendingApproval) obj;
        if (other.approvedByUsers != null && !other.approvedByUsers.equals(approvedByUsers))
            return false;
        return Objects.equals(other.id, this.id);

    }

    public PendingApproval(String id, Date createDate, String recipientAddr, String comment, String coin, String amount, ArrayList<BitgoUser> approvedByUsers, BitgoUser creator, TokenInfo token) {
        this.id = id;
        this.createDate = createDate;
        this.recipientAddr = recipientAddr;
        this.comment = comment;
        this.coin = coin;
        this.amount = amount;
        this.approvedByUsers = approvedByUsers;
        this.creator = creator;
        this.token = token;
    }

    public List<Approval> getApprovals(List<BitgoUser> guardians) {
        return guardians.stream().filter(b -> b != null).map(b -> {
            boolean isApproved = false;

            for (BitgoUser user : approvedByUsers) {
                if (user != null && user.email.equals(b.email))
                    isApproved = true;
            }
            if (b.email.equals("did@approve")) {
                isApproved = true;
            }
            return new Approval(b.name, isApproved ? ApprovalState.APPROVED : ApprovalState.WAITING);

        }).collect(Collectors.toList());
    }


    @NonNull
    @Override
    public String toString() {
        StringBuilder approvedByUsers = new StringBuilder();
        for (BitgoUser approver : this.approvedByUsers) {
            approvedByUsers.append(approver).append(" ");
        }
        return String.format("id=%s amount=%s approvedByUsers=%s", id, amount, approvedByUsers);
    }
}
