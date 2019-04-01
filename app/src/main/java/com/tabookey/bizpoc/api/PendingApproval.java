package com.tabookey.bizpoc.api;

import com.tabookey.bizpoc.Approval;
import com.tabookey.bizpoc.ApprovalState;

import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import androidx.annotation.Nullable;

public class PendingApproval {
    public String id, recipientAddr, comment, coin, amount;
    public Date createDate;
    public List<BitgoUser> approvedByUsers;    //users who already approved (e.g sender himself)
    public BitgoUser creator;
    public TokenInfo token;

    public PendingApproval() {

    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if ( !(obj instanceof PendingApproval) )
            return false;
        return Objects.equals(((PendingApproval) obj).id, this.id);
    }

    public PendingApproval(String id, Date createDate, String recipientAddr, String comment, String coin, String amount, List<BitgoUser> approvedByUsers, BitgoUser creator, TokenInfo token) {
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
        return guardians.stream().map(b -> {
            boolean isApproved = false;

            for (BitgoUser user : approvedByUsers) {
                if (user.email.equals(b.email))
                    isApproved = true;
            }
            if (b.email.equals("did@approve")) {
                isApproved = true;
            }
            return new Approval(b.name, isApproved ? ApprovalState.APPROVED : ApprovalState.WAITING);

        }).collect(Collectors.toList());
    }
}
