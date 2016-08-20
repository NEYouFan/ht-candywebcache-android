package com.netease.hearttouch.candywebcache.cachemanager;

import java.io.UnsupportedEncodingException;

/**
 * Created by netease on 16/6/28.
 */
public class EnDecryptionUtils {
    private static final String PASSWORD = "12344321";
    public static String encode(String dataSrc) {
        String encryptedStr = null;
        try {
            encryptedStr = Base64.encode(DESUtils.encrypt(dataSrc.getBytes(),  PASSWORD.getBytes()));
        } catch (UnsupportedEncodingException e) {
        }
        return encryptedStr;
    }

    public static String decode(String encryptedStr) {
        String decryptedStr = "";
        try {
            byte[] decryptedData = Base64.decode(encryptedStr);
            decryptedStr = new String(DESUtils.decrypt(decryptedData, PASSWORD.getBytes()), "UTF-8");
        } catch (UnsupportedEncodingException e) {
        } catch (Exception e) {
            e.printStackTrace();
        }
        return decryptedStr;
    }


}
