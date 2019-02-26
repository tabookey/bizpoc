package com.tabookey.bizpoc.api;

import java.util.List;

public interface IBitgoWallet {

    String getCoin();
    String getId();

    String getLabel();
    //users of this vault. each with its role for this vault.
    List<BitgoUser> getUsers();

    String getBalance();
    String getAddress();

    //get past transfers from or to this wallet.
    List<Transfer> getTransfers();

    void sendMoney(SendRequest req);

    List<PendingApproval> getPendingApprovals();

    //can't approve through API: according to documentation, must use web interface
    // (access token has only permission to reject, not approve)
    //    public void approvePending(PendingApproval approval, String otp);

    //admin reject a transaction (or owner cancel its own transaction)
    //NOTE: api says otp is required. test shows its ignored (works with garbage value)
    public void rejectPending(PendingApproval approval, String otp);
}
