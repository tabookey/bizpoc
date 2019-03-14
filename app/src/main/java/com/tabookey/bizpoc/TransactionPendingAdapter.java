package com.tabookey.bizpoc;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.tabookey.bizpoc.api.PendingApproval;
import com.tabookey.bizpoc.api.Transfer;
import com.tabookey.bizpoc.impl.Utils;

import java.util.List;
import java.util.Locale;

public class TransactionPendingAdapter extends ArrayAdapter<PendingApproval> {

    private Context context;
    private List<PendingApproval> data;

    TransactionPendingAdapter(Context context, int resource, List<PendingApproval> data) {
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
        View view = inflater.inflate(R.layout.pending_transaction_line, parent, false);
        TextView dateTextView = view.findViewById(R.id.transactionDate);
        TextView idTextView = view.findViewById(R.id.transactionId);
        TextView valueTextView = view.findViewById(R.id.transactionValue);
        TextView coinTextView = view.findViewById(R.id.transactionCoin);
        TextView dollarTextView = view.findViewById(R.id.transactionDollarValue);
        TextView remoteTextView = view.findViewById(R.id.transactionRemoteAddress);
        TextView transactionComment = view.findViewById(R.id.transactionComment);
//        txid, valueString, coin, usd, date, remoteAddress
        dateTextView.setText(data.get(position).createDate.toString());
        idTextView.setText(data.get(position).id);
        double value = Utils.weiStringToEtherDouble(data.get(position).amount);
        valueTextView.setText(String.format(Locale.US,"%.6f", value));
        coinTextView.setText(data.get(position).coin);
        dollarTextView.setText("Not saved for pending?");
        remoteTextView.setText(data.get(position).recipientAddr);
        return view;
    }
}
