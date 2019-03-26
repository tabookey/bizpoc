package com.tabookey.bizpoc.api;

import com.tabookey.bizpoc.Approval;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class PendingApproval {
    public String id, recipientAddr, comment, coin, amount;
    public Date createDate;
    public List<BitgoUser> approvedByUsers;    //users who already approved (e.g sender himself)
    public BitgoUser creator;
    public TokenInfo token;

    public PendingApproval() {

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
            return new Approval(b.name, isApproved);

        }).collect(Collectors.toList());
    }
}
