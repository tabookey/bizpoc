package com.tabookey.bizpoc.api;

public class PendingApproval {
    public String id, createDate, recipientAddr, comment, coin, amount;
    public BitgoUser[] approvedByUsers;    //users who already approved (e.g sender himself)
    public BitgoUser creator;
}
