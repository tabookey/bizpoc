package com.tabookey.bizpoc;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.fingerprint.FingerprintManager;
import android.os.CancellationSignal;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyPermanentlyInvalidatedException;
import android.security.keystore.KeyProperties;
import android.support.annotation.NonNull;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;

class SecretStorge {


    static final String PREFS_API_KEY_ENCODED = "api_key_encoded";
    static final String PREFS_PASSWORD_ENCODED = "passphrase_encoded";
    private static final String KEY_STORE_ID = "AndroidKeyStore";
    private static final String KEY_PAIR_ALIAS = "MyKeyPair";

    static SharedPreferences getPrefs(Activity activity) {
        return activity.getSharedPreferences(
                "com.example.app", Context.MODE_PRIVATE);
    }

    private static PrivateKey getPrivateKey() {
        KeyStore keyStore = getKeyStore();
        try {
            return (PrivateKey) keyStore.getKey(KEY_PAIR_ALIAS, null);
        } catch (KeyStoreException | NoSuchAlgorithmException | UnrecoverableKeyException e) {
            throw new RuntimeException(e);
        }
    }

    private PublicKey getPublicKey() {
        KeyStore keyStore = getKeyStore();
        Certificate certificate;
        try {
            certificate = keyStore.getCertificate(KEY_PAIR_ALIAS);
        } catch (KeyStoreException e) {
            throw new RuntimeException(e);
        }
        if (certificate == null) {
            throw new RuntimeException("Key pair not found");
        }
        PublicKey publicKey = certificate.getPublicKey();

        // This conversion is currently needed on API Level 23 (Android M) due to a platform bug which prevents the
        // use of Android Keystore public keys when their private keys require user authentication. This conversion
        // creates a new public key which is not backed by Android Keystore and thus is not affected by the bug.
        // See https://developer.android.com/reference/android/security/keystore/KeyGenParameterSpec.html
        try {
            KeyFactory keyFactory = KeyFactory.getInstance(publicKey.getAlgorithm());
            return keyFactory.generatePublic(new X509EncodedKeySpec(publicKey.getEncoded()));
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new RuntimeException("Failed to copy public key.", e);
        }
    }

    private static KeyStore getKeyStore() {
        try {
            KeyStore keyStore = KeyStore.getInstance(KEY_STORE_ID);
            keyStore.load(null);
            if (!keyStore.containsAlias(KEY_PAIR_ALIAS)) {
                resetKeyPair();
            }
            return keyStore;
        } catch (KeyStoreException | NoSuchAlgorithmException | CertificateException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void resetKeyPair() {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA, KEY_STORE_ID);
            keyPairGenerator.initialize(new KeyGenParameterSpec.Builder(KEY_PAIR_ALIAS, KeyProperties.PURPOSE_DECRYPT)
                    .setDigests(KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA512)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_OAEP)
                    .setUserAuthenticationRequired(true).build());
            keyPairGenerator.generateKeyPair();
        } catch (NoSuchAlgorithmException | NoSuchProviderException | InvalidAlgorithmParameterException e) {
            throw new RuntimeException("Could not generate key pair", e);
        }
    }

    byte[] encrypt(byte[] input) throws KeyPermanentlyInvalidatedException, BadPaddingException, IllegalBlockSizeException {
        Cipher cipher = getCipher(Cipher.ENCRYPT_MODE, getPublicKey());
        return cipher.doFinal(input);
    }

    public interface Callback {
        void done(byte[] result);
    }

    static CancellationSignal decrypt(Context context, final byte[] input, final Callback callback) throws KeyPermanentlyInvalidatedException {
        CancellationSignal cancellationSignal = new CancellationSignal();
        FingerprintManager fingerprintManager = (FingerprintManager) context.getSystemService(Context.FINGERPRINT_SERVICE);
        FingerprintManager.CryptoObject cryptoObject = getCryptoObject();
        fingerprintManager.authenticate(cryptoObject, cancellationSignal, 0, new FingerprintManager.AuthenticationCallback() {
            @Override
            public void onAuthenticationSucceeded(FingerprintManager.AuthenticationResult result) {
                Cipher cipher = result.getCryptoObject().getCipher();
                byte[] output;
                try {
                    output = cipher.doFinal(input);
                } catch (IllegalBlockSizeException | BadPaddingException e) {
                    throw new RuntimeException(e);
                }
                callback.done(output);
            }
            // Override other methods as well
        }, null);
        return cancellationSignal;
    }

    @NonNull
    static FingerprintManager.CryptoObject getCryptoObject() throws KeyPermanentlyInvalidatedException {
        return new FingerprintManager.CryptoObject(getCipher(Cipher.DECRYPT_MODE, getPrivateKey()));
    }

    private static Cipher getCipher(int mode, Key key) throws KeyPermanentlyInvalidatedException {
        Cipher cipher;
        try {
            cipher = Cipher.getInstance(KeyProperties.KEY_ALGORITHM_RSA + "/" + KeyProperties.BLOCK_MODE_ECB + "/" + "OAEPWithSHA-256AndMGF1Padding");
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            throw new RuntimeException(e);
        }
        OAEPParameterSpec algorithmParameterSpec = new OAEPParameterSpec("SHA-256", "MGF1", MGF1ParameterSpec.SHA1, PSource.PSpecified.DEFAULT);
        try {
            cipher.init(mode, key, algorithmParameterSpec);
        } catch (KeyPermanentlyInvalidatedException e) {
            // The key pair has been invalidated!
            throw e;
        } catch (InvalidKeyException | InvalidAlgorithmParameterException e) {
            throw new RuntimeException(e);
        }
        return cipher;
    }

    static byte[] getEncryptedBytes(String encryptedApiKey) {
        String[] split = encryptedApiKey.substring(1, encryptedApiKey.length() - 1).split(", ");
        byte[] array = new byte[split.length];
        for (int i = 0; i < split.length; i++) {
            array[i] = Byte.parseByte(split[i]);
        }
        return array;
    }
}
