package com.netease.hearttouch.candywebcache.cachemanager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Created by netease on 16/6/16.
 */
public class Md5Utils {
    private static char[] sHexChar = { '0', '1', '2', '3', '4', '5', '6', '7',
            '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

    public static String toHexString(byte[] b) {
        StringBuilder sb = new StringBuilder(b.length * 2);
        for (int i = 0; i < b.length; i++) {
            sb.append(sHexChar[(b[i] & 0xf0) >>> 4]);
            sb.append(sHexChar[b[i] & 0x0f]);
        }
        return sb.toString();
    }

    public static String calculateMd5(InputStream is) {
        String md5 = "";
        try {
            MessageDigest md5Digest = MessageDigest.getInstance("MD5");

            byte[] buffer = new byte[4096];
            int numRead;

            while ((numRead = is.read(buffer)) > 0) {
                md5Digest.update(buffer, 0, numRead);
            }
            byte[] digest = md5Digest.digest();
            md5 = toHexString(digest);
        } catch (NoSuchAlgorithmException e) {
        } catch (IOException e) {
        } finally {
            try {
                is.close();
            } catch (IOException e) {
            }
        }
        return md5;
    }

    public static String calculateMd5(File file) {
        FileInputStream fis = null;
        String md5 = "";
        try {
            fis = new FileInputStream(file);
            md5 = calculateMd5(fis);
        } catch (FileNotFoundException e) {
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                }
            }
        }
        return md5;
    }

    public static String calculateMd5(String filePath) {
        File file = new File(filePath);
        return calculateMd5(file);
    }

    public static boolean checkFileValidation(String filePath, String md5) {
        String strDigest = calculateMd5(filePath);
        if (strDigest.equalsIgnoreCase(md5)) {
            return true;
        }
        return false;
    }
}
