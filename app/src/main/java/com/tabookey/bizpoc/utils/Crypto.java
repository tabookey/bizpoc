package com.tabookey.bizpoc.utils;

import com.tabookey.logs.Log;

import org.bouncycastle.crypto.generators.SCrypt;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.nio.charset.Charset;
import java.security.spec.KeySpec;
import java.util.Arrays;
import java.util.Base64;
import java.util.Locale;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import androidx.annotation.NonNull;

import static com.tabookey.bizpoc.impl.Utils.fromJson;

public class Crypto {
    private final static String TAG = "scrypt";


    static class SjClData {
        public String iv;
        public int v, iter, ks, ts;
        public String mode, adata, cipher, salt, ct;
    }

    static private Base64.Decoder base64decoder = Base64.getDecoder();
    static private Base64.Encoder base64encoder = Base64.getEncoder();

    public static byte[] fromBase64(String s) {
        return base64decoder.decode(s);
    }

    public static String toBase64(byte[] b) {
        return new String(base64encoder.encode(b));
    }

    //from: https://blog.degering.name/posts/java-sjcl
    public static String decrypt(String encodedJSON, char[] password) throws Exception {

        SjClData j = fromJson(encodedJSON, SjClData.class);

        // We need the salt, the IV and the cipher text;
        // all of them need to be Base64 decoded
        byte[] salt = base64decoder.decode(j.salt);
        byte[] iv = base64decoder.decode(j.iv);
        byte[] cipherText = base64decoder.decode(j.ct);

        // Also, we need the keySize and the iteration count
        int keySize = j.ks, iterations = j.iter;

        // Now, SJCL doesn't use the whole IV in CCM mode;
        // the length L depends on the length of the cipher text and is
        // either 2 (< 32768 bit length),
        // 3 (< 8388608 bit length) or
        // 4 (everything larger).
        // c.f. https://github.com/bitwiseshiftleft/sjcl/blob/master/core/ccm.js#L60
        int lol = 2;
        if (cipherText.length >= 1 << 16) lol++;
        if (cipherText.length >= 1 << 24) lol++;

        // Cut the IV to the appropriate length, which is 15 - L
        iv = Arrays.copyOf(iv, 15 - lol);

        // Crypto stuff.
        // First, we need the secret AES key,
        // which is generated from password and salt
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        KeySpec spec = new PBEKeySpec(password,
                salt, iterations, keySize);
        SecretKey tmp = factory.generateSecret(spec);
        SecretKey secret = new SecretKeySpec(tmp.getEncoded(), "AES");

        // Now it's time to decrypt.
        Cipher cipher = Cipher.getInstance("AES/CCM/NoPadding",
                new BouncyCastleProvider());
        cipher.init(Cipher.DECRYPT_MODE, secret, new IvParameterSpec(iv));

        // Return the final result after converting it to a string.
        return new String(cipher.doFinal(cipherText));
    }

    @SuppressWarnings("WeakerAccess")
    public static class ScryptOptions {
        public String salt;
        public int N, r, p, dkLen;

        public ScryptOptions() {

        }

        public ScryptOptions(String salt, int n, int r, int p, int dkLen) {
            this.salt = salt;
            this.N = n;
            this.r = r;
            this.p = p;
            this.dkLen = dkLen;
        }

        @NonNull
        @Override
        public String toString() {
            return String.format(Locale.US, "salt=%s N=%d r=%d p=%d dkLen=%d", salt, N, r, p, dkLen);
        }

        public byte[] getSalt() {
            return salt.getBytes();
        }
    }

    public static byte[] scrypt(String password, ScryptOptions scryptOptions) {
        Log.v(TAG, String.format("start generation with parameters: %s", scryptOptions));
        byte[] generate = SCrypt.generate(password.getBytes(), scryptOptions.getSalt(), scryptOptions.N, scryptOptions.r, scryptOptions.p, scryptOptions.dkLen);
        Log.v(TAG, "end generation");
        return generate;
    }
}
