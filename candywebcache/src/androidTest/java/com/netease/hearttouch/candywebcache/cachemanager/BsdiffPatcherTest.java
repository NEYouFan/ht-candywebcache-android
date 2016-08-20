package com.netease.hearttouch.candywebcache.cachemanager;

import android.test.InstrumentationTestCase;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.*;

/**
 * Created by netease on 16/6/13.
 */
public class BsdiffPatcherTest extends InstrumentationTestCase {
    static {
        System.loadLibrary("patcher");
    }
    @Before
    public void setUp() throws Exception {

    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void testApplyPatch() throws Exception {
        BsdiffPatcher patcher = new BsdiffPatcher();
        String oldFilePath = "/sdcard/webcachetestfiles/kaola_20160827.zip";
        assertTrue(Md5Utils.checkFileValidation(oldFilePath, "6e3ace2638be9fc855e7672964cf4d4c"));

        String patchFilePath = "/sdcard/webcachetestfiles/kaola.diff";
        assertTrue(Md5Utils.checkFileValidation(patchFilePath, "836a47fe852fa5352322dbd6f2c9a813"));

        String outFilePath = "/sdcard/webcachetestfiles/kaola.zip";
        String patchedFilePath = patcher.applyPatch(oldFilePath, patchFilePath, outFilePath);
        assertNotNull(patchedFilePath);
        File file = new File(outFilePath);
        assertTrue(file.exists());
        assertTrue(file.isFile());
        assertTrue(Md5Utils.checkFileValidation(patchedFilePath, "3244787f9fe856920a77a807a285e6b5"));

        file.delete();
        assertTrue(!file.exists());
    }
}