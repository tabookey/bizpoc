package com.tabookey.bizpoc;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Paint;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.tabookey.bizpoc.api.BitgoUser;
import com.tabookey.bizpoc.api.ExchangeRate;
import com.tabookey.bizpoc.api.Global;
import com.tabookey.bizpoc.api.IBitgoWallet;
import com.tabookey.bizpoc.api.SendRequest;
import com.tabookey.bizpoc.api.TokenInfo;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import static android.content.ClipDescription.MIMETYPE_TEXT_PLAIN;

public class SendFragment extends Fragment {

    public List<BalancesAdapter.Balance> balances;
    EditText etherSendAmountEditText;
    ExchangeRate exchangeRate;
    EditText destinationEditText;
    List<BitgoUser> guardians;
    IBitgoWallet mBitgoWallet;
    private AppCompatActivity mActivity;

    TextView amountRequiredNote;
    TextView destinationRequiredNote;
    TextView balanceTextView;
    double maximumTransferValue = 0;
    TokenInfo selectedToken = Global.ent.getTokens().get("teth");

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.send_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Button continueButton = view.findViewById(R.id.continueButton);
        destinationEditText = view.findViewById(R.id.destinationEditText);

        amountRequiredNote = view.findViewById(R.id.amountRequiredNote);
        destinationRequiredNote = view.findViewById(R.id.destinationRequiredNote);
        balanceTextView = view.findViewById(R.id.balanceTextView);
        updateTokenBalance();

        Button selectCoinButton = view.findViewById(R.id.selectCoinButton);
        selectCoinButton.setOnClickListener(v -> {
            List<TokenInfo> collect = balances.stream().map(b -> b.tokenInfo).collect(Collectors.toList());
            CryptoCurrencySpinnerAdapter cryptoCurrencySpinnerAdapter = new CryptoCurrencySpinnerAdapter(mActivity, collect);
            new AlertDialog.Builder(mActivity)
                    .setTitle("Select token")
                    .setAdapter(cryptoCurrencySpinnerAdapter, (dialog, index) -> {
                        selectedToken = collect.get(index);
                        selectCoinButton.setText(selectedToken.getTokenCode().toUpperCase());
                        updateTokenBalance();
                    })
                    .create().show();
        });
        Button scanDestinationButton = view.findViewById(R.id.scanDestinationButton);
        Button pasteDestinationButton = view.findViewById(R.id.pasteDestinationButton);
        pasteDestinationButton.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) mActivity.getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard != null
                    && clipboard.hasPrimaryClip()
                    && clipboard.getPrimaryClip() != null
                    && clipboard.getPrimaryClipDescription() != null
                    && clipboard.getPrimaryClipDescription().hasMimeType(MIMETYPE_TEXT_PLAIN)) {
                ClipData.Item item = clipboard.getPrimaryClip().getItemAt(0);
                String pasteData = item.getText().toString();
                destinationEditText.setText(pasteData);
                Toast.makeText(mActivity, "Destination address pasted", Toast.LENGTH_LONG).show();
            }
        });
        scanDestinationButton.setPaintFlags(scanDestinationButton.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        pasteDestinationButton.setPaintFlags(pasteDestinationButton.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        scanDestinationButton.setOnClickListener(v -> startActivityForResult(new Intent(mActivity, ScanActivity.class), 1));
        ListView guardiansListView = view.findViewById(R.id.guardiansListView);
        etherSendAmountEditText = view.findViewById(R.id.etherSendAmountEditText);
        etherSendAmountEditText.setPaintFlags(etherSendAmountEditText.getPaintFlags() & (~Paint.UNDERLINE_TEXT_FLAG));
        continueButton.setOnClickListener(v -> moveToContinue());

        ArrayAdapter<String> adapter = new ArrayAdapter<>(mActivity, android.R.layout.simple_list_item_1);

        for (BitgoUser guardian : guardians) {
            adapter.add(guardian.name);
        }
        guardiansListView.setAdapter(adapter);
        etherSendAmountEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int start, int count, int after) {
                try {
                    setDollarEquivalent();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (editable.toString().equals(".")) {
                    etherSendAmountEditText.setText("0.");
                    etherSendAmountEditText.setSelection(etherSendAmountEditText.getText().length());
                }
            }
        });
    }

    private void updateTokenBalance() {
        for (BalancesAdapter.Balance b :
                balances) {
            if (b.coinName.toLowerCase().equals(selectedToken.getTokenCode())) {
                maximumTransferValue = b.getValue();
                balanceTextView.setText(String.format(Locale.US, "Maximum: %.6f %s", maximumTransferValue, b.coinName.toUpperCase()));
                break;
            }
        }
    }

    private void moveToContinue() {
        if (!isEnteredValueValid()) {
            return;
        }
        ConfirmFragment cf = new ConfirmFragment();
        String destination = destinationEditText.getText().toString();
        String amountInput = etherSendAmountEditText.getText().toString();
        BigInteger amountBigInt = new BigDecimal(amountInput).multiply(new BigDecimal(selectedToken.decimalPlaces)).toBigInteger();
        SendRequest sendRequest = new SendRequest(selectedToken.getTokenCode(), amountBigInt.toString(), destination, null, null, null);
        cf.setRequest(sendRequest);
        cf.exchangeRate = exchangeRate;
        cf.guardians = guardians;
        cf.mBitgoWallet = mBitgoWallet;
        mActivity.getSupportFragmentManager().beginTransaction().replace(R.id.frame_layout, cf).addToBackStack(null).commit();
    }

    private boolean isEnteredValueValid() {
        amountRequiredNote.setText("Required");
        destinationRequiredNote.setText("Required");
        String amountString = etherSendAmountEditText.getText().toString();
        boolean isAmountValid = amountString.length() != 0;
        try {
            double v = Double.parseDouble(amountString);
            if (v > maximumTransferValue) {
                amountRequiredNote.setText("Larger than balance");
                isAmountValid = false;
            }
        } catch (Exception e) {
            isAmountValid = false;
        }
        String destination = destinationEditText.getText().toString();
        boolean isDestinationValid = AddressChecker.isValidAddress(destination);
        boolean isDestinationChecksummed = AddressChecker.isCheckedAddress(destination);
        if (!isDestinationValid) {
            destinationRequiredNote.setText("Not a valid address");
        } else if (!isDestinationChecksummed) {
            AlertDialog dialog = new AlertDialog.Builder(mActivity).create();
            dialog.setTitle("The address is not checksummed");
            dialog.setMessage("This address you have inserted does not seem to have a correct checksum. " +
                    "This can indicate you got the address from a client  that does not support EIP-55 style checksum, " +
                    "or that the address is invalid. Proceed on your own discretion.");
            dialog.setButton(DialogInterface.BUTTON_NEGATIVE, "I understand", (d, w) -> d.dismiss());
            dialog.show();
        }
        amountRequiredNote.setVisibility(isAmountValid ? View.GONE : View.VISIBLE);
        destinationRequiredNote.setVisibility(isDestinationValid ? View.GONE : View.VISIBLE);
        return isAmountValid && isDestinationValid;
    }

    private void setDollarEquivalent() {
        View view = getView();
        if (view == null || exchangeRate == null) {
            return;
        }
        TextView dollarEquivalent = view.findViewById(R.id.dollarEquivalent);
        String etherAmount = etherSendAmountEditText.getText().toString();
        if (etherAmount.length() == 0) {
            dollarEquivalent.setText("0.00 USD");
        }
        double etherDouble = Double.parseDouble(etherAmount);
        dollarEquivalent.setText(String.format(Locale.US, "$%.2f", etherDouble * exchangeRate.average24h));
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof Activity) {
            mActivity = (AppCompatActivity) context;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            String destAddress = data.getStringExtra(ScanActivity.SCANNED_STRING_EXTRA);
            destAddress = destAddress.replaceAll("ethereum:", ""); // MetaMask format
            destinationEditText.setText(destAddress);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                mActivity.onBackPressed();
        }
        return super.onOptionsItemSelected(item);
    }
}
