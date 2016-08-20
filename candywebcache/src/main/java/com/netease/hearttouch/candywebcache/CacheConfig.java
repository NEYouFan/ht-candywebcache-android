package com.netease.hearttouch.candywebcache;

import java.util.List;

/**
 * Created by netease on 16/6/3.
 */
public class CacheConfig {
    private long mUpdateCheckCycle;
    private String mCacheDirPath;
    private String mManifestDirPath;
    private int mMemCacheSize;
    private List<String> mUncachedFileType;

    public static class ConfigBuilder {
        private long mUpdateCheckCycle;
        private String mCacheDirPath;
        private String mManifestDirPath;
        private int mMemCacheSize;
        private List<String> mUncachedFileType;

        private ConfigBuilder() {
        }

        public ConfigBuilder setUpdateCycle(long millis) {
            mUpdateCheckCycle = millis;
            return this;
        }

        public ConfigBuilder setCacheDirPath(String path) {
            mCacheDirPath = path;
            return this;
        }

        public ConfigBuilder setManifestDirPath(String path) {
            mManifestDirPath = path;
            return this;
        }

        public ConfigBuilder setMemCacheSize(int szBytes) {
            mMemCacheSize = szBytes;
            return this;
        }

        public ConfigBuilder setUncachedFileTypes(List<String> uncachedFileType) {
            mUncachedFileType = uncachedFileType;
            return this;
        }

        public CacheConfig build() {
            CacheConfig cacheConfig = new CacheConfig();
            cacheConfig.mUpdateCheckCycle = mUpdateCheckCycle;
            cacheConfig.mCacheDirPath = mCacheDirPath;
            cacheConfig.mManifestDirPath = mManifestDirPath;
            cacheConfig.mMemCacheSize = mMemCacheSize;
            cacheConfig.mUncachedFileType = mUncachedFileType;
            return cacheConfig;
        }
    }

    private CacheConfig() {

    }

    public static ConfigBuilder createCofigBuilder() {
        ConfigBuilder configBuilder = new ConfigBuilder();
        return configBuilder;
    }

    public long getUpdateCheckCycle() {
        return mUpdateCheckCycle;
    }

    public String getCacheDirPath() {
        return mCacheDirPath;
    }

    public String getManifestDirPath() {
        return mManifestDirPath;
    }

    public int getMemCacheSize() {
        return mMemCacheSize;
    }

    public List<String> getUncachedFileType() {
        return mUncachedFileType;
    }
}
