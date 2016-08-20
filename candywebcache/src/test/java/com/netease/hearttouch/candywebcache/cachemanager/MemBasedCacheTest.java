package com.netease.hearttouch.candywebcache.cachemanager;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedList;

import static org.junit.Assert.*;

/**
 * Created by netease on 16/6/18.
 */
public class MemBasedCacheTest {
    private MemBasedCache mCache;

    @Before
    public void setUp() throws Exception {
        mCache = new MemBasedCache();
    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void testInitialize() throws Exception {
        mCache.initialize();
        assertTrue(true);
    }

    private Cache.Entry loadFile(String key, File file) {
        Cache.Entry entry = null;
        InputStream is = null;
        try {
            is = new FileInputStream(file);
            byte[] data = new byte[(int)file.length()];
            int readLength = is.read(data);

            if (readLength == file.length()) {
                entry = new Cache.Entry();
                entry.data = data;
                mCache.put(key, entry);
            }
        } catch (FileNotFoundException e) {
        } catch (IOException e) {
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                }
            }
        }
        return entry;
    }

    private long loadAndCacheAppResource(String appid, String appRootPath) {
        File rootDir = new File(appRootPath);
        File rootParentDir = rootDir.getParentFile();

        long totalSize = 0;
        LinkedList<File> appFiles = new LinkedList<>();
        appFiles.add(rootDir);
        while (!appFiles.isEmpty()) {
            File file = appFiles.remove();
            if (file.isFile()) {
                String key = appid + File.separator + file.getAbsolutePath().substring(rootParentDir.getAbsolutePath().length() + 1);
                Cache.Entry entry = loadFile(key, file);
                assertNotNull(entry);
                assertNotNull(entry.data);
                assertNotEquals(entry.data.length, 0);
                totalSize += entry.data.length;
            } else {
                File[] files = file.listFiles();
                if (files != null && files.length > 0) {
                    for (File subFile : files) {
                        appFiles.add(subFile);
                    }
                }
            }
        }
        return totalSize;
    }

    private void testLoadFiles() {
        String appid1 = "kaola";
        String appRootPath1 = "/Users/netease/webcacheunittestfiles/web_app_old_kaola";
        long totalSize1 = loadAndCacheAppResource(appid1, appRootPath1);
        assertEquals(mCache.totalDataSize(), totalSize1);

        String appid2 = "cloudmusic";
        String appRootPath2 = "/Users/netease/webcacheunittestfiles/web_app_new_cloudmusic";
        long totalSize2 = loadAndCacheAppResource(appid2, appRootPath2);
        assertEquals(mCache.totalDataSize(), totalSize1 + totalSize2);
    }

    @Test
    public void testPut() throws Exception {
        testLoadFiles();
    }

    @Test
    public void testPut1() throws Exception {
        testLoadFiles();

        for(int i = 0; i < 15; ++ i) {
            String appid = "cloudmusic" + i;
            String appRootPath;
            if (i % 2 == 0) {
                appRootPath = "/Users/netease/webcacheunittestfiles/web_app_new_cloudmusic";
            } else {
                appRootPath = "/Users/netease/webcacheunittestfiles/web_app_old_kaola";
            }

            long totalSize = loadAndCacheAppResource(appid, appRootPath);
            assertTrue(totalSize < 3 * 1024 * 1024);
        }
    }

    @Test
    public void testGet() throws Exception {
        testLoadFiles();
        Cache.Entry entry = mCache.get("kaola/web_app_old_kaola/module/dashboard/index.html");
        assertNotNull(entry);

        entry = mCache.get("kaola/web_app_old_kaola/module/resource/interface/index.html");
        assertNotNull(entry);

        entry = mCache.get("kaola/web_app_old_kaola/module/resource/datatype/create/index.html");
        assertNotNull(entry);

        entry = mCache.get("kaola/web_app_old_kaola/module/page/detail/index.html");
        assertNotNull(entry);

        entry = mCache.get("kaola/web_app_old_kaola/pt_main.js");
        assertNotNull(entry);

        entry = mCache.get("kaola/web_app_old_kaola/tp_module_resource_interface_create_index.js");
        assertNotNull(entry);
    }

    @Test
    public void testGet1() throws Exception {
        testLoadFiles();
        Cache.Entry entry = mCache.get("cloudmusic/web_app_new_cloudmusic/tp_module_resource_interface_create_index.js");
        assertNotNull(entry);

        entry = mCache.get("cloudmusic/web_app_new_cloudmusic/tp_module_frames_layout_index.js");
        assertNotNull(entry);

        entry = mCache.get("cloudmusic/web_app_new_cloudmusic/tp_module_resource_template_index.js");
        assertNotNull(entry);

        entry = mCache.get("cloudmusic/web_app_new_cloudmusic/tp_module_frames_head_index.js");
        assertNotNull(entry);

        entry = mCache.get("cloudmusic/web_app_new_cloudmusic/module/resource/template/create/index.html");
        assertNotNull(entry);

        entry = mCache.get("cloudmusic/web_app_new_cloudmusic/module/resource/apiInterface/create/index.html");
        assertNotNull(entry);
    }

    @Test
    public void testRemove() throws Exception {
        testLoadFiles();

        String key = "cloudmusic/web_app_new_cloudmusic/tp_module_resource_interface_create_index.js";
        Cache.Entry entry = mCache.get(key);
        long totalSizeBefore = mCache.totalDataSize();
        assertNotNull(entry);
        mCache.remove(key);
        entry = mCache.get(key);
        File file = new File("/Users/netease/webcacheunittestfiles/web_app_new_cloudmusic/tp_module_resource_interface_create_index.js");
        assertEquals(totalSizeBefore, file.length() + mCache.totalDataSize());
        assertNull(entry);

        key = "cloudmusic/web_app_new_cloudmusic/tp_module_frames_layout_index.js";
        entry = mCache.get(key);
        totalSizeBefore = mCache.totalDataSize();
        assertNotNull(entry);
        mCache.remove(key);
        entry = mCache.get(key);
        file = new File("/Users/netease/webcacheunittestfiles/web_app_new_cloudmusic/tp_module_frames_layout_index.js");
        assertEquals(totalSizeBefore, file.length() + mCache.totalDataSize());
        assertNull(entry);

        key = "cloudmusic/web_app_new_cloudmusic/module/resource/apiInterface/create/index.html";
        entry = mCache.get(key);
        totalSizeBefore = mCache.totalDataSize();
        assertNotNull(entry);
        mCache.remove(key);
        entry = mCache.get(key);
        file = new File("/Users/netease/webcacheunittestfiles/web_app_new_cloudmusic/module/resource/apiInterface/create/index.html");
        assertEquals(totalSizeBefore, file.length() + mCache.totalDataSize());
        assertNull(entry);
    }

    @Test
    public void testInvalidate() throws Exception {
        testLoadFiles();

        String key = "cloudmusic/web_app_new_cloudmusic/tp_module_resource_interface_create_index.js";
        Cache.Entry entry = mCache.get(key);
        long totalSizeBefore = mCache.totalDataSize();
        assertNotNull(entry);
        mCache.invalidate(key, true);
        entry = mCache.get(key);
        File file = new File("/Users/netease/webcacheunittestfiles/web_app_new_cloudmusic/tp_module_resource_interface_create_index.js");
        assertEquals(totalSizeBefore, file.length() + mCache.totalDataSize());
        assertNull(entry);

        key = "cloudmusic/web_app_new_cloudmusic/tp_module_frames_layout_index.js";
        entry = mCache.get(key);
        totalSizeBefore = mCache.totalDataSize();
        assertNotNull(entry);
        mCache.invalidate(key, true);
        entry = mCache.get(key);
        file = new File("/Users/netease/webcacheunittestfiles/web_app_new_cloudmusic/tp_module_frames_layout_index.js");
        assertEquals(totalSizeBefore, file.length() + mCache.totalDataSize());
        assertNull(entry);

        key = "cloudmusic/web_app_new_cloudmusic/module/resource/apiInterface/create/index.html";
        entry = mCache.get(key);
        totalSizeBefore = mCache.totalDataSize();
        assertNotNull(entry);
        mCache.invalidate(key, true);
        entry = mCache.get(key);
        file = new File("/Users/netease/webcacheunittestfiles/web_app_new_cloudmusic/module/resource/apiInterface/create/index.html");
        assertEquals(totalSizeBefore, file.length() + mCache.totalDataSize());
        assertNull(entry);
    }

    @Test
    public void testClear() throws Exception {
        testLoadFiles();
        Cache.Entry entry = mCache.get("cloudmusic/web_app_new_cloudmusic/tp_module_resource_interface_create_index.js");
        assertNotNull(entry);

        entry = mCache.get("cloudmusic/web_app_new_cloudmusic/tp_module_frames_layout_index.js");
        assertNotNull(entry);

        entry = mCache.get("cloudmusic/web_app_new_cloudmusic/tp_module_resource_template_index.js");
        assertNotNull(entry);

        entry = mCache.get("cloudmusic/web_app_new_cloudmusic/tp_module_frames_head_index.js");
        assertNotNull(entry);

        entry = mCache.get("cloudmusic/web_app_new_cloudmusic/module/resource/template/create/index.html");
        assertNotNull(entry);

        entry = mCache.get("cloudmusic/web_app_new_cloudmusic/module/resource/apiInterface/create/index.html");
        assertNotNull(entry);

        mCache.clear();
        assertEquals(mCache.totalDataSize(), 0);

        entry = mCache.get("cloudmusic/web_app_new_cloudmusic/tp_module_frames_layout_index.js");
        assertNull(entry);

        entry = mCache.get("cloudmusic/web_app_new_cloudmusic/tp_module_resource_template_index.js");
        assertNull(entry);

        entry = mCache.get("cloudmusic/web_app_new_cloudmusic/tp_module_frames_head_index.js");
        assertNull(entry);

        entry = mCache.get("cloudmusic/web_app_new_cloudmusic/module/resource/template/create/index.html");
        assertNull(entry);

        entry = mCache.get("cloudmusic/web_app_new_cloudmusic/module/resource/apiInterface/create/index.html");
        assertNull(entry);
    }
}