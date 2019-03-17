package com.tabookey.bizpoc;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.tabookey.bizpoc.api.Transfer;
import com.tabookey.bizpoc.impl.Utils;

import java.util.List;
import java.util.Locale;

public class TransactionHistoryAdapter extends ArrayAdapter<Transfer> {

    private Context context;
    private List<Transfer> data;

    TransactionHistoryAdapter(Context context, int resource, List<Transfer> data) {
        super(context, resource);
        this.context = context;
        this.data = data;
    }

    @Override
    public int getCount() {
        return data.size();
    }

    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.transaction_line, parent, false);
        TextView dateTextView = view.findViewById(R.id.transactionDate);
        TextView idTextView = view.findViewById(R.id.transactionId);
        TextView valueTextView = view.findViewById(R.id.transactionValue);
        TextView coinTextView = view.findViewById(R.id.transactionCoin);
        TextView dollarTextView = view.findViewById(R.id.transactionDollarValue);
        TextView remoteTextView = view.findViewById(R.id.transactionRemoteAddress);
        TextView transactionComment = view.findViewById(R.id.transactionComment);
        Transfer transfer = data.get(position);
        transactionComment.setText(transfer.comment);
//        txid, valueString, coin, usd, date, remoteAddress
        dateTextView.setText(transfer.date.toString());
        idTextView.setText(transfer.txid);
        double value = Utils.integerStringToCoinDouble(transfer.valueString, transfer.token.decimalPlaces);
        valueTextView.setText(String.format(Locale.US, "%.6f", value));
        coinTextView.setText(transfer.coin);
        dollarTextView.setText(transfer.usd);
        remoteTextView.setText(transfer.remoteAddress);
        return view;
    }
}
