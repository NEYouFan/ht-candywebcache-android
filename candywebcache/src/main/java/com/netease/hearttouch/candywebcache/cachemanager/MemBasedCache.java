package com.netease.hearttouch.candywebcache.cachemanager;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by netease on 16/6/16.
 */
public class MemBasedCache implements Cache {
    /** Default maximum memory usage in bytes. */
    private static final int DEFAULT_MEMORY_USAGE_BYTES = (int)Runtime.getRuntime().maxMemory() / 8 > 0 ?
            (int)Runtime.getRuntime().maxMemory() / 8 : 4 * 1024 * 1024;

    /** High water mark percentage for the cache */
    private static final float HYSTERESIS_FACTOR = 0.9f;

    /** Total amount of space currently used by the cache in bytes. */
    private long mTotalSize = 0;

    /** The maximum size of the cache in bytes. */
    private final int mMaxCacheSizeInBytes;

    private Map<String, CacheHeader> mDataEntries;

    /**
     * Constructs an instance of the MemBasedCache.
     * @param maxCacheSizeInBytes The maximum size of the cache in bytes.
     */
    public MemBasedCache(int maxCacheSizeInBytes) {
        mMaxCacheSizeInBytes = maxCacheSizeInBytes;
        mDataEntries = new LinkedHashMap<String, CacheHeader>(16, .75f, true);
    }

    /**
     * Constructs an instance of the MemBasedCache using the default maximum cache size of 3MB.
     */
    public MemBasedCache() {
        this(DEFAULT_MEMORY_USAGE_BYTES);
    }

    @Override
    public synchronized void initialize() {

    }

    @Override
    public synchronized void put(String key, Entry entry) {
        pruneIfNeeded(entry.data.length);
        CacheHeader e = new CacheHeader(key, entry);
        if (!mDataEntries.containsKey(key)) {
            mTotalSize += e.data.length;
        } else {
            CacheHeader oldEntry = mDataEntries.get(key);
            mTotalSize += (e.data.length - oldEntry.data.length);
        }
        mDataEntries.put(key, e);
    }

    /**
     * Prunes the cache to fit the amount of bytes specified.
     * @param neededSpace The amount of bytes we are trying to fit into the cache.
     */
    private void pruneIfNeeded(int neededSpace) {
        if ((mTotalSize + neededSpace) < mMaxCacheSizeInBytes) {
            return;
        }
        long before = mTotalSize;
        int prunedFiles = 0;
//        long startTime = SystemClock.elapsedRealtime();
        Iterator<Map.Entry<String, CacheHeader>> iterator = mDataEntries.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, CacheHeader> entry = iterator.next();
            CacheHeader e = entry.getValue();
            mTotalSize -= e.data.length;
            iterator.remove();
            ++ prunedFiles;
            if ((mTotalSize + neededSpace) < mMaxCacheSizeInBytes * HYSTERESIS_FACTOR) {
                break;
            }
        }

//        if (WebcacheLog.DEBUG) {
//            WebcacheLog.v("pruned %d files, %d bytes, %d ms",
//                    prunedFiles, (mTotalSize - before), SystemClock.elapsedRealtime() - startTime);
//        }
    }

    @Override
    public synchronized Entry get(String key) {
        CacheHeader entry = mDataEntries.get(key);
        // if the entry does not exist, return.
        if (entry == null) {
            return null;
        }
        Entry e = new Entry();
        e.data = entry.data;
        return e;
    }

    @Override
    public synchronized void remove(String key) {
        CacheHeader entry = mDataEntries.get(key);
        // if the entry does not exist, return.
        if (entry != null) {
            mTotalSize -= entry.data.length;
            mDataEntries.remove(key);
        }
    }

    @Override
    public synchronized void invalidate(String key, boolean fullExpire) {
        remove(key);
    }


    @Override
    public synchronized void clear() {
        mDataEntries.clear();
        mTotalSize = 0;
    }

    public synchronized long totalDataSize() {
        return mTotalSize;
    }

    static class CacheHeader {
        public CacheHeader(String key, Entry entry) {
            this.key = key;
            this.data = entry.data;
        }

        public String key;

        public byte[] data;
    }
}
