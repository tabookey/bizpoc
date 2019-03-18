package com.tabookey.bizpoc;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.tabookey.bizpoc.api.PendingApproval;

import java.util.List;

public class ApprovalsAdapter extends ArrayAdapter<ApprovalsAdapter.Approval> {

    static class Approval {
        String name;
        boolean isApproved;

        public Approval(String name, boolean isApproved) {
            this.name = name;
            this.isApproved = isApproved;
        }
    }

    private Context context;
    private List<Approval> data;

    ApprovalsAdapter(Context context, int resource, List<Approval> data) {
        super(context, resource);
        this.context = context;
        this.data = data;
    }

    @Override
    public int getCount() {
        return data.size();
    }

    //
//    public String id, createDate, recipientAddr, comment, coin, amount;
//    public List<BitgoUser> approvedByUsers;    //users who already approved (e.g sender himself)
//    public BitgoUser creator;
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.approvals_line, parent, false);
        TextView guardianName = view.findViewById(R.id.guardianName);
        ImageView approvedCheckbox = view.findViewById(R.id.approvedCheckbox);
        Approval approval = data.get(position);
        guardianName.setText(approval.name);
        approvedCheckbox.setVisibility(approval.isApproved ? View.VISIBLE : View.GONE);
        return view;
    }
}
