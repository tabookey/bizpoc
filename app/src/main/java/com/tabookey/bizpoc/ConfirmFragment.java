package com.tabookey.bizpoc;

import android.app.AlertDialog;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.tabookey.bizpoc.api.ExchangeRate;
import com.tabookey.bizpoc.api.SendRequest;
import com.tabookey.bizpoc.impl.Utils;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.Locale;

public class ConfirmFragment extends Fragment {
    TextView recipientAddress;
    TextView dollarEquivalent;
    TextView etherSendAmount;
    private SendRequest sendRequest;
    private ExchangeRate exchangeRate;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.confirm_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Button submit = view.findViewById(R.id.submitButton);
        recipientAddress = view.findViewById(R.id.recipientAddressTextView);
        dollarEquivalent = view.findViewById(R.id.dollarEquivalent);
        etherSendAmount = view.findViewById(R.id.etherSendAmount);

        ListView guardiansListView = view.findViewById(R.id.guardiansListView);
        FragmentActivity activity = getActivity();
        if (activity == null) {
            return;
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(activity, android.R.layout.simple_list_item_1);
        adapter.addAll("liraz", "eli");
        guardiansListView.setAdapter(adapter);
        submit.setOnClickListener(v -> promptOtp());

        double etherDouble = Utils.weiStringToEtherDouble(sendRequest.amount);
        dollarEquivalent.setText(String.format(Locale.US, "$%.2f USD", etherDouble * exchangeRate.average24h));
        etherSendAmount.setText(String.format(Locale.US, "%.6f ETH", etherDouble));
        recipientAddress.setText(sendRequest.recipientAddress);
    }

    public void setRequest(SendRequest sendRequest) {
        this.sendRequest = sendRequest;
    }

    private void sendTransaction() {
        new Handler().postDelayed(()->{

        }, 2000);
    }


    private void promptOtp() {
        FragmentActivity activity = getActivity();
        if (activity == null) {
            return;
        }
        View view = getView();
        if (view == null) {
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle("Title");

        final EditText input = new EditText(activity);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        builder.setPositiveButton("OK", (dialog, which) -> {
            sendTransaction();
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    public void setExchangeRate(ExchangeRate exchangeRate) {
        this.exchangeRate = exchangeRate;
    }
}