package com.tabookey.bizpoc.api;

import java.util.List;

public interface IBitgoWallet {

    String getCoin();
    String getId();

    String getLabel();
    //users of this vault. each with its role for this vault.
    List<BitgoUser> getUsers();

    String getBalance();

    //get past transfers from or to this wallet.
    List<Transfer> getTransfers();

    void sendMoney(SendRequest req);

    List<PendingApproval> getPendingApprovals();

    //2nd/3rd admin approves a transaction
    public void approvePending(PendingApproval approval);

    //admin reject a transaction (or owner cancel its own transaction
    public void rejectPending(PendingApproval approval);
}
