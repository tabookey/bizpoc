package com.tabookey.logs;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

class EncStream {
    //yes, our encryption is STO...
    static byte secret[] = "tbkLogFile@461.".getBytes();

    static boolean encrypt = true;
    static boolean compress = true;

    private static Cipher getCipher(SecretKeySpec key, boolean encrypt) {
        try {
            Cipher enc = Cipher.getInstance("AES");
            enc.init(encrypt ? Cipher.ENCRYPT_MODE : Cipher.DECRYPT_MODE, key);
            return enc;
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    static Cipher getAesCipher(byte[] secret, boolean encrypt) {
        byte[] secrethash;
        try {
            MessageDigest d = MessageDigest.getInstance("SHA-256");
            secrethash = d.digest(secret);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        SecretKeySpec key = new SecretKeySpec(secrethash, "AES");
        return getCipher(key, encrypt);
    }

    static OutputStream cipherOutput(OutputStream base) {
        return new CipherOutputStream(base, getAesCipher(secret, true));
    }

    static InputStream cipherInput(InputStream base) {
        return new CipherInputStream(base, getAesCipher(secret, false));
    }

    static InputStream openEncLog(String filename) throws IOException {
        InputStream in = new FileInputStream(filename);
        return openEncLog(in);
    }

    static InputStream openEncLog(InputStream in) throws IOException {
        if (encrypt)
            in = cipherInput(in);
        if (compress)
            in = new GZIPInputStream(in);
        return in;
    }

    static PrintStream createEncLog(String filename) throws IOException {
        OutputStream out = new FileOutputStream(filename);
        if (encrypt)
            out = cipherOutput(out);
        if (compress)
            out = new GZIPOutputStream(out);
        return new PrintStream(out);

    }
}
