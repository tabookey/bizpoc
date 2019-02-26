package com.tabookey.bizpoc.api;

import java.util.List;

public class PendingApproval {
    public String id, createDate, recipientAddr, comment, coin, amount;
    public List<BitgoUser> approvedByUsers;    //users who already approved (e.g sender himself)
    public BitgoUser creator;
}
