package com.tabookey.bizpoc;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

class ApprovalViewHolder extends RecyclerView.ViewHolder {
    TextView guardianName;
    ImageView approvedCheckbox;

    ApprovalViewHolder(View itemView) {
        super(itemView);
        guardianName = itemView.findViewById(R.id.guardianName);
        approvedCheckbox = itemView.findViewById(R.id.approvedCheckbox);
    }
}