package com.netease.hearttouch.candywebcache.cachemanager;

import android.content.Context;
import android.test.InstrumentationTestCase;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Created by netease on 16/6/20.
 */
public class DatabaseManagerTest extends InstrumentationTestCase {
    Context mContext;
    DatabaseManager mDatabaseManager;

    @Before
    public void setUp() throws Exception {
        mContext = getInstrumentation().getContext();
    }

    @After
    public void tearDown() throws Exception {
    }

    private void initDatabase() {
        String databaseDirPath = "/sdcard/cloudmusic/database";
        File databaseDir = new File(databaseDirPath);
        if (!databaseDir.exists()) {
            assertTrue(databaseDir.mkdirs());
        }
        File databaseFile = new File("/sdcard/cloudmusic/database/cache_info.db");
        if (databaseFile.exists()) {
            assertTrue(databaseFile.delete());
        }
        mDatabaseManager = new DatabaseManager(mContext, databaseDirPath);
    }

    @Test
    public void testGetAllWebappInfo() throws Exception {
        initDatabase();

        List<WebappInfo> allAppInfos = mDatabaseManager.getAllWebappInfo();
        assertNotNull(allAppInfos);
        File databaseFile = new File("/sdcard/cloudmusic/database/cache_info.db");
        assertTrue(databaseFile.exists());
    }

    private WebappInfo insertWebappInfo() {
        initDatabase();
        List<String> domains = new ArrayList<>();
        domains.add("www.163.com");
        domains.add("www.163.com/pub");
        domains.add("www.m.163.com");

        String fullUrl = "http://test/res.rar";
        String appid = "kaola";
        String verStr = "20160618";
        String appDirPath = "/sdcard/netease/webcache/webapps/kaola";
        long pkgFileSize = 234753;
        String pkgFilePath = "/sdcard/netease/webcache/webapps/kaola/kaola.zip";
        String fullMd5 = "6787678sdjfhjhdfjal998";

        WebappInfo appInfo = new WebappInfo(domains, fullUrl, appid, verStr, appDirPath, pkgFileSize,
                pkgFilePath, fullMd5, WebappInfo.WEBAPP_STATUS_UPDATE_START);
        mDatabaseManager.insertWebappInfo(appInfo);
        return appInfo;
    }

    @Test
    public void testInsertAndWebappInfo() throws Exception {
        insertWebappInfo();

        List<WebappInfo> allAppInfos = mDatabaseManager.getAllWebappInfo();
        assertEquals(allAppInfos.size(), 1);
        WebappInfo appinfo = allAppInfos.get(0);
        assertTrue(appinfo.mDomains.contains("www.163.com"));
        assertTrue(appinfo.mDomains.contains("www.163.com/pub"));
        assertTrue(appinfo.mDomains.contains("www.m.163.com"));

        assertEquals("http://test/res.rar", appinfo.mFullUrl);
        assertEquals("kaola", appinfo.mWebappName);
        assertEquals("20160618", appinfo.mVerStr);
        assertEquals("/sdcard/netease/webcache/webapps/kaola", appinfo.mCachedDirPath);
        assertEquals(234753, appinfo.mCacheSize);
        assertEquals("/sdcard/netease/webcache/webapps/kaola/kaola.zip", appinfo.mPkgFilePath);
        assertEquals("6787678sdjfhjhdfjal998", appinfo.mPkgFileMd5);
    }

    @Test
    public void testUpdateWebappInfo() throws Exception {
        WebappInfo webappInfo = insertWebappInfo();

        webappInfo = new WebappInfo(webappInfo.mDomains, "http://download.163.com/res.rar", webappInfo.mWebappName, "20160923",
                webappInfo.mCachedDirPath, webappInfo.mCacheSize, webappInfo.mPkgFilePath, "6787678a23d53fjal998",
                webappInfo.mStatus, webappInfo.mFileInfos);

        assertTrue(mDatabaseManager.updateWebappInfo(webappInfo));

        List<WebappInfo> allAppInfos = mDatabaseManager.getAllWebappInfo();
        assertEquals(allAppInfos.size(), 1);
        WebappInfo appinfo = allAppInfos.get(0);
        assertTrue(appinfo.mDomains.contains("www.163.com"));
        assertTrue(appinfo.mDomains.contains("www.163.com/pub"));
        assertTrue(appinfo.mDomains.contains("www.m.163.com"));

        assertEquals("http://download.163.com/res.rar", appinfo.mFullUrl);
        assertEquals("kaola", appinfo.mWebappName);
        assertEquals("20160923", appinfo.mVerStr);
        assertEquals("/sdcard/netease/webcache/webapps/kaola", appinfo.mCachedDirPath);
        assertEquals(234753, appinfo.mCacheSize);
        assertEquals("/sdcard/netease/webcache/webapps/kaola/kaola.zip", appinfo.mPkgFilePath);
        assertEquals("6787678a23d53fjal998", appinfo.mPkgFileMd5);
    }

    @Test
    public void testDeleteWebappInfo() throws Exception {
        insertWebappInfo();
        mDatabaseManager.deleteWebappInfo("kaola");
        List<WebappInfo> allAppInfos = mDatabaseManager.getAllWebappInfo();
        assertTrue(allAppInfos.isEmpty());
    }

    private List<FileInfo> insertFileInfos() {
        initDatabase();

        List<FileInfo> fileInfos = new ArrayList<>();
        fileInfos.add(new FileInfo("82fa3b9e3374a9b077894c8396285f95", "/sdcard/netease/web_app_new_full/tp_module_dashboard_index.js", "kaola",
                "web_app_new_full/tp_module_dashboard_index.js", 0, FileInfo.FILE_STATUS_GOOD));
        fileInfos.add(new FileInfo("fd862c0757a86092131b9e3afdab730e", "/sdcard/netease/web_app_new_full/module/test/index.html", "kaola",
                "web_app_new_full/module/test/index.html", 0, FileInfo.FILE_STATUS_GOOD));
        fileInfos.add(new FileInfo("d3c86cc19520cccc2b320ab1dfd8e342", "/sdcard/netease/web_app_new_full/tp_module_resource_template_create_index.js", "kaola",
                "web_app_new_full/tp_module_resource_template_create_index.js", 0, FileInfo.FILE_STATUS_GOOD));
        fileInfos.add(new FileInfo("52dcce74a346419e0d355c12cf124a23", "/sdcard/netease/web_app_new_full/tp_module_resource_index.js", "kaola",
                "web_app_new_full/tp_module_resource_index.js", 0, FileInfo.FILE_STATUS_GOOD));
        fileInfos.add(new FileInfo("dfda7f03a4751aa7467e217640df2591", "/sdcard/netease/web_app_new_full/tp_module_test_index.js", "kaola",
                "web_app_new_full/tp_module_test_index.js", 0, FileInfo.FILE_STATUS_GOOD));

        assertTrue(mDatabaseManager.updateFileInfos(fileInfos));
        fileInfos = mDatabaseManager.getFileInfos();
        assertEquals(fileInfos.size(), 5);
        return fileInfos;
    }

    @Test
    public void testUpdateAndFileInfo() throws Exception {
        insertFileInfos();

        FileInfo fileInfo = new FileInfo("82fa3b9e3374a9b077894c8396285f95", "/sdcard/netease/web_app_new_full/tp_module_dashboard_index.js", "kaola",
                "web_app_new_full/tp_module_dashboard_index.js", 5, FileInfo.FILE_STATUS_BROKEN);
        mDatabaseManager.updateFileInfo(fileInfo);
        List<FileInfo> fileInfos = mDatabaseManager.getFileInfos();
        assertEquals(fileInfos.size(), 5);

        for (FileInfo afileInfo : fileInfos) {
            if ("82fa3b9e3374a9b077894c8396285f95".equals(afileInfo.mMd5)) {
                assertEquals("/sdcard/netease/web_app_new_full/tp_module_dashboard_index.js", afileInfo.mLocalPath);
                assertEquals("web_app_new_full/tp_module_dashboard_index.js", afileInfo.mUrl);
                assertEquals("kaola", afileInfo.mAppname);
                assertEquals(5, afileInfo.getmAccessCount());
                assertEquals(FileInfo.FILE_STATUS_BROKEN, afileInfo.getStatus());
            }
        }
    }

    @Test
    public void testUpdateAndGetFileInfos() throws Exception {
        List<FileInfo> fileInfos = insertFileInfos();

        for (FileInfo fileInfo : fileInfos) {
            if ("82fa3b9e3374a9b077894c8396285f95".equals(fileInfo.mMd5)) {
                assertEquals("/sdcard/netease/web_app_new_full/tp_module_dashboard_index.js", fileInfo.mLocalPath);
                assertEquals("web_app_new_full/tp_module_dashboard_index.js", fileInfo.mUrl);
                assertEquals("kaola", fileInfo.mAppname);
                assertEquals(0, fileInfo.getmAccessCount());
                assertEquals(FileInfo.FILE_STATUS_GOOD, fileInfo.getStatus());
            } else if ("fd862c0757a86092131b9e3afdab730e".equals(fileInfo.mMd5)) {
                assertEquals("/sdcard/netease/web_app_new_full/module/test/index.html", fileInfo.mLocalPath);
                assertEquals("web_app_new_full/module/test/index.html", fileInfo.mUrl);
                assertEquals("kaola", fileInfo.mAppname);
                assertEquals(0, fileInfo.getmAccessCount());
                assertEquals(FileInfo.FILE_STATUS_GOOD, fileInfo.getStatus());
            } else if ("d3c86cc19520cccc2b320ab1dfd8e342".equals(fileInfo.mMd5)) {
                assertEquals("/sdcard/netease/web_app_new_full/tp_module_resource_template_create_index.js", fileInfo.mLocalPath);
                assertEquals("web_app_new_full/tp_module_resource_template_create_index.js", fileInfo.mUrl);
                assertEquals("kaola", fileInfo.mAppname);
                assertEquals(0, fileInfo.getmAccessCount());
                assertEquals(FileInfo.FILE_STATUS_GOOD, fileInfo.getStatus());
            } else if ("52dcce74a346419e0d355c12cf124a23".equals(fileInfo.mMd5)) {
                assertEquals("/sdcard/netease/web_app_new_full/tp_module_resource_index.js", fileInfo.mLocalPath);
                assertEquals("web_app_new_full/tp_module_resource_index.js", fileInfo.mUrl);
                assertEquals("kaola", fileInfo.mAppname);
                assertEquals(0, fileInfo.getmAccessCount());
                assertEquals(FileInfo.FILE_STATUS_GOOD, fileInfo.getStatus());
            } else if ("dfda7f03a4751aa7467e217640df2591".equals(fileInfo.mMd5)) {
                assertEquals("/sdcard/netease/web_app_new_full/tp_module_test_index.js", fileInfo.mLocalPath);
                assertEquals("web_app_new_full/tp_module_test_index.js", fileInfo.mUrl);
                assertEquals("kaola", fileInfo.mAppname);
                assertEquals(0, fileInfo.getmAccessCount());
                assertEquals(FileInfo.FILE_STATUS_GOOD, fileInfo.getStatus());
            }
        }
    }

    @Test
    public void testDeleteWebappFileInfos() throws Exception {
        insertFileInfos();
        mDatabaseManager.deleteWebappFileInfos("kaola");
        List<FileInfo> fileInfos = mDatabaseManager.getFileInfos();
        assertNotNull(fileInfos);
        assertEquals(0, fileInfos.size());
    }
}