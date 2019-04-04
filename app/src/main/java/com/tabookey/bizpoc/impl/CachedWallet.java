package com.tabookey.bizpoc.impl;

import com.tabookey.bizpoc.api.BitgoUser;
import com.tabookey.bizpoc.api.IBitgoWallet;
import com.tabookey.bizpoc.api.PendingApproval;
import com.tabookey.bizpoc.api.SendRequest;
import com.tabookey.bizpoc.api.Transfer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class CachedWallet implements IBitgoWallet {

    private final IBitgoWallet netwallet;
    private List<String> coins;
    private List<BitgoUser> guardians;
    private HashMap<String, String> balances = new HashMap<>();

    private List<Transfer> transfers;
    private List<PendingApproval> pendingapprovals;

    private final String id, label, address;

    /**
     * initialize a cached wallet.
     * initial call will fill the object (so must be called from a background thread)
     * later calls return cached values, except for update()
     *
     * @param netwallet - network-connected wallet, to fetch dated data.
     */
    public CachedWallet(IBitgoWallet netwallet) {
        this.netwallet = netwallet;
        this.id = netwallet.getId();
        this.label = netwallet.getLabel();
        this.address = netwallet.getAddress();
        this.coins = netwallet.getCoins();
        this.guardians = netwallet.getGuardians();
        update(null);
    }

    /**
     * Force update all items that might modify over time
     * MUST be called from a background thread, since it blocks until network access is complete
     * <p>
     * - coin balances
     * - transfers
     * - pending approvals
     *
     * @param onChange - callback to call (from the background thread!) in case of change.
     */
    @Override
    public void update(Runnable onChange) {
        HashMap<String, String> newBalance = new HashMap<>();
        List<String> coins = new ArrayList<>(getCoins());
        for (String coin : coins)
            newBalance.put(coin, netwallet.getBalance(coin));
        List<Transfer> newTransfers = netwallet.getTransfers(0);
        List<PendingApproval> newPendingapprovals = netwallet.getPendingApprovals();
        if (newBalance.equals(balances) &&  //no changes in balance
                newTransfers.equals(transfers) &&
                newPendingapprovals.equals(pendingapprovals) ) {
            //no change. return.
            return;
        }
        balances = newBalance;
        transfers = newTransfers;
        pendingapprovals = newPendingapprovals;
        if (onChange != null)
            onChange.run();
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
        int size = transfers.size();
        return transfers.subList(0, limit == 0 || limit > size ? size : limit);
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
