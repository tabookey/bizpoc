package com.tabookey.bizpoc;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.tabookey.bizpoc.api.ExchangeRate;
import com.tabookey.bizpoc.api.Global;
import com.tabookey.bizpoc.api.SendRequest;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Locale;
import java.util.Random;

public class SendFragment extends Fragment {

    EditText etherSendAmountEditText;
    ExchangeRate exchangeRate;
    EditText destinationEditText;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.send_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        new Thread(() -> exchangeRate = Global.ent.getMarketData()).start();

        super.onViewCreated(view, savedInstanceState);
        Button continueButton = view.findViewById(R.id.continueButton);
        destinationEditText = view.findViewById(R.id.destinationEditText);
        Button scanDestinationButton = view.findViewById(R.id.scanDestinationButton);
        scanDestinationButton.setOnClickListener(v -> startActivityForResult(new Intent(getActivity(), ScanActivity.class), 1));
        ListView guardiansListView = view.findViewById(R.id.guardiansListView);
        FragmentActivity activity = getActivity();
        if (activity == null) {
            return;
        }
        etherSendAmountEditText = view.findViewById(R.id.etherSendAmountEditText);
        continueButton.setOnClickListener(v -> {
            ConfirmFragment cf = new ConfirmFragment();

            String destination = destinationEditText.getText().toString();
            String amountInput = etherSendAmountEditText.getText().toString();
            BigInteger amountBigInt = new BigDecimal(amountInput).multiply(new BigDecimal("1000000000000000000")).toBigInteger();
            SendRequest sendRequest = new SendRequest("teth", amountBigInt.toString(), destination,  "000000", "passphrase", getNewMemoID());
            cf.setRequest(sendRequest);
            cf.setExchangeRate(exchangeRate);
            activity.getSupportFragmentManager().beginTransaction().replace(R.id.frame_layout, cf).addToBackStack(null).commit();
        });

        ArrayAdapter<String> adapter = new ArrayAdapter<>(activity, android.R.layout.simple_list_item_1);
        adapter.addAll("liraz", "eli");
        guardiansListView.setAdapter(adapter);
        etherSendAmountEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                try {
                    setDollarEquivalent();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });


    }

    private void setDollarEquivalent() {
        View view = getView();
        FragmentActivity activity = getActivity();
        if (activity == null || view == null || exchangeRate == null) {
            return;
        }
        double etherDouble = Double.parseDouble(etherSendAmountEditText.getText().toString());
        TextView dollarEquivalent = view.findViewById(R.id.dollarEquivalent);
        dollarEquivalent.setText(String.format(Locale.US, "$%.2f", etherDouble * exchangeRate.average24h));
    }

    /**
     * The bitgo IDs are not reliable and not visible to the guardians.
     * They require a way to tell similar transactions apart easily.
     *
     * @return a string that will be used as an ID by both client and guardians.
     * It will be saved in 'comment' field of the transaction.
     */
    private String getNewMemoID() {
        int memoId = 100000 + new Random().nextInt(899999);
        return "TXID" + memoId;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        destinationEditText.setText(data.getStringExtra("apiKey"));
    }
}
