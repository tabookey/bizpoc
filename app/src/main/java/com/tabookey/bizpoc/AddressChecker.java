package com.tabookey.bizpoc;

import org.web3j.crypto.Hash;
import org.web3j.utils.Numeric;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AddressChecker {

    private static String checkedAddress(final String address) {
        final String cleanAddress = Numeric.cleanHexPrefix(address).toLowerCase();
        //
        StringBuilder o = new StringBuilder();
        String keccak = Hash.sha3String(cleanAddress);
        char[] checkChars = keccak.substring(2).toCharArray();

        char[] cs = cleanAddress.toLowerCase().toCharArray();
        for (int i = 0; i < cs.length; i++) {
            char c = cs[i];
            c = (Character.digit(checkChars[i], 16) & 0xFF) > 7 ? Character.toUpperCase(c) : Character.toLowerCase(c);
            o.append(c);
        }
        return Numeric.prependHexPrefix(o.toString());
    }


    static boolean isValidAddress(final String address) {
        return Pattern.compile("^0x[a-fA-F0-9]{40}$").matcher(address).matches();
    }
    static boolean isCheckedAddress(final String address) {
        return Numeric.prependHexPrefix(address).equals(checkedAddress(address));
    }

}