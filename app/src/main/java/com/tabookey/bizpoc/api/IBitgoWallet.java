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

    /**
     * send coins
     * @param req - request paramters
     * @param cb - optional callback. type is "state" for for interim state changes, "result" for final result, or "error" in case of error.
     * @throws RuntimeException in case any error returns (error message is the "error" status reported
     */
    void sendCoins(SendRequest req, StatusCB cb);

    interface StatusCB {
        void onStatus(String type, String msg);
    }


    List<PendingApproval> getPendingApprovals();

    //can't approve through API: according to documentation, must use web interface
    // (access token has only permission to reject, not approve)
    //    public void approvePending(PendingApproval approval, String otp);

    //admin reject a transaction (or owner cancel its own transaction)
    //NOTE: api says otp is required. test shows its ignored (works with garbage value)
    public void rejectPending(PendingApproval approval, String otp);
}
