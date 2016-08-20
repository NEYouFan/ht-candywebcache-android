package com.netease.hearttouch.candywebcache;

import android.content.Context;
import android.support.test.runner.AndroidJUnit4;
import android.test.InstrumentationTestCase;

import com.netease.hearttouch.candywebcache.cachemanager.FileUtils;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Created by netease on 16/6/12.
 */
public class CandyWebCacheTest extends InstrumentationTestCase {
    Context mContext;
    CandyWebCache mCandyWebCache;
    @Before
    public void setUp() throws Exception {
        mContext = getInstrumentation().getContext();
        mCandyWebCache = CandyWebCache.getsInstance();
    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void testGetInstance() throws Exception {
        assertNotNull(mCandyWebCache);
        CandyWebCache webcache = CandyWebCache.getsInstance();
        assertNotNull(webcache);
        assertEquals(mCandyWebCache, webcache);
    }

    @Test
    public void testInitNullCacheConfig() throws Exception {
        assertTrue(true);
        mCandyWebCache.init(mContext, null, "kaola", "20160617", null);
        Thread.sleep(4000);
        File protectedFilesDir = new File(mContext.getFilesDir(), "webcache");
        assertTrue(protectedFilesDir.exists());
        File databaseDir = new File(protectedFilesDir, "database");
        assertTrue(databaseDir.exists());
        File databaseFile = new File(databaseDir, "cache_info.db");
        assertTrue(databaseFile.exists());

        File cacheFilesDir = new File("/sdcard/netease/webcache/" + mContext.getPackageName()
                + File.separator + "webapps");
        assertTrue(cacheFilesDir.exists());

        File downloadTmpDir = new File("/sdcard/netease/webcache/" + mContext.getPackageName()
                + File.separator + "download");
        assertTrue(downloadTmpDir.exists());
    }

    @Test
    public void testInit() throws Exception {
        assertTrue(FileUtils.deleteDir("/sdcard/kaola"));
        CacheConfig.ConfigBuilder builder = CacheConfig.createCofigBuilder();

        List<String> uncachedFileType = new ArrayList<String>();
        builder.setUncachedFileTypes(uncachedFileType);

        builder.setMemCacheSize(2 * 1024 * 1024);

        String cacheDirPath = "/sdcard/kaola/webcache";
        builder.setCacheDirPath(cacheDirPath);

        long cyclemillis = 100;
        builder.setUpdateCycle(cyclemillis);

        String protectedFilesPath = "/sdcard/kaola/manifest";
        builder.setManifestDirPath(protectedFilesPath);
        CacheConfig config = builder.build();

        mCandyWebCache.init(mContext, config, "kaola", "20160617", null);
        Thread.sleep(4000);

        File protectedFilesDir = new File("/sdcard/kaola/manifest");
        assertTrue(protectedFilesDir.exists());

        File databaseDir = new File("/sdcard/kaola/manifest/database");
        assertTrue(databaseDir.exists());
        File databaseFile = new File("/sdcard/kaola/manifest/database/cache_info.db");
        assertTrue(databaseFile.exists());

        File cacheFilesDir = new File("/sdcard/kaola/webcache");
        assertTrue(cacheFilesDir.exists());

        File downloadTmpDir = new File("/sdcard/kaola/webcache/download");
        assertTrue(downloadTmpDir.exists());
    }

    @Test
    public void testRecvServerMsg() throws Exception {
        assertTrue(true);
    }

    public void testUpdateWebapp() {

    }

    @org.junit.Test
    public void testConstructUrl() throws Exception {
        String url = "http://www.kaola.com/activity/detail/11884.shtml";
        Map<String, String> params = new HashMap<>();
        params.put("navindex", "5");
        params.put("zn", "top");
        String result = mCandyWebCache.constructUrl(url, params);
        assertTrue(result.contains("navindex=5"));
        assertTrue(result.contains("zn=top"));
        assertTrue(result.indexOf("http://www.kaola.com/activity/detail/11884.shtml?") == 0);
        assertTrue(result.indexOf("navindex=5") >= "http://www.kaola.com/activity/detail/11884.shtml?".length());
        assertTrue(result.indexOf("zn=top") >= "http://www.kaola.com/activity/detail/11884.shtml?".length());
        assertEquals(result.length(), "http://www.kaola.com/activity/detail/11884.shtml?".length()
                + "navindex=5".length() + "zn=top".length() + 1);

        url = "http://www.kaola.com/activity/detail/11884.shtml?";
        result = mCandyWebCache.constructUrl(url, params);
        assertTrue(result.contains("navindex=5"));
        assertTrue(result.contains("zn=top"));
        assertTrue(result.indexOf("http://www.kaola.com/activity/detail/11884.shtml?") == 0);
        assertTrue(result.indexOf("navindex=5") >= "http://www.kaola.com/activity/detail/11884.shtml?".length());
        assertTrue(result.indexOf("zn=top") >= "http://www.kaola.com/activity/detail/11884.shtml?".length());
        assertEquals(result.length(), "http://www.kaola.com/activity/detail/11884.shtml?".length()
                + "navindex=5".length() + "zn=top".length() + 1);

        url = "http://www.kaola.com/activity/detail/11884.shtml?navindex=5";
        params = new HashMap<>();
        params.put("zn", "top");
        result = mCandyWebCache.constructUrl(url, params);
        assertTrue(result.contains("navindex=5"));
        assertTrue(result.contains("zn=top"));
        assertTrue(result.indexOf("http://www.kaola.com/activity/detail/11884.shtml?") == 0);
        assertTrue(result.indexOf("navindex=5") >= "http://www.kaola.com/activity/detail/11884.shtml?".length());
        assertTrue(result.indexOf("zn=top") >= "http://www.kaola.com/activity/detail/11884.shtml?".length());
        assertEquals(result.length(), "http://www.kaola.com/activity/detail/11884.shtml?".length()
                + "navindex=5".length() + "zn=top".length() + 1);

        url = "http://www.kaola.com/activity/detail/11884.shtml?navindex=";
        params = new HashMap<>();
        params.put("zn", "top");
        result = mCandyWebCache.constructUrl(url, params);
        assertTrue(result.contains("navindex="));
        assertTrue(result.contains("zn=top"));
        assertTrue(result.indexOf("http://www.kaola.com/activity/detail/11884.shtml?") == 0);
        assertTrue(result.indexOf("navindex=") >= "http://www.kaola.com/activity/detail/11884.shtml?".length());
        assertTrue(result.indexOf("zn=top") >= "http://www.kaola.com/activity/detail/11884.shtml?".length());
        assertEquals(result.length(), "http://www.kaola.com/activity/detail/11884.shtml?".length()
                + "navindex=".length() + "zn=top".length() + 1);

    }
}