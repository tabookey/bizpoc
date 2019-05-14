package com.tabookey.bizpoc.utils;

import android.app.Activity;
import android.util.Log;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.safetynet.SafetyNet;
import com.google.android.gms.safetynet.SafetyNetApi;
import com.google.android.gms.safetynet.SafetyNetClient;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.tabookey.bizpoc.BuildConfig;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class SafetyNetHelper implements SafetynetHelperInterface {

    private static final String TAG = "SafetyNetHelper";

    private final Random mRandom = new SecureRandom();

    private String mResult;


    @Override
    public void sendSafetyNetRequest(Activity activity, byte[] nonce, OnSuccessParsedListener successListener, OnFailureListener failureListener) {
        Log.i(TAG, "Sending SafetyNet API request.");

         /*
        Create a nonce for this request.
        The nonce is returned as part of the response from the
        SafetyNet API. Here we append the string to a number of random bytes to ensure it larger
        than the minimum 16 bytes required.
        Read out this value and verify it against the original request to ensure the
        response is correct and genuine.
        NOTE: A nonce must only be used once and a different nonce should be used for each request.
        As a more secure option, you can obtain a nonce from your own server using a secure
        connection. Here in this sample, we generate a String and append random bytes, which is not
        very secure. Follow the tips on the Security Tips page for more information:
        https://developer.android.com/training/articles/security-tips.html#Crypto
         */
        // TODO(developer): Change the nonce generation to include your own, used once value,
        // ideally from your remote server.
        if (nonce == null) {
            Log.w(TAG, "Will create a local SafetyNet nonce");
            String nonceData = "Safety Net Sample: " + System.currentTimeMillis();
            nonce = getRequestNonce(nonceData);
        }
        if (nonce == null) {
            throw new RuntimeException("How is this even possible? Lint wont stop complaining.");
        }
        /*
         Call the SafetyNet API asynchronously.
         The result is returned through the success or failure listeners.
         First, get a SafetyNetClient for the foreground Activity.
         Next, make the call to the attestation API. The API key is specified in the gradle build
         configuration and read from the gradle.properties file.
         */
        SafetyNetClient client = SafetyNet.getClient(activity);
        Task<SafetyNetApi.AttestationResponse> task = client.attest(nonce, BuildConfig.API_KEY);

        task.addOnSuccessListener(activity, attestationResponse -> {
         /*
                     Successfully communicated with SafetyNet API.
                     Use result.getJwsResult() to get the signed result data. See the server
                     component of this sample for details on how to verify and parse this result.
                     */
            mResult = attestationResponse.getJwsResult();
            Log.d(TAG, "Success! SafetyNet result:\n" + mResult + "\n");

            SafetyNetResponse response = SafetyNetResponse.parseJsonWebSignature(mResult);
            successListener.onSuccess(response);
            /*
             TODO(developer): Forward this result to your server
            */
        });

        task.addOnFailureListener(activity, e -> {
            // An error occurred while communicating with the service.
            mResult = null;

            if (e instanceof ApiException) {
                // An error with the Google Play Services API contains some additional details.
                ApiException apiException = (ApiException) e;
                Log.d(TAG, "Error: " +
                        CommonStatusCodes.getStatusCodeString(apiException.getStatusCode()) + ": " +
                        apiException.getMessage());
            } else {
                // A different, unknown type of error occurred.
                Log.d(TAG, "ERROR! " + e.getMessage());
            }
            failureListener.onFailure(e);
        });

    }

    /**
     * There is little value in checking the attestation locally. Just a sanity check.
     */
    public static boolean isAttestationLookingGood(SafetyNetResponse safetyNetResponse) {
        boolean isPackageNameCorrect = BuildConfig.APPLICATION_ID.equals(safetyNetResponse.getApkPackageName());
        boolean isTimestampSane = Math.abs(safetyNetResponse.getTimestampMs() - System.currentTimeMillis()) < TimeUnit.HOURS.toMillis(1);
        return safetyNetResponse.isBasicIntegrity()
                && safetyNetResponse.isCtsProfileMatch()
                && isPackageNameCorrect
                && isTimestampSane;
    }

    /**
     * Generates a 16-byte nonce with additional data.
     * The nonce should also include additional information, such as a user id or any other details
     * you wish to bind to this attestation. Here you can provide a String that is included in the
     * nonce after 24 random bytes. During verification, extract this data again and check it
     * against the request that was made with this nonce.
     */
    private byte[] getRequestNonce(String data) {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        byte[] bytes = new byte[24];
        mRandom.nextBytes(bytes);
        try {
            byteStream.write(bytes);
            byteStream.write(data.getBytes());
        } catch (IOException e) {
            return null;
        }

        return byteStream.toByteArray();
    }


}
