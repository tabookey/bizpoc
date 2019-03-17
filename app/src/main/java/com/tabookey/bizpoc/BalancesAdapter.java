package com.tabookey.bizpoc;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.tabookey.bizpoc.api.TokenInfo;
import com.tabookey.bizpoc.impl.Utils;

import java.util.List;
import java.util.Locale;

import com.tabookey.bizpoc.BalancesAdapter.Balance;

public class BalancesAdapter extends ArrayAdapter<Balance> {

    private Context context;
    private List<Balance> data;

    public static class Balance {
        String coinName;
        String coinBalance;
        double coinWorthUsd;
        TokenInfo tokenInfo;

        public Balance(String coinName, String coinBalance, double coinWorthUsd, TokenInfo tokenInfo) {
            this.coinName = coinName;
            this.coinBalance = coinBalance;
            this.coinWorthUsd = coinWorthUsd;
            this.tokenInfo = tokenInfo;
        }
    }

    BalancesAdapter(Context context, int resource, List<Balance> data) {
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
        View view = inflater.inflate(R.layout.balance_line, parent, false);
        TextView coinExchangeRate = view.findViewById(R.id.coinExchangeRate);
        TextView coinBalance = view.findViewById(R.id.coinBalance);
        TextView coinName = view.findViewById(R.id.coinName);
        TextView coinDollarValue = view.findViewById(R.id.coinDollarValue);
        Balance balance = data.get(position);
        double coinWorthUsd = balance.coinWorthUsd;
        coinExchangeRate.setText(String.format(Locale.US, "%.2f", coinWorthUsd));
        double value = Utils.integerStringToCoinDouble(balance.coinBalance, balance.tokenInfo.decimalPlaces);
        coinBalance.setText(String.format(Locale.US, "%.6f", value));
        coinName.setText(balance.tokenInfo.name);
        coinDollarValue.setText(String.format(Locale.US, "%.2f USD", coinWorthUsd * value));
        return view;
    }
}
