package com.tabookey.bizpoc.api;

import java.util.List;

public class PendingApproval {
    public String id, createDate, recipientAddr, comment, coin, amount;
    public List<BitgoUser> approvedByUsers;    //users who already approved (e.g sender himself)
    public BitgoUser creator;

    public PendingApproval() {

    }

    public PendingApproval(String id, String createDate, String recipientAddr, String comment, String coin, String amount, List<BitgoUser> approvedByUsers, BitgoUser creator) {
        this.id = id;
        this.createDate = createDate;
        this.recipientAddr = recipientAddr;
        this.comment = comment;
        this.coin = coin;
        this.amount = amount;
        this.approvedByUsers = approvedByUsers;
        this.creator = creator;
    }
}
