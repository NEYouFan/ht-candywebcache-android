package com.netease.hearttouch.candywebcache;

import org.junit.After;
import org.junit.Before;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Created by netease on 16/6/12.
 */
public class CacheConfigTest {
    private CacheConfig.ConfigBuilder mConfigBuilder;

    @Before
    public void setUp() throws Exception {
        mConfigBuilder = CacheConfig.createCofigBuilder();
    }

    @After
    public void tearDown() throws Exception {

    }

    @org.junit.Test
    public void testCreateCofigBuilder() throws Exception {
        assertNotNull(mConfigBuilder);
    }

    @org.junit.Test
    public void testGetUpdateCheckCycle() throws Exception {
        mConfigBuilder.setUpdateCycle(1000);
        CacheConfig cacheConfig = mConfigBuilder.build();
        assertNotNull(cacheConfig);
        assertEquals(1000, cacheConfig.getUpdateCheckCycle());
    }

    @org.junit.Test
    public void testGetCacheDirPath() throws Exception {
        mConfigBuilder.setCacheDirPath("/sdcard/netease/webcache/webapp");
        CacheConfig cacheConfig = mConfigBuilder.build();
        assertNotNull(cacheConfig);
        assertEquals("/sdcard/netease/webcache/webapp", cacheConfig.getCacheDirPath());
    }

    @org.junit.Test
    public void testGetManifestDirPath() throws Exception {
        mConfigBuilder.setManifestDirPath("/sdcard/netease/webcache/manifest");
        CacheConfig cacheConfig = mConfigBuilder.build();
        assertNotNull(cacheConfig);
        assertEquals("/sdcard/netease/webcache/manifest", cacheConfig.getManifestDirPath());
    }

    @org.junit.Test
    public void testGetMemCacheSize() throws Exception {
        mConfigBuilder.setMemCacheSize(5 * 1024 * 1024);
        CacheConfig cacheConfig = mConfigBuilder.build();
        assertNotNull(cacheConfig);
        assertEquals(5 * 1024 * 1024, cacheConfig.getMemCacheSize());
    }

    @org.junit.Test
    public void testGetUncachedFileType() throws Exception {
        ArrayList<String> uncachedFileTypes = new ArrayList<String>();
        uncachedFileTypes.add(".index");
        uncachedFileTypes.add(".css");
        mConfigBuilder.setUncachedFileTypes(uncachedFileTypes);
        CacheConfig cacheConfig = mConfigBuilder.build();
        assertNotNull(cacheConfig);
        List<String> fileTypes = cacheConfig.getUncachedFileType();
        assertTrue(fileTypes.contains(".index"));
        assertTrue(fileTypes.contains(".css"));
        assertFalse(fileTypes.contains(".js"));
    }
}