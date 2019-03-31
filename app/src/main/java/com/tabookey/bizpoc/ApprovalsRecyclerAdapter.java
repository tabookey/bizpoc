package com.tabookey.bizpoc;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

public class ApprovalsRecyclerAdapter extends RecyclerView.Adapter<ApprovalViewHolder> {
    private List<Approval> approvals;
    private State state;

    public enum State {
        NORMAL, // Some guardians may have approved the transaction, some not yet, some rejected
        HISTORY_APPROVED, // Show all guardians as "approving" the transaction (does not have to be the case if we allow config changes)
        HISTORY_CANCELLED // Gray out all guardians - the transaction is already rejected
    }

    ApprovalsRecyclerAdapter(List<Approval> approvals, State state) {
        this.approvals = approvals;
        this.state = state;
    }

    @NonNull
    @Override
    public ApprovalViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.approvals_line, parent, false);
        return new ApprovalViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final ApprovalViewHolder holder, final int position) {
        Approval approval = approvals.get(position);
        holder.guardianName.setText(approval.name);
        boolean isApproved = approval.state == Approval.State.APPROVED || state == State.HISTORY_APPROVED;
        holder.approvedCheckbox.setImageResource(isApproved ? R.drawable.ic_approved : R.drawable.ic_waiting);
    }

    @Override
    public int getItemCount() {
        return approvals.size();
    }
}