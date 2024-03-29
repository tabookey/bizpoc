package com.tabookey.bizpoc;

import android.Manifest;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.tabookey.bizpoc.api.ExchangeRate;
import com.tabookey.bizpoc.api.Global;
import com.tabookey.bizpoc.api.SendRequest;
import com.tabookey.bizpoc.api.TokenInfo;
import com.tabookey.bizpoc.impl.Utils;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import static android.content.ClipDescription.MIMETYPE_TEXT_PLAIN;

public class SendFragment extends Fragment {

    EditText tokenSendAmountEditText;
    EditText dollarEquivalent;
    EditText destinationEditText;
    private AppCompatActivity mActivity;

    TextView amountRequiredNote;
    TextView destinationRequiredNote;
    Button maximumAmountButton;
    double maximumTransferValue = 0;
    TokenInfo selectedToken = Global.ent.getBaseCoin();
    boolean didTryToSubmit = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.send_fragment, container, false);
    }

    boolean isEthChange = false;
    boolean isUsdChange = false;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Button continueButton = view.findViewById(R.id.continueButton);
        destinationEditText = view.findViewById(R.id.destinationEditText);

        amountRequiredNote = view.findViewById(R.id.amountRequiredNote);
        destinationRequiredNote = view.findViewById(R.id.destinationRequiredNote);
        maximumAmountButton = view.findViewById(R.id.maximumAmountButton);
        updateTokenBalance();

        Button selectCoinButton = view.findViewById(R.id.selectCoinButton);
        selectCoinButton.setText(selectedToken.getTokenCode().toUpperCase());
        selectCoinButton.setOnClickListener(v -> {
            List<TokenInfo> collect = Global.sBalances.stream().map(b -> b.tokenInfo).collect(Collectors.toList());
            CryptoCurrencySpinnerAdapter cryptoCurrencySpinnerAdapter = new CryptoCurrencySpinnerAdapter(mActivity, collect);
            new AlertDialog.Builder(mActivity)
                    .setTitle("Select asset")
                    .setAdapter(cryptoCurrencySpinnerAdapter, (dialog, index) -> {
                        selectedToken = collect.get(index);
                        selectCoinButton.setText(selectedToken.getTokenCode().toUpperCase());
                        updateTokenBalance();
                        setDollarEquivalent();
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
            }
        });
        scanDestinationButton.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(mActivity, Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(mActivity, new String[]{Manifest.permission.CAMERA}, 11);
                return;
            }
            startActivityForResult(new Intent(mActivity, ScanActivity.class), 1);
        });
        tokenSendAmountEditText = view.findViewById(R.id.tokenSendAmountEditText);
        dollarEquivalent = view.findViewById(R.id.dollarEquivalent);
        tokenSendAmountEditText.setPaintFlags(tokenSendAmountEditText.getPaintFlags() & (~Paint.UNDERLINE_TEXT_FLAG));
        continueButton.setOnClickListener(v -> {
            didTryToSubmit = true;
            if (!isEnteredValueValid(true)) {
                return;
            }
            moveToContinue();
        });

        tokenSendAmountEditText.addTextChangedListener(new TextWatcher() {
            private boolean skipNextTextChanged = false;

            @Override
            public void beforeTextChanged(CharSequence charSequence, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int start, int before, int count) {
                if (count <= 0) {
                    skipNextTextChanged = true;
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (didTryToSubmit) {
                    isEnteredValueValid(false);
                }
                String string = editable.toString();
                if (string.equals(".")) {
                    tokenSendAmountEditText.setText("0.");
                    tokenSendAmountEditText.setSelection(tokenSendAmountEditText.getText().length());
                    return;
                }
                if (string.contains(".") && !skipNextTextChanged && !isUsdChange) {
                    try {

                        String[] su = string.split("\\.");
                        if (su.length == 2 && su[1].length() >= selectedToken.decimalPlaces) {
                            skipNextTextChanged = true;
                            tokenSendAmountEditText.setText(su[0] + "." + su[1].substring(0, selectedToken.decimalPlaces));
                        }
                        tokenSendAmountEditText.setSelection(tokenSendAmountEditText.getText().length());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    skipNextTextChanged = false;
                }

                try {
                    isEthChange = true;
                    if (!isUsdChange) {
                        setDollarEquivalent();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                isUsdChange = false;
            }
        });
        dollarEquivalent.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (didTryToSubmit) {
                    isEnteredValueValid(false);
                }
                if (editable.toString().equals(".")) {
                    dollarEquivalent.setText("0.");
                    dollarEquivalent.setSelection(dollarEquivalent.getText().length());
                }
                try {
                    isUsdChange = true;
                    if (!isEthChange) {
                        setCoinEquivalent();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                isEthChange = false;
            }
        });
        destinationEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (didTryToSubmit) {
                    isEnteredValueValid(false);
                }
            }
        });
    }

    private void updateTokenBalance() {
        for (Balance b : Global.sBalances) {
            if (b.coinName.toLowerCase().equals(selectedToken.getTokenCode())) {
                maximumTransferValue = b.getValue();
                maximumAmountButton.setOnClickListener(v ->
                        {
                            BigDecimal maxAmountDec = new BigDecimal(maximumTransferValue);
                            BigDecimal roundOff = maxAmountDec.setScale(getTokenDecimalOurs(), BigDecimal.ROUND_DOWN);
                            String amountText = roundOff.toPlainString();
                            tokenSendAmountEditText.setText(amountText);
                        }
                );
                break;
            }
        }
    }

    private void moveToContinue() {
        ConfirmFragment cf = new ConfirmFragment();
        String destination = destinationEditText.getText().toString();
        String amountInput = tokenSendAmountEditText.getText().toString();
        BigInteger amountBigInt = Utils.doubleStringToBigInteger(amountInput, selectedToken.decimalPlaces);
        SendRequest sendRequest = new SendRequest(selectedToken, amountBigInt.toString(), destination, null, null, null);
        cf.setRequest(sendRequest);
        mActivity.getSupportFragmentManager().beginTransaction().replace(R.id.frame_layout, cf).addToBackStack(null).commit();
    }

    private boolean isEnteredValueValid(boolean showDialog) {
        amountRequiredNote.setText(R.string.please_enter_amount);
        destinationRequiredNote.setText(R.string.please_enter_address);
        String amountString = tokenSendAmountEditText.getText().toString();
        boolean isAmountValid = amountString.length() != 0;
        try {
            double v = Double.parseDouble(amountString);
            if (v > maximumTransferValue) {
                amountRequiredNote.setText(R.string.larger_than_balance);
                isAmountValid = false;
            }
        } catch (Exception e) {
            isAmountValid = false;
        }


        amountRequiredNote.setVisibility(isAmountValid ? View.GONE : View.VISIBLE);
        highlightEditText(tokenSendAmountEditText, !isAmountValid);

        String destination = destinationEditText.getText().toString();
        boolean isDestinationEntered = destination.length() != 0;
        if (!isDestinationEntered) {
            highlightEditText(destinationEditText, true);
            destinationRequiredNote.setVisibility(View.VISIBLE);
            destinationRequiredNote.setText(R.string.please_enter_address);
            return false;
        }
        boolean isDestinationValid = AddressChecker.isValidAddress(destination);
        // Ban sending to self
        isDestinationValid &= !destination.toLowerCase().equals(Global.sBitgoWallet.getAddress());
        boolean isDestinationChecksummed = AddressChecker.isCheckedAddress(destination);
        if (!isDestinationValid) {
            destinationRequiredNote.setText(R.string.not_valid_address);
        } else if (!isDestinationChecksummed && showDialog && isAmountValid) {
            AlertDialog dialog = new AlertDialog.Builder(mActivity).create();
            dialog.setTitle("Please verify the address");
            dialog.setMessage("The address you have entered might be invalid, due to:\n" +
                    "- Incorrect checksum\n" +
                    "- Incorrect address copy\n\n" +
                    "Please double check to make sure that this address is correct or proceed at " +
                    "your own discretion\n\n");
            dialog.setButton(DialogInterface.BUTTON_POSITIVE, "   It's OK   ", (d, w) -> {
                moveToContinue();
                d.dismiss();
            });
            dialog.setButton(DialogInterface.BUTTON_NEGATIVE, "   Go back   ", (d, w) -> d.dismiss());
            dialog.show();

            Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            Button negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);

            int pL = positiveButton.getPaddingLeft();
            int pT = positiveButton.getPaddingTop();
            int pR = positiveButton.getPaddingRight();
            int pB = positiveButton.getPaddingBottom();

            positiveButton.setBackgroundResource(R.drawable.custom_button);
            positiveButton.setPadding(pL, pT, pR, pB);

            positiveButton.setAllCaps(false);
            positiveButton.setTextColor(mActivity.getColor(android.R.color.white));


            negativeButton.setAllCaps(false);
            negativeButton.setTextColor(mActivity.getColor(R.color.text_color));
        }
        highlightEditText(destinationEditText, !isDestinationValid);
        destinationRequiredNote.setVisibility(isDestinationValid ? View.GONE : View.VISIBLE);
        return isAmountValid && isDestinationValid && isDestinationChecksummed;
    }

    private void highlightEditText(EditText editText, boolean on) {
        if (on) {
            editText.getBackground().setColorFilter(getResources().getColor(R.color.error_red, null), PorterDuff.Mode.SRC_ATOP);
        } else {
            editText.getBackground().clearColorFilter();
        }
    }


    private void setDollarEquivalent() {
        View view = getView();
        ExchangeRate exchangeRate = Global.sExchangeRates.get(selectedToken.type);
        if (view == null || exchangeRate == null) {
            return;
        }
        String tokenAmount = tokenSendAmountEditText.getText().toString();
        if (tokenAmount.length() == 0) {
            dollarEquivalent.setText(R.string.zero_zero_zero);
            return;
        }
        double tokenDouble = Double.parseDouble(tokenAmount);
        dollarEquivalent.setText(String.format(Locale.US, "%.2f", tokenDouble * exchangeRate.average24h));
    }

    private void setCoinEquivalent() {
        View view = getView();
        ExchangeRate exchangeRate = Global.sExchangeRates.get(selectedToken.type);
        if (view == null || exchangeRate == null) {
            return;
        }
        String dollarAmount = dollarEquivalent.getText().toString();
        if (dollarAmount.length() == 0) {
            tokenSendAmountEditText.setText(String.format(Locale.US, getTokenFormat(), 0));
            return;
        }
        double dollarDouble = Double.parseDouble(dollarAmount);
        tokenSendAmountEditText.setText(String.format(Locale.US, getTokenFormat(), dollarDouble / exchangeRate.average24h));
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof Activity) {
            mActivity = (AppCompatActivity) context;
            ActionBar supportActionBar = mActivity.getSupportActionBar();
            if (supportActionBar == null) {
                return;
            }
            supportActionBar.setTitle("Send");
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
        if (item.getItemId() == android.R.id.home) {
            mActivity.onBackPressed();
        }
        return super.onOptionsItemSelected(item);
    }

    private int getTokenDecimalOurs() {
        if (selectedToken.decimalPlaces >= 3) {
            return 3;
        }
        return selectedToken.decimalPlaces;
    }

    private String getTokenFormat() {
        switch (selectedToken.decimalPlaces) {
            case 0:
                return "%.0f";
            case 1:
                return "%.1f";
            case 2:
                return "%.2f";
            case 3:
            default:
                return "%.3f";
        }
    }
}
