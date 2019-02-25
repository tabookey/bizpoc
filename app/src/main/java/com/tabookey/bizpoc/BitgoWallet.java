package com.tabookey.bizpoc;

import java.util.List;

public interface BitgoWallet {
    String getId();

    //users of this vault. each with its role for this vault.
    List<BitgoUser> getUsers();

    String getBalance(String coin);

    List<Transfer> getTransfers();

    void send(SendRequest req);

    List<PendingApproval> getPendingApprovals();

    //2nd/3rd admin approves a transaction
    public void approve(PendingApproval approval);

    //admin reject a transaction (or owner cancel its own transaction
    public void reject(PendingApproval approval);
}
