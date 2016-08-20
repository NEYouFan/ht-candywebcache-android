package com.netease.hearttouch.candywebcache.cachemanager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by netease on 16/6/3.
 */
public class WebappInfo {
    public static final int WEBAPP_STATUS_INIT = 1;
    public static final int WEBAPP_STATUS_LOCAL_LOAD_START = 2;
    public static final int WEBAPP_STATUS_LOCAL_LOAD_FAILED = 3;

    public static final int WEBAPP_STATUS_UPDATE_START = 4;
    public static final int WEBAPP_STATUS_OUT_OF_DATE = 5;

    public static final int WEBAPP_STATUS_DELETE_START = 6;
    public static final int WEBAPP_STATUS_DELETED = 7;

    public static final int WEBAPP_STATUS_AVAILABLE = 8;
    public static final int WEBAPP_STATUS_BROKEN = 9;

    public final String mWebappName;
    public final String mCachedDirPath;

    public final String mVerStr;
    public final long mVerNum;

    public final String mPkgFilePath;
    public final String mPkgFileMd5;
    public final String mFullUrl;

    public final Set<String> mDomains;

    public final Map<String, FileInfo> mFileInfos;
    public final long mCacheSize;

    public final int mStatus;
    public int mFileNumber;

    public WebappInfo(List<String> domains, String fullUrl, String appName, String verStr, String cachedDirPath,
                      long cacheSize, String pkgFilePath, String md5, int status) {
        mDomains = new HashSet(domains);
        mFullUrl = fullUrl;

        mWebappName = appName;
        mVerStr = verStr;
        mVerNum = Long.parseLong(mVerStr);

        mCachedDirPath = cachedDirPath;
        mCacheSize = cacheSize;

        mPkgFilePath = pkgFilePath;
        mPkgFileMd5 = md5;
        mFileInfos = new HashMap<>();

        mStatus = status;
    }

    public WebappInfo(Set<String> domains, String fullUrl, String appName, String verStr, String cachedDirPath,
                      long cacheSize, String pkgFilePath, String md5, int status, Map<String, FileInfo> fileInfos) {
        mDomains = new HashSet(domains);
        mFullUrl = fullUrl;

        mWebappName = appName;
        mVerStr = verStr;
        mVerNum = Long.parseLong(mVerStr);

        mCachedDirPath = cachedDirPath;
        mCacheSize = cacheSize;

        mPkgFilePath = pkgFilePath;
        mPkgFileMd5 = md5;
        mFileInfos = fileInfos;

        mStatus = status;
    }

    public boolean inProcessStatus() {
        return mStatus == WEBAPP_STATUS_LOCAL_LOAD_START ||
                mStatus == WEBAPP_STATUS_LOCAL_LOAD_START ||
                mStatus == WEBAPP_STATUS_LOCAL_LOAD_START;
    }

    public boolean isInvalid() {
        return mStatus == WebappInfo.WEBAPP_STATUS_LOCAL_LOAD_FAILED ||
                mStatus == WebappInfo.WEBAPP_STATUS_OUT_OF_DATE ||
                mStatus == WebappInfo.WEBAPP_STATUS_DELETED ||
                mStatus == WebappInfo.WEBAPP_STATUS_BROKEN;
    }
}
