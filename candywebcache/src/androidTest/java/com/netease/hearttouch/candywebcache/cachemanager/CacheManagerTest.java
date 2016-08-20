package com.netease.hearttouch.candywebcache.cachemanager;

import android.content.Context;
import android.net.Uri;
import android.test.InstrumentationTestCase;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by netease on 16/6/17.
 */
public class CacheManagerTest extends InstrumentationTestCase {
    private Context mContext;
    private CacheManager mCacheManager;

    @Before
    public void setUp() throws Exception {
        String protectedDirPath = "/sdcard/kaola/webcache/manifest";
        File protectedDir = new File(protectedDirPath);
        if (!protectedDir.exists()) {
            assertTrue(protectedDir.mkdirs());
        }

        String cacheDirPath = "/sdcard/kaola/webcache/webapps";
        File cacheDir = new File(cacheDirPath);
        if (!cacheDir.exists()) {
            assertTrue(cacheDir.mkdirs());
        }

        List<String> uncachedFileTypes = new ArrayList<>();
        uncachedFileTypes.add(".html");

        mContext = getInstrumentation().getContext();
        mCacheManager = new CacheManager(mContext, protectedDirPath, cacheDirPath, uncachedFileTypes, 3 * 1024 * 1024);
        Thread.sleep(2000);
    }

    @After
    public void tearDown() throws Exception {
        mCacheManager.closeDB();
        assertTrue(FileUtils.deleteDir("/sdcard/kaola"));
    }

    private String getWithoutSchemaUrl(String urlstr) {
        String schemeSpecificPart = Uri.parse(urlstr).getSchemeSpecificPart();
        if (schemeSpecificPart.startsWith("//")) {
            schemeSpecificPart = schemeSpecificPart.substring("//".length());
        }
        return schemeSpecificPart;
    }

    @Test
    public void testGetWithoutSchemaUrl() throws Exception {
        String schemeSpecificPart = getWithoutSchemaUrl("https://g.hz.netease.com/web-cache/webcache_android/commits/master");
        assertEquals("g.hz.netease.com/web-cache/webcache_android/commits/master", schemeSpecificPart);

        schemeSpecificPart = getWithoutSchemaUrl("www.baidu.com");
        assertEquals("www.baidu.com", schemeSpecificPart);

        schemeSpecificPart = getWithoutSchemaUrl("http://my.oschina.net/wolfcs/blog/653833#OSC_h2_3");
        assertEquals("my.oschina.net/wolfcs/blog/653833", schemeSpecificPart);
    }

    private void loadLocalPackage() throws Exception {
        List<String> domains = new ArrayList<>();
        domains.add("www.163.com");
        domains.add("www.126.com/pub");
        domains.add("www.m.163.com");

        String filename = "kaola.zip";
        InputStream is = new FileInputStream("/sdcard/webcachetestfiles/kaola_20160827.zip");
        String version = "20160827";
        String md5 = "kC/KhxC55iM83nDOCXxvtaSeEQ0Qf99pCDYD6+ZUVp+ZagIKgzal4A==";
        mCacheManager.loadLocalPackage(filename, version, md5, domains, is);
        Thread.sleep(5000);

        String appDirPath = "/sdcard/kaola/webcache/webapps/kaola";
        File appDir = new File(appDirPath);
        assertTrue(appDir.exists());
        assertTrue(appDir.isDirectory());
        String pkgFilePath = "/sdcard/kaola/webcache/webapps/kaola/kaola.zip";
        File pkgFile = new File(pkgFilePath);
        assertTrue(pkgFile.exists());
        assertTrue(pkgFile.isFile());
    }

    @Test
    public void testLoadLocalPackage() throws Exception {
        loadLocalPackage();
    }

    @Test
    public void testGetWebappInfo() throws Exception {
        loadLocalPackage();
        WebappInfo webappInfo = mCacheManager.getWebappInfo("kaola");
        assertNotNull(webappInfo);
        assertEquals("kaola", webappInfo.mWebappName);
        assertEquals(20160827L, webappInfo.mVerNum);
        assertEquals("/sdcard/kaola/webcache/webapps/kaola", webappInfo.mCachedDirPath);
        assertEquals("20160827", webappInfo.mVerStr);
        assertEquals("/sdcard/kaola/webcache/webapps/kaola/kaola.zip", webappInfo.mPkgFilePath);
        assertEquals(3, webappInfo.mDomains.size());
        assertTrue(webappInfo.mDomains.contains("www.163.com"));
        assertTrue(webappInfo.mDomains.contains("www.126.com/pub"));
        assertTrue(webappInfo.mDomains.contains("www.m.163.com"));
        assertEquals(WebappInfo.WEBAPP_STATUS_AVAILABLE, webappInfo.mStatus);
        assertEquals("6e3ace2638be9fc855e7672964cf4d4c", webappInfo.mPkgFileMd5);
    }

    @Test
    public void testGetAllWebappInfo() throws Exception {
        loadLocalPackage();
        List<WebappInfo> allAppInfos = mCacheManager.getAllWebappInfo();
        assertEquals(1, allAppInfos.size());
        WebappInfo webappInfo = allAppInfos.get(0);
        assertNotNull(webappInfo);
        assertEquals("kaola", webappInfo.mWebappName);
        assertEquals(20160827L, webappInfo.mVerNum);
        assertEquals("/sdcard/kaola/webcache/webapps/kaola", webappInfo.mCachedDirPath);
        assertEquals("20160827", webappInfo.mVerStr);
        assertEquals("/sdcard/kaola/webcache/webapps/kaola/kaola.zip", webappInfo.mPkgFilePath);
        assertEquals(3, webappInfo.mDomains.size());
        assertTrue(webappInfo.mDomains.contains("www.163.com"));
        assertTrue(webappInfo.mDomains.contains("www.126.com/pub"));
        assertTrue(webappInfo.mDomains.contains("www.m.163.com"));
        assertEquals(WebappInfo.WEBAPP_STATUS_AVAILABLE, webappInfo.mStatus);
        assertEquals("6e3ace2638be9fc855e7672964cf4d4c", webappInfo.mPkgFileMd5);
    }

    private String copyToTmpFile(String patchPath, String suffix) {
        File patchFile = new File(patchPath);
        File outFile = null;
        try {
            FileInputStream fis = new FileInputStream(patchFile);
            outFile = File.createTempFile("tmp", suffix, new File("/sdcard/webcachetestfiles"));
            FileUtils.copyFile(fis, outFile);
        } catch (FileNotFoundException e) {
        } catch (IOException e) {
        }
        if (outFile != null) {
            return outFile.getAbsolutePath();
        }
        return null;
    }

    @Test
    public void testApplyPatch() throws Exception {
        loadLocalPackage();
        WebappInfo webappInfo = mCacheManager.getWebappInfo("kaola");
        assertNotNull(webappInfo);

        List<String> domains = new ArrayList<>();
        domains.add("www.163.com");
        domains.add("www.126.com/pub");
        domains.add("www.m.163.com");
        String patchPath = copyToTmpFile("/sdcard/webcachetestfiles/kaola.diff", ".diff");
        assertNotNull(patchPath);
        assertEquals(CacheManager.DIFF_SUCCESS, mCacheManager.applyPatch("kaola", CacheManager.PkgType.PkgDiff, patchPath,
                "ZuIMHTf3d+sS3C3dOK9UepD96EsChggc3XmVZhIn3MKZagIKgzal4A==", "S1zVHQTTe5gDJphM1w9xS7RK+NRoaR42nJsE9o8jkzeZagIKgzal4A==",
                "http://10.242.27.37:8000/test/web_app_new.zip", "20161019", domains));

        webappInfo = mCacheManager.getWebappInfo("kaola");
        assertNotNull(webappInfo);
        assertEquals("kaola", webappInfo.mWebappName);
        assertEquals(20161019L, webappInfo.mVerNum);
        assertEquals("/sdcard/kaola/webcache/webapps/kaola", webappInfo.mCachedDirPath);
        assertEquals("20161019", webappInfo.mVerStr);
        assertEquals("/sdcard/kaola/webcache/webapps/kaola/kaola.zip", webappInfo.mPkgFilePath);
        assertEquals(3, webappInfo.mDomains.size());
        assertTrue(webappInfo.mDomains.contains("www.163.com"));
        assertTrue(webappInfo.mDomains.contains("www.126.com/pub"));
        assertTrue(webappInfo.mDomains.contains("www.m.163.com"));
        assertEquals(WebappInfo.WEBAPP_STATUS_AVAILABLE, webappInfo.mStatus);
        assertEquals("3244787f9fe856920a77a807a285e6b5", webappInfo.mPkgFileMd5);
    }

    @Test
    public void testApplyFullPatch() throws Exception {
        loadLocalPackage();
        WebappInfo webappInfo = mCacheManager.getWebappInfo("kaola");
        assertNotNull(webappInfo);

        List<String> domains = new ArrayList<>();
        domains.add("www.163.com");
        domains.add("www.126.com/pub");
        domains.add("www.m.163.com");
        String tmpFile = copyToTmpFile("/sdcard/webcachetestfiles/kaola_20161019.zip", ".zip");
        assertNotNull(tmpFile);
        assertEquals(CacheManager.DIFF_SUCCESS, mCacheManager.applyPatch("kaola", CacheManager.PkgType.PkgFull, tmpFile,
                "S1zVHQTTe5gDJphM1w9xS7RK+NRoaR42nJsE9o8jkzeZagIKgzal4A==", "S1zVHQTTe5gDJphM1w9xS7RK+NRoaR42nJsE9o8jkzeZagIKgzal4A==",
                "http://10.242.27.37:8000/test/web_app_new.zip", "20161019", domains));

        webappInfo = mCacheManager.getWebappInfo("kaola");
        assertNotNull(webappInfo);
        assertEquals("kaola", webappInfo.mWebappName);
        assertEquals(20161019L, webappInfo.mVerNum);
        assertEquals("/sdcard/kaola/webcache/webapps/kaola", webappInfo.mCachedDirPath);
        assertEquals("20161019", webappInfo.mVerStr);
        assertEquals("/sdcard/kaola/webcache/webapps/kaola/kaola.zip", webappInfo.mPkgFilePath);
        assertEquals(3, webappInfo.mDomains.size());
        assertTrue(webappInfo.mDomains.contains("www.163.com"));
        assertTrue(webappInfo.mDomains.contains("www.126.com/pub"));
        assertTrue(webappInfo.mDomains.contains("www.m.163.com"));
        assertEquals(WebappInfo.WEBAPP_STATUS_AVAILABLE, webappInfo.mStatus);
        assertEquals("3244787f9fe856920a77a807a285e6b5", webappInfo.mPkgFileMd5);
    }

    @Test
    public void testApplyNewApp() throws Exception {
        loadLocalPackage();
        WebappInfo webappInfo = mCacheManager.getWebappInfo("kaola");
        assertNotNull(webappInfo);

        List<String> domains = new ArrayList<>();
        domains.add("www.163.com");
        domains.add("www.126.com/pub");
        domains.add("www.m.163.com");
        domains.add("music.163.com");
        String tmpFile = copyToTmpFile("/sdcard/webcachetestfiles/cloudmusic_20160923.zip", ".zip");
        assertNotNull(tmpFile);
        assertEquals(CacheManager.DIFF_SUCCESS, mCacheManager.applyPatch("cloudmusic", CacheManager.PkgType.PkgFull, tmpFile,
                "S1zVHQTTe5gDJphM1w9xS7RK+NRoaR42nJsE9o8jkzeZagIKgzal4A==", "S1zVHQTTe5gDJphM1w9xS7RK+NRoaR42nJsE9o8jkzeZagIKgzal4A==",
                "http://10.242.27.37:8000/test/web_app_new.zip", "20160923", domains));

        webappInfo = mCacheManager.getWebappInfo("cloudmusic");
        assertNotNull(webappInfo);
        assertEquals("cloudmusic", webappInfo.mWebappName);
        assertEquals(20160923L, webappInfo.mVerNum);
        assertEquals("/sdcard/kaola/webcache/webapps/cloudmusic", webappInfo.mCachedDirPath);
        assertEquals("20160923", webappInfo.mVerStr);
        assertEquals("/sdcard/kaola/webcache/webapps/cloudmusic/cloudmusic.zip", webappInfo.mPkgFilePath);
        assertEquals(4, webappInfo.mDomains.size());
        assertTrue(webappInfo.mDomains.contains("www.163.com"));
        assertTrue(webappInfo.mDomains.contains("www.126.com/pub"));
        assertTrue(webappInfo.mDomains.contains("www.m.163.com"));
        assertTrue(webappInfo.mDomains.contains("music.163.com"));
        assertEquals(WebappInfo.WEBAPP_STATUS_AVAILABLE, webappInfo.mStatus);
        assertEquals("3244787f9fe856920a77a807a285e6b5", webappInfo.mPkgFileMd5);
    }

    @Test
    public void testUpdateWebappInfo() throws Exception {
        loadLocalPackage();
        String appid = "kaola";
        WebappInfo webappInfo = mCacheManager.getWebappInfo(appid);
        assertNotNull(webappInfo);

        String fullUrl = "http://10.242.27.37:8000/test/web_app.zip";
        String verStr = "20160827";
        List<String> domains = new ArrayList<>();
        domains.add("www.163.com");
        domains.add("www.126.com/pub");
        domains.add("www.m.163.com");
        domains.add("www.m.kaola.com");

        mCacheManager.updateWebappInfo(appid, fullUrl, verStr, domains);

        webappInfo = mCacheManager.getWebappInfo("kaola");
        assertNotNull(webappInfo);
        assertEquals("kaola", webappInfo.mWebappName);
        assertEquals(20160827L, webappInfo.mVerNum);
        assertEquals("/sdcard/kaola/webcache/webapps/kaola", webappInfo.mCachedDirPath);
        assertEquals("20160827", webappInfo.mVerStr);
        assertEquals("/sdcard/kaola/webcache/webapps/kaola/kaola.zip", webappInfo.mPkgFilePath);
        assertEquals(4, webappInfo.mDomains.size());
        assertTrue(webappInfo.mDomains.contains("www.163.com"));
        assertTrue(webappInfo.mDomains.contains("www.126.com/pub"));
        assertTrue(webappInfo.mDomains.contains("www.m.163.com"));
        assertTrue(webappInfo.mDomains.contains("www.m.kaola.com"));
        assertEquals(WebappInfo.WEBAPP_STATUS_AVAILABLE, webappInfo.mStatus);
        assertEquals("6e3ace2638be9fc855e7672964cf4d4c", webappInfo.mPkgFileMd5);
        assertEquals("http://10.242.27.37:8000/test/web_app.zip", webappInfo.mFullUrl);
    }

    @Test
    public void testGetAppidFromUrl() throws Exception {
        loadLocalPackage();
        String url = "http://www.126.com/pub/web_app/tp_module_page_detail_index.js";
        String appid = mCacheManager.getAppidFromUrl(url);
        assertEquals("kaola", appid);

        url = "http://www.m.163.com/web_app/tp_module_frames_head_index.js";
        appid = mCacheManager.getAppidFromUrl(url);
        assertEquals("kaola", appid);

        url = "http://www.m.163.com/web_app/module/resource/template/create/index.html";
        appid = mCacheManager.getAppidFromUrl(url);
        assertEquals("kaola", appid);

        url = "http://www.m.126.com/web_app/module/resource/apiInterface/create/index.html";
        appid = mCacheManager.getAppidFromUrl(url);
        assertNull(appid);
    }

    @Test
    public void testGetResource() throws Exception {
        loadLocalPackage();
        String url = "http://www.126.com/pub/web_app/tp_module_page_detail_index.js";
        InputStream is = mCacheManager.getResource(url);
        assertNotNull(is);
        is.close();

        url = "http://www.m.163.com/web_app/tp_module_frames_head_index.js";
        is = mCacheManager.getResource(url);
        assertNotNull(is);
        is.close();

        url = "http://www.m.163.com/web_app/module/resource/template/create/index.html";
        is = mCacheManager.getResource(url);
        assertNull(is);
    }

    @Test
    public void testClearCache() throws Exception {
        loadLocalPackage();
        mCacheManager.clearAllCache();
    }

    @Test
    public void testClearAllCache() throws Exception {
        loadLocalPackage();
        mCacheManager.clearCache("kaola");
    }

    @Test
    public void testDeleteWebapp() throws Exception {
        loadLocalPackage();

        mCacheManager.deleteWebapp("kaola");
        WebappInfo webappInfo = mCacheManager.getWebappInfo("kaola");
        assertNull(webappInfo);

        File appCacheDir = new File("/sdcard/kaola/webcache/webapps/kaola");
        assertFalse(appCacheDir.exists());
    }
}