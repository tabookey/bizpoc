package com.tabookey.bizpoc;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

public class ApprovalsRecyclerAdapter extends RecyclerView.Adapter<ApprovalViewHolder> {
    private List<Approval> approvals;

    ApprovalsRecyclerAdapter(List<Approval> approvals) {
        this.approvals = approvals;
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
        holder.approvedCheckbox.setImageResource(approval.isApproved ? R.drawable.ic_approved : R.drawable.ic_waiting);
    }

    @Override
    public int getItemCount() {
        return approvals.size();
    }
}