package com.netease.hearttouch.candywebcache.cachemanager;

/**
 * Created by netease on 16/6/15.
 */
public class FileInfo {
    public static final int FILE_STATUS_GOOD = 1;
    public static final int FILE_STATUS_BROKEN = 2;

    public final String mMd5;
    public final String mLocalPath;
    public final String mAppname;
    public final String mUrl;

    private int mAccessCount;
    private int mStatus;

    public FileInfo(String md5, String localPath, String appname, String url, int accessCount, int status) {
        mMd5 = md5;
        mLocalPath = localPath;
        mAppname = appname;
        mUrl = url;
        mAccessCount = accessCount;
        mStatus = status;
    }

    public void setStatus(int status) {
        mStatus = status;
    }

    public int getStatus() {
        return mStatus;
    }

    public void increaseAccessCount() {
        ++ mAccessCount;
    }

    public int getmAccessCount() {
        return mAccessCount;
    }
}
