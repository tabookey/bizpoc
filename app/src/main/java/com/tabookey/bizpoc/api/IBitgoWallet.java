package com.tabookey.bizpoc.api;

import java.util.List;

public interface IBitgoWallet {
    String getId();

    //list of coins supported by this wallet.
    List<String> getCoins();

    String getLabel();

    //all vault users that are not "me"
    List<BitgoUser> getGuardians();

    String getBalance(String coin);

    String getAddress();

    //get past transfers from or to this wallet.
    List<Transfer> getTransfers();

    /**
     * send coins
     * @param req - request paramters
     * @param cb - optional callback. type is "state" for for interim state changes, "result" for final result, or "error" in case of error.
     * @return pending transaction id
     * @throws RuntimeException in case any error returns (error message is the "error" status reported
     */
    String sendCoins(SendRequest req, StatusCB cb);

    /**
     * check if the given passphrase is valid for this wallet - that is, it can be used by sendCoins
     * @param passphrase - passphrase to check
     */
    boolean checkPassphrase(String passphrase);

    interface StatusCB {
        void onStatus(String type, String msg);
    }

    List<PendingApproval> getPendingApprovals();

    //can't approve through API: according to documentation, must use web interface
    // (access token has only permission to reject, not approve)
    //    public void approvePending(PendingApproval approval, String otp);

    //admin reject a transaction (or owner cancel its own transaction)
    //NOTE: api says otp is required. test shows its ignored (works with garbage value)
    void rejectPending(PendingApproval approval);
}
