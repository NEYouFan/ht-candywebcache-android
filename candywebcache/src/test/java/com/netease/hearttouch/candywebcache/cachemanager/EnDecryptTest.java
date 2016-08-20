package com.netease.hearttouch.candywebcache.cachemanager;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Created by netease on 16/6/27.
 */
public class EnDecryptTest {
    @Before
    public void setUp() throws Exception {

    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void testEnDecrypt() throws Exception {
        String dataSrc = "20398415cf372481131278f074119188";
        String passwd = "12344321";

        assertEquals("E13F4ECDDBB54551664F74AC76634BBAD8F5ACB35DFF99CA711C05169E990064996A020A8336A5E0", DESUtils.encrypt(dataSrc,  passwd));
        assertEquals("4T9Ozdu1RVFmT3SsdmNLutj1rLNd/5nKcRwFFp6ZAGSZagIKgzal4A==", Base64.encode(DESUtils.encrypt(dataSrc.getBytes(),  passwd.getBytes())));
        assertEquals("4T9Ozdu1RVFmT3SsdmNLutj1rLNd/5nKcRwFFp6ZAGSZagIKgzal4A==", EnDecryptionUtils.encode(dataSrc));

        dataSrc = "cbc2d2376ef405c6c49088706b294275";
        assertEquals("rjNJ5lUmD06tE4rWnohvd3ifijh9lXDkRTGRDzNwrn+ZagIKgzal4A==", EnDecryptionUtils.encode(dataSrc));
        assertEquals(dataSrc, EnDecryptionUtils.decode("rjNJ5lUmD06tE4rWnohvd3ifijh9lXDkRTGRDzNwrn+ZagIKgzal4A=="));

        dataSrc = "a2e4c9b7ae6beb31e68daca9d7ad6f4e";
        assertEquals("lQdR6sBdy+AIakJz7Pi53ZlIPWGEj1IDybvPm0RfGsKZagIKgzal4A==", EnDecryptionUtils.encode(dataSrc));
        assertEquals(dataSrc, EnDecryptionUtils.decode("lQdR6sBdy+AIakJz7Pi53ZlIPWGEj1IDybvPm0RfGsKZagIKgzal4A=="));

        dataSrc = "6e3ace2638be9fc855e7672964cf4d4c";
        assertEquals("kC/KhxC55iM83nDOCXxvtaSeEQ0Qf99pCDYD6+ZUVp+ZagIKgzal4A==", EnDecryptionUtils.encode(dataSrc));
        assertEquals(dataSrc, EnDecryptionUtils.decode("kC/KhxC55iM83nDOCXxvtaSeEQ0Qf99pCDYD6+ZUVp+ZagIKgzal4A=="));

        dataSrc = "836a47fe852fa5352322dbd6f2c9a813";
        assertEquals("ZuIMHTf3d+sS3C3dOK9UepD96EsChggc3XmVZhIn3MKZagIKgzal4A==", EnDecryptionUtils.encode(dataSrc));
        assertEquals(dataSrc, EnDecryptionUtils.decode("ZuIMHTf3d+sS3C3dOK9UepD96EsChggc3XmVZhIn3MKZagIKgzal4A=="));

        dataSrc = "32d32a0700b8067d0cc98a770a8a1c61";
        assertEquals("+a8HfZHo5447sQc3uhcGSTa6ZLa+LwXs4DzONPSTNz2ZagIKgzal4A==", EnDecryptionUtils.encode(dataSrc));
        assertEquals(dataSrc, EnDecryptionUtils.decode("+a8HfZHo5447sQc3uhcGSTa6ZLa+LwXs4DzONPSTNz2ZagIKgzal4A=="));

        dataSrc = "3244787f9fe856920a77a807a285e6b5";
        assertEquals("S1zVHQTTe5gDJphM1w9xS7RK+NRoaR42nJsE9o8jkzeZagIKgzal4A==", EnDecryptionUtils.encode(dataSrc));
        assertEquals(dataSrc, EnDecryptionUtils.decode("S1zVHQTTe5gDJphM1w9xS7RK+NRoaR42nJsE9o8jkzeZagIKgzal4A=="));

        String encryptedStr = "4T9Ozdu1RVFmT3SsdmNLutj1rLNd/5nKcRwFFp6ZAGSZagIKgzal4A==";
        assertEquals("e13f4ecddbb54551664f74ac76634bbad8f5acb35dff99ca711c05169e990064996a020a8336a5e0", Md5Utils.toHexString(Base64.decode(encryptedStr)));
        byte[] decryptedData = Base64.decode(encryptedStr);
        String md5 = new String(DESUtils.decrypt(decryptedData, passwd.getBytes()), "UTF-8");
        assertEquals("20398415cf372481131278f074119188", new String(DESUtils.decrypt(decryptedData, passwd.getBytes()), "UTF-8"));
    }
}