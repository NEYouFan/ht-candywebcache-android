package com.netease.hearttouch.candywebcache.cachemanager;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;

/**
 * Created by netease on 16/6/27.
 */
public class DESUtils {
    private final static String DES = "DES";

    private static SecretKey generateKey(byte[] passwd) {
        try {
            DESKeySpec desKeySpec = new DESKeySpec(passwd);
            SecretKeyFactory keyFactory = SecretKeyFactory.getInstance(DES);
            SecretKey secretKey = keyFactory.generateSecret(desKeySpec);
            return secretKey;
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static byte[] encrypt(byte[] dataSrc, byte[] passwd) {
        Cipher cipher = null;
        try {
            SecureRandom random = new SecureRandom();
            cipher = Cipher.getInstance(DES);
            cipher.init(Cipher.ENCRYPT_MODE, generateKey(passwd), random);
            return cipher.doFinal(dataSrc);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     *
     * @param dataSrc 密码
     * @param passwd 加密字符串
     * @return
     */
    public final static String encrypt(String dataSrc, String passwd) {
        try {
            return byte2String(encrypt(dataSrc.getBytes(), passwd.getBytes()));
        } catch (Exception e) {
        }
        return null;
    }

    private static String byte2String(byte[] b) {
        String hs = "";
        String stmp = "";
        for (int n = 0; n < b.length; n++) {
            stmp = (java.lang.Integer.toHexString(b[n] & 0XFF));
            if (stmp.length() == 1)
                hs = hs + "0" + stmp;
            else
                hs = hs + stmp;
        }
        return hs.toUpperCase();
    }

    /**
     *
     * @param dataSrc 数据源
     * @param passwd 密钥，长度必须是8的倍数
     * @return
     * @throws Exception
     */
    public static byte[] decrypt(byte[] dataSrc, byte[] passwd) {
        Cipher cipher = null;
        try {
            cipher = Cipher.getInstance(DES);
            cipher.init(Cipher.DECRYPT_MODE, generateKey(passwd));
            return cipher.doFinal(dataSrc);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        }
        return null;
    }

    public final static String decrypt(String dataSrc, String passwd) {
        try {
            return new String(decrypt(String2byte(dataSrc.getBytes()), passwd.getBytes()));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private static byte[] String2byte(byte[] b) {
        if ((b.length % 2) != 0)
            throw new IllegalArgumentException("长度不是偶数");
        byte[] b2 = new byte[b.length / 2];
        for (int n = 0; n < b.length; n += 2) {
            String item = new String(b, n, 2);
            b2[n / 2] = (byte) Integer.parseInt(item, 16);
        }
        return b2;
    }
}
