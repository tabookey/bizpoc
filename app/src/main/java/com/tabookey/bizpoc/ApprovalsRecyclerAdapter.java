package com.tabookey.bizpoc;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

public class ApprovalsRecyclerAdapter extends RecyclerView.Adapter<ApprovalViewHolder> {
    private final Context mContext;
    private List<Approval> approvals;
    private ApprovalState state;


    ApprovalsRecyclerAdapter(Context context, List<Approval> approvals, ApprovalState state) {
        this.mContext = context;
        this.approvals = approvals;
        this.state = state;
    }

    @NonNull
    @Override
    public ApprovalViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.approvals_line, parent, false);
        return new ApprovalViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final ApprovalViewHolder holder, final int position) {
        Approval approval = approvals.get(position);
        holder.guardianName.setText(approval.name);
        boolean isApproved = approval.state == ApprovalState.APPROVED || state == ApprovalState.APPROVED;
        boolean isDeclined = approval.state == ApprovalState.DECLINED;
        int image = R.drawable.ic_waiting;
        if (isApproved) {
            image = R.drawable.ic_success;
        } else if (isDeclined) {
            image = R.drawable.ic_declined_hist;
        }
        holder.approvedCheckbox.setImageResource(image);
        if (state == ApprovalState.CANCELLED) {
            holder.guardianName.setTextColor(mContext.getColor(R.color.black33));
            holder.approvedCheckbox.setAlpha(0.33f);
        }
    }

    @Override
    public int getItemCount() {
        return approvals.size();
    }
}