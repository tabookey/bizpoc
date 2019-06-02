package com.tabookey.bizpoc;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.hardware.fingerprint.FingerprintManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

public class WelcomeFragment extends Fragment {

    private AppCompatActivity mActivity;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.welcome_fragment, container, false);
        Button letsGoButton = view.findViewById(R.id.letsGoButton);
        Button sendEmailButton = view.findViewById(R.id.sendEmailButton);
        sendEmailButton.setOnClickListener(b -> {
            Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts(
                    "mailto", "support@tabookey.com", null));
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Support request");
            emailIntent.putExtra(Intent.EXTRA_TEXT, "");
            startActivity(Intent.createChooser(emailIntent, "Send email..."));
        });
        letsGoButton.setOnClickListener(b -> {
            FingerprintManager fingerprintManager = mActivity.getSystemService(FingerprintManager.class);
            if (fingerprintManager == null ||
                    !fingerprintManager.isHardwareDetected() ||
                    !fingerprintManager.hasEnrolledFingerprints()) {
                AlertDialog dialog = new AlertDialog.Builder(mActivity).create();
                dialog.setTitle("Fingerprint identification");
                dialog.setMessage("You must add fingerprint ID on your device before using your safe");
                dialog.setButton(DialogInterface.BUTTON_POSITIVE, "OK, go to settings", (d, w) -> {
                    startActivityForResult(new Intent(Settings.ACTION_SECURITY_SETTINGS), 0);
                    d.dismiss();
                });
                dialog.setButton(DialogInterface.BUTTON_NEGATIVE, "   Go back   ", (d, w) -> d.dismiss());

                dialog.show();
                Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);

                int pL = positiveButton.getPaddingLeft();
                int pT = positiveButton.getPaddingTop();
                int pR = positiveButton.getPaddingRight();
                int pB = positiveButton.getPaddingBottom();

                positiveButton.setBackgroundResource(R.drawable.custom_button);
                positiveButton.setPadding(pL, pT, pR, pB);

                positiveButton.setAllCaps(false);
                dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setAllCaps(false);
                positiveButton.setTextColor(mActivity.getColor(android.R.color.white));
                return;
            }
            ImportApiKeyFragment f = new ImportApiKeyFragment();
            mActivity.getSupportFragmentManager().beginTransaction().replace(R.id.frame_layout, f).commit();
        });
        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof Activity) {
            mActivity = (AppCompatActivity) context;
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }
}