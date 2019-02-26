package com.tabookey.bizpoc;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class TransactionHistoryAdapter extends ArrayAdapter<String> {

    private Context context;
    private String[] data;

    TransactionHistoryAdapter(Context context, int resource, String[] data) {
        super(context, resource);
        this.context = context;
        this.data = data;
    }

    @Override
    public int getCount() {
        return data.length;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.transaction_line, parent, false);
        TextView dateTextView = view.findViewById(R.id.transactionDate);
        dateTextView.setText(data[position]);
        return view;
    }
}
