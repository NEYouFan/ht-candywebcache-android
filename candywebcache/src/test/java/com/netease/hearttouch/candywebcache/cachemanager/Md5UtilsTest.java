package com.netease.hearttouch.candywebcache.cachemanager;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Created by netease on 16/6/16.
 */
public class Md5UtilsTest {

    @Test
    public void testCalculateMd5() throws Exception {
        String filePath1 = "/Users/netease/webcacheunittestfiles/web_app_old.zip";
        String md5_1 = "1ab73685e5fbb4269d5c1432c232a574";
        assertEquals(md5_1, Md5Utils.calculateMd5(filePath1));

        String filePath2 = "/Users/netease/webcacheunittestfiles/web_app_new.zip";
        String md5_2 = "dd859ff9180e292ada81e53da5b46e71";
        assertEquals(md5_2, Md5Utils.calculateMd5(filePath2));
    }

    @Test
    public void testCheckFileValidation() throws Exception {
        String filePath1 = "/Users/netease/webcacheunittestfiles/web_app_old.zip";
        String md5_1 = "1ab73685e5fbb4269d5c1432c232a574";
        assertTrue(Md5Utils.checkFileValidation(filePath1, md5_1));

        String filePath2 = "/Users/netease/webcacheunittestfiles/web_app_new.zip";
        String md5_2 = "dd859ff9180e292ada81e53da5b46e71";
        assertTrue(Md5Utils.checkFileValidation(filePath2, md5_2));
    }
}