package com.tabookey.bizpoc.impl;

import com.tabookey.bizpoc.api.BitgoUser;
import com.tabookey.bizpoc.api.IBitgoWallet;
import com.tabookey.bizpoc.api.PendingApproval;
import com.tabookey.bizpoc.api.SendRequest;
import com.tabookey.bizpoc.api.Transfer;

import java.util.HashMap;
import java.util.List;

public class CachedWallet implements IBitgoWallet {

    private final IBitgoWallet netwallet;
    private final Runnable walletChange;
    private List<String> coins;
    private List<BitgoUser> guardians;
    private HashMap<String, String> balances = new HashMap<>();

    private List<Transfer> transfers;
    private List<PendingApproval> pendingapprovals;

    private final String id, label, address;

    public CachedWallet(IBitgoWallet netwallet, Runnable walletChange) {
        this.walletChange = walletChange;
        this.netwallet = netwallet;
        this.id = netwallet.getId();
        this.label = netwallet.getLabel();
        this.address = netwallet.getAddress();
        this.coins = netwallet.getCoins();
        this.guardians = netwallet.getGuardians();
        update();
    }

    /**
     * update all items that might modify over time:
     * - coin balances
     * - transfers
     * - pending approvals
     */
    public void update() {
        HashMap<String, String> newBalance = new HashMap<>();
        for (String coin : getCoins())
            newBalance.put(coin, netwallet.getBalance(coin));
        List<Transfer> newTransfers = netwallet.getTransfers(0);
        List<PendingApproval> newPendingapprovals = getPendingApprovals();
        //we check only first transfer and first pendingApproval, since these are ordered list: a new item is always put first, pushing
        // the rest down.
        if (newBalance.equals(balances) &&  //no changes in balance
                (newTransfers.size() == 0 || newTransfers.get(0).txid.equals(transfers.get(0).txid)) &&
                (newPendingapprovals.size() == 0 || newPendingapprovals.get(0).id.equals(pendingapprovals.get(0).id))
        ) {
            //no change. return.
            return;
        }
        balances = newBalance;
        transfers = newTransfers;
        pendingapprovals = newPendingapprovals;
        walletChange.run();
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public List<String> getCoins() {
        return coins;
    }

    @Override
    public String getLabel() {
        return label;
    }

    @Override
    public List<BitgoUser> getGuardians() {
        return guardians;
    }

    @Override
    public String getBalance(String coin) {
        return this.balances.get(coin);
    }

    @Override
    public String getAddress() {
        return address;
    }

    @Override
    public List<Transfer> getTransfers(int limit) {
        return transfers.subList(0, limit == 0 ? transfers.size() : limit);
    }

    @Override
    public String sendCoins(SendRequest req, StatusCB cb) {
        return netwallet.sendCoins(req, cb);
    }

    @Override
    public boolean checkPassphrase(String passphrase) {
        return netwallet.checkPassphrase(passphrase);
    }

    @Override
    public List<PendingApproval> getPendingApprovals() {
        return pendingapprovals;
    }

    @Override
    public void rejectPending(PendingApproval approval) {
        netwallet.rejectPending(approval);
    }
}
