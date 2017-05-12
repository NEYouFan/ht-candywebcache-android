package com.netease.hearttouch.candywebcache.cachemanager;

import android.content.Context;
import android.text.TextUtils;

import com.netease.hearttouch.brotlij.Brotli;
import com.netease.hearttouch.candywebcache.WebcacheLog;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Created by netease on 16/6/3.
 */
public class CacheManager {
    private static final String TAG = "CacheManager";

    public static final int DIFF_SUCCESS = 0;
    public static final int DIFF_ERROR_UNKNOWN = 1;
    public static final int DIFF_ERROR_INVALID_LOCAL_FILE = 2;
    public static final int DIFF_ERROR_INVALID_DIFF_FILE = 3;
    public static final int DIFF_ERROR_DIFF_FILE_UNSUPPORTED = 4;
    public static final int DIFF_ERROR_DIFFAPPLIED_FAILED = 5;
    public static final int DIFF_ERROR_INVALID_PATCHED_FILE = 6;

    private static final String DATABASE_FOLDER_NAME = "database";

    public enum PkgType {
        PKG_ZIP_WITH_BSDIFF, PKG_ZIP_WITH_COURGETTE,
        PKG_ZIP, PKG_ZIP_WITH_GZIP, PKG_ZIP_WITH_BROTLI,
    }

    private final String mProtectedFilesDirPath;
    private final String mCacheFilesDirPath;
    private final Set<String> mUncachedFileTypes;

    private Context mContext;
    private ExecutorService mExecutorService;

    private DatabaseManager mDatabaseManager;
    private CountDownLatch mInitializationLatch;
    private Cache mCache;

    private Map<String, WebappInfo> mDomainWebappInfos;
    private Map<String, String> mUrlToDomain;
    private Map<String, WebappInfo> mWebappInfos;

    public CacheManager(Context context, String protectedDirPath, String cacheDir, List<String> uncachedFileTypes,
                        int maxCacheSizeInBytes) {
        mProtectedFilesDirPath = protectedDirPath;
        mCacheFilesDirPath = cacheDir;
        mUncachedFileTypes = new HashSet(uncachedFileTypes);

        mContext = context.getApplicationContext();
        mExecutorService = Executors.newFixedThreadPool(1);
        mInitializationLatch = new CountDownLatch(1);
        mCache = new MemBasedCache(maxCacheSizeInBytes);

        mWebappInfos = new HashMap<String, WebappInfo>();
        mDomainWebappInfos = new HashMap<String, WebappInfo>();
        mUrlToDomain = new HashMap<String, String>();
        startLoadLocalCacheInfo();
    }

    private void startLoadLocalCacheInfo() {
        Runnable task = new LocalCacheInfoLoadTask();
        mExecutorService.execute(task);
    }

    private class LocalCacheInfoLoadTask implements Runnable {
        @Override
        public void run() {
            WebcacheLog.d("Load local cache file info start.");
            synchronized(CacheManager.this) {
                mDatabaseManager = new DatabaseManager(mContext, mProtectedFilesDirPath + File.separator + DATABASE_FOLDER_NAME);
                List<WebappInfo> appInfos = mDatabaseManager.getAllWebappInfo();

                for (WebappInfo appinfo : appInfos) {
                    int newStatus = 0;
                    if (appinfo.mStatus == WebappInfo.WEBAPP_STATUS_LOCAL_LOAD_START) {
                        newStatus = WebappInfo.WEBAPP_STATUS_LOCAL_LOAD_FAILED;
                    } else if (appinfo.mStatus == WebappInfo.WEBAPP_STATUS_UPDATE_START) {
                        newStatus = WebappInfo.WEBAPP_STATUS_OUT_OF_DATE;
                    } else if (appinfo.mStatus == WebappInfo.WEBAPP_STATUS_DELETE_START) {
                        newStatus = WebappInfo.WEBAPP_STATUS_OUT_OF_DATE;
                    } else if (appinfo.mStatus == WebappInfo.WEBAPP_STATUS_AVAILABLE) {
                        File pkgFile = new File(appinfo.mPkgFilePath);
                        if (!pkgFile.exists()) {
                            WebcacheLog.d("Pkg file " + appinfo.mPkgFilePath + " is lost.");
                            newStatus = WebappInfo.WEBAPP_STATUS_BROKEN;
                        }
                    }
                    if (newStatus != 0) {
                        appinfo = new WebappInfo(appinfo.mDomains, appinfo.mFullUrl, appinfo.mWebappName, appinfo.mVerStr,
                                appinfo.mCachedDirPath, appinfo.mCacheSize, appinfo.mPkgFilePath, appinfo.mPkgFileMd5,
                                newStatus, appinfo.mFileInfos);
                        mDatabaseManager.updateWebappInfo(appinfo);
                    }

                    mWebappInfos.put(appinfo.mWebappName, appinfo);
                    Iterator<String> domains = appinfo.mDomains.iterator();
                    while (domains.hasNext()) {
                        String domain = domains.next();
                        if (!TextUtils.isEmpty(domain)) {
                            mDomainWebappInfos.put(domain, appinfo);
                        }
                    }
                }
                List<FileInfo> fileinfos = mDatabaseManager.getFileInfos();
                cacheFileInfosLocked(fileinfos);
                mInitializationLatch.countDown();
            }
            WebcacheLog.d("Load local cache file info end.");
        }
    }

    private File getAppDir(String appname) {
        File appDir = new File(mCacheFilesDirPath, appname);
        return appDir;
    }

    private File getAppFilesDir(String appname) {
        File appDir = getAppDir(appname);
        File appFilesDir = new File(appDir, "res");
        return appFilesDir;
    }

    private synchronized boolean updateWebappInfo(WebappInfo webappInfo) {
        if (webappInfo == null || TextUtils.isEmpty(webappInfo.mWebappName)) {
            return false;
        }
        WebappInfo oldWebappInfo = mWebappInfos.get(webappInfo.mWebappName);
        if (oldWebappInfo == null) {
            if (!mDatabaseManager.insertWebappInfo(webappInfo)) {
                return false;
            }
        } else {
            if (!mDatabaseManager.updateWebappInfo(webappInfo)) {
                return false;
            }
        }
        mWebappInfos.put(webappInfo.mWebappName, webappInfo);
        for (String domain : webappInfo.mDomains) {
            if (!TextUtils.isEmpty(domain)) {
                mDomainWebappInfos.put(domain, webappInfo);
            }
        }
        return true;
    }

    private void clearWebappCacheDir(String dirpath) {
        File appdir = new File(dirpath);
        File[] files = appdir.listFiles();
        if (files != null && files.length > 0) {
            for (File file : files) {
                if (file.isDirectory()) {
                    FileUtils.deleteDir(file);
                }
            }
        }
    }

    private void cacheFileInfosLocked(List<FileInfo> fileInfos) {
        for (FileInfo fileinfo : fileInfos) {
            WebappInfo webappInfo = mWebappInfos.get(fileinfo.mAppname);
            if (webappInfo != null) {
                webappInfo.mFileInfos.put(fileinfo.mUrl, fileinfo);
            }
        }
    }

    private boolean checkDatabaseManager() {
        if (mDatabaseManager != null) {
            return true;
        }
        try {
            mInitializationLatch.await(500, TimeUnit.MILLISECONDS);
            if (mDatabaseManager != null) {
                return true;
            }
        } catch (InterruptedException e) {
        }
        return false;
    }

    private long createIndex(String appname, WebappInfo info, Map<String, String> fileMd5List) {
        if (WebcacheLog.DEBUG) {
            WebcacheLog.d("%s", "Create index for " + appname + " start.");
        }
        File appFilesDir = getAppFilesDir(appname);
        if (info == null || !appFilesDir.exists()) {
            return -1;
        }
        String appFilesDirPath = appFilesDir.getAbsolutePath();
        List<FileInfo> fileInfos = new ArrayList<FileInfo>();
        long totalSize = 0;
        for (String relPath : fileMd5List.keySet()) {
            String md5 = fileMd5List.get(relPath);
            String localPath = appFilesDirPath + File.separator + relPath;

            String url = relPath;
            if (!File.separator.equals('/')) {
                String[] pathElems = relPath.split(File.separator);
                StringBuilder urlBuilder = new StringBuilder(pathElems[0]);
                for (int i = 1; i < pathElems.length; ++i) {
                    urlBuilder.append('/');
                    urlBuilder.append(pathElems[i]);
                }
                url = urlBuilder.toString();
            }

            FileInfo fileInfo = new FileInfo(md5, localPath, appname, url, 0, FileInfo.FILE_STATUS_GOOD);
            fileInfos.add(fileInfo);

            File file = new File(localPath);
            totalSize += file.length();
        }

        synchronized (this) {
            for (FileInfo fileinfo : fileInfos) {
                info.mFileInfos.put(fileinfo.mUrl, fileinfo);
            }
        }
        if (WebcacheLog.DEBUG) {
            WebcacheLog.d("%s", "Create index for " + appname + " end, begin to insert to database.");
        }
        if (checkDatabaseManager() && !mDatabaseManager.updateFileInfos(fileInfos)) {
            totalSize = -1;
        }
        if (WebcacheLog.DEBUG) {
            WebcacheLog.d("%s", "Create index for " + appname + " end, total size " + totalSize);
        }
        return totalSize;
    }

    private long unpackAndCreateIndex(String appname, WebappInfo webappInfo) {
        File appDir = getAppDir(appname);
        clearWebappCacheDir(appDir.getAbsolutePath());

        File appFilesDir = getAppFilesDir(appname);
        if (!appFilesDir.exists()) {
            appFilesDir.mkdirs();
        }

        WebcacheLog.d("%s", "Unpack package " + webappInfo.mPkgFilePath + " start.");
        Map<String, String> fileMd5List = new HashMap<String, String>();
        if (!FileUtils.unpackPackage(appFilesDir, webappInfo.mPkgFilePath, fileMd5List)) {
            WebcacheLog.d("%s", "Unpack package " + webappInfo.mPkgFilePath + " failed.");
            clearWebappCacheDir(appDir.getAbsolutePath());
            return -1;
        } else {
            if (WebcacheLog.DEBUG) {
                WebcacheLog.d("%s", "Unpack package " + webappInfo.mPkgFilePath + " end.");
            }
        }

        return createIndex(appname, webappInfo, fileMd5List);
    }

    private class LocalPackageLoadTask implements Runnable {
        private final String mFileName;
        private final String mVersion;
        private final String mMd5;
        private final List<String> mAppDomains;
        private final InputStream mIs;

        public LocalPackageLoadTask(String fileName, String version, String md5, List<String> appDomains, InputStream is) {
            mFileName = fileName;
            mVersion = version;
            mMd5 = md5;
            mAppDomains = appDomains;
            mIs = is;
        }

        private String copyLocalFile(String filename, String md5, InputStream is) {
            String appname = FileUtils.getPrimaryFileName(filename);

            File appDir = getAppDir(appname);
            if (appDir.exists()) {
                FileUtils.deleteDir(appDir);
            }
            if (!appDir.mkdirs()) {
                WebcacheLog.d("%s", "Create dir failed: " + appDir.getAbsolutePath());
                return null;
            }

            File pkgFile = new File(appDir, filename);
            String strDigest = FileUtils.copyFile(is, pkgFile);
            if (!strDigest.equalsIgnoreCase(md5)) {
                WebcacheLog.d("MD5 validation check failed");
                strDigest = null;
            }
            if (strDigest == null) {
                return null;
            }
            return pkgFile.getAbsolutePath();
        }

        @Override
        public void run() {
            WebcacheLog.d("%s", "Load local package " + mFileName + " start.");
            int waitCount = 3;
            while (waitCount-- > 0 && !checkDatabaseManager()) {
            }
            if (!checkDatabaseManager()) {
                return;
            }
            String appname = FileUtils.getPrimaryFileName(mFileName);
            File appDir = getAppDir(appname);
            String fullMd5 = EnDecryptionUtils.decode(mMd5);
            WebappInfo webappInfo;
            synchronized(CacheManager.this) {
                webappInfo = getWebappInfoLocked(appname);
                if (webappInfo != null) {
                    if (!mVersion.equalsIgnoreCase(webappInfo.mVerStr)) {
                        return;
                    }
                    int status = webappInfo.mStatus;
                    if (status != WebappInfo.WEBAPP_STATUS_LOCAL_LOAD_FAILED && status != WebappInfo.WEBAPP_STATUS_BROKEN) {
                        WebcacheLog.d("Load local package failed: error status " + status);
                        return;
                    }
                }

                webappInfo = new WebappInfo(mAppDomains, null, appname, mVersion, appDir.getAbsolutePath(), 0,
                        null, fullMd5, WebappInfo.WEBAPP_STATUS_LOCAL_LOAD_START);
                if (!updateWebappInfo(webappInfo)) {
                    return;
                }
            }
            WebcacheLog.d("%s", "Load local package " + mFileName + " copy local file start.");
            String pkgFilePath = copyLocalFile(mFileName, fullMd5, mIs);
            if (pkgFilePath == null) {
                WebcacheLog.d("Load local package failed: copy local file failed.");
                webappInfo = new WebappInfo(mAppDomains, null, appname, mVersion, appDir.getAbsolutePath(), 0,
                        pkgFilePath, fullMd5, WebappInfo.WEBAPP_STATUS_LOCAL_LOAD_FAILED);
                updateWebappInfo(webappInfo);
                return;
            }
            File pkgFile = new File(pkgFilePath);
            webappInfo = new WebappInfo(mAppDomains, null, appname, mVersion, appDir.getAbsolutePath(), pkgFile.length(),
                    pkgFilePath, fullMd5, WebappInfo.WEBAPP_STATUS_LOCAL_LOAD_START);
            long totalFileSize = unpackAndCreateIndex(appname, webappInfo);
            if (totalFileSize <= 0) {
                WebcacheLog.d("Load local package failed: unpack and create index failed.");
                clearWebappCacheDir(appDir.getAbsolutePath());
                webappInfo = new WebappInfo(mAppDomains, null, appname, mVersion, appDir.getAbsolutePath(), pkgFile.length(),
                        pkgFilePath, fullMd5, WebappInfo.WEBAPP_STATUS_LOCAL_LOAD_FAILED);
                updateWebappInfo(webappInfo);
            } else {
                WebcacheLog.d("%s", "Load local package " + mFileName + " end successfully.");
                totalFileSize += pkgFile.length();
                webappInfo = new WebappInfo(webappInfo.mDomains, webappInfo.mFullUrl, webappInfo.mWebappName, webappInfo.mVerStr,
                        webappInfo.mCachedDirPath, totalFileSize, webappInfo.mPkgFilePath, webappInfo.mPkgFileMd5,
                        WebappInfo.WEBAPP_STATUS_AVAILABLE, webappInfo.mFileInfos);
                updateWebappInfo(webappInfo);
            }

            try {
                mIs.close();
            } catch (IOException e) {
            }
        }
    }

    public boolean loadLocalPackage(String fileName, String version, String md5, List<String> appDomains, InputStream is) {
        LocalPackageLoadTask task = new LocalPackageLoadTask(fileName, version, md5, appDomains, is);
        task.run();
        return true;
    }

    private WebappInfo getWebappInfoLocked(String appid) {
        WebappInfo webappInfo = mWebappInfos.get(appid);
        return webappInfo;
    }

    public synchronized WebappInfo getWebappInfo(String appid) {
        if (!checkDatabaseManager()) {
            return null;
        }
        WebappInfo webappInfo = getWebappInfoLocked(appid);
        if (webappInfo == null) {
            return null;
        } else if (webappInfo.mStatus == WebappInfo.WEBAPP_STATUS_DELETED) {
            return null;
        }
        return webappInfo;
    }

    public synchronized List<WebappInfo> getAllWebappInfo() {
        if (!checkDatabaseManager()) {
            return null;
        }
        List<WebappInfo> webappInfos = new ArrayList<>();
        for (String appid : mWebappInfos.keySet()) {
            WebappInfo webappInfo = mWebappInfos.get(appid);
            if (webappInfo.mStatus != WebappInfo.WEBAPP_STATUS_DELETED) {
                webappInfos.add(webappInfo);
            }
        }
        return webappInfos;
    }

    private class PatchApplyer {
        private final String mAppId;
        private final PkgType mPkgType;
        private final String mFullUrl;
        private final String mVerStr;
        private final List<String> mDomains;

        private String mPatchFilePath;
        private String mDiffMD5;
        private String mFullMD5;

        private WebappInfo mWebappInfo;

        private File mAppDir;
        private String mPkgExtName;

        private File mTmpPkgFile;
        private File mTmpCacheDir;

        public PatchApplyer(String appid, PkgType type, String patchFilePath, String diffMd5, String fullMd5,
                            String fullUrl, String verStr, List<String> domains) {
            mAppId = appid;
            mPkgType = type;
            mPatchFilePath = patchFilePath;
            mFullUrl = fullUrl;
            mVerStr = verStr;
            mDomains = domains;

            mDiffMD5 = diffMd5;
            mFullMD5 = fullMd5;

            mAppDir = getAppDir(appid);
        }

        private boolean enterUpdatingStatus() {
            if (mPkgType == PkgType.PKG_ZIP_WITH_BROTLI) {
                String decompressedFilePath = mPatchFilePath.substring(0, mPatchFilePath.lastIndexOf("."));
                Brotli.decompressFile(mPatchFilePath, decompressedFilePath);
                File origFile = new File(mPatchFilePath);
                origFile.delete();
                mPatchFilePath = decompressedFilePath;
            }
            boolean newApp;
            if (mWebappInfo == null || TextUtils.isEmpty(mWebappInfo.mPkgFilePath)) { // Full package update.
                mPkgExtName = FileUtils.getExtName(mPatchFilePath);
                File targetFile = new File(mAppDir, mAppId + mPkgExtName);
                mWebappInfo = new WebappInfo(mDomains, mFullUrl, mAppId, mVerStr, mAppDir.getAbsolutePath(), 0,
                        targetFile.getAbsolutePath(), mFullMD5, WebappInfo.WEBAPP_STATUS_UPDATE_START);
                newApp = true;
                if (WebcacheLog.DEBUG) {
                    WebcacheLog.d("%s", "New webapp " + mAppId + " update start ");
                }
            } else {
                mPkgExtName = FileUtils.getExtName(mWebappInfo.mPkgFilePath);
                mWebappInfo = new WebappInfo(mWebappInfo.mDomains, mWebappInfo.mFullUrl, mWebappInfo.mWebappName, mWebappInfo.mVerStr,
                        mWebappInfo.mCachedDirPath, mWebappInfo.mCacheSize, mWebappInfo.mPkgFilePath, mWebappInfo.mPkgFileMd5,
                        WebappInfo.WEBAPP_STATUS_UPDATE_START, mWebappInfo.mFileInfos);
                newApp = false;
                if (WebcacheLog.DEBUG) {
                    WebcacheLog.d("%s", "Exist webapp " + mAppId + " update start ");
                }
            }
            return newApp;
        }

        private int installNewApp() {
            if (Md5Utils.checkFileValidation(mPatchFilePath, mFullMD5)) {
                FileUtils.deleteDir(mAppDir);
                mAppDir.mkdirs();

                File targetFile = new File(mAppDir, mAppId + mPkgExtName);
                File fullPkgFile = new File(mPatchFilePath);
                if (fullPkgFile.renameTo(targetFile)) {
                    long totalFileSize = unpackAndCreateIndex(mAppId, mWebappInfo);
                    if (totalFileSize > 0) {
                        totalFileSize += targetFile.length();
                        mWebappInfo = new WebappInfo(mWebappInfo.mDomains, mWebappInfo.mFullUrl, mWebappInfo.mWebappName,
                                mWebappInfo.mVerStr, mWebappInfo.mCachedDirPath, totalFileSize, mWebappInfo.mPkgFilePath,
                                mWebappInfo.mPkgFileMd5, WebappInfo.WEBAPP_STATUS_AVAILABLE, mWebappInfo.mFileInfos);
                        updateWebappInfo(mWebappInfo);
                        WebcacheLog.d("%s", "New webapp " + mAppId + " update end successfully.");
                        return DIFF_SUCCESS;
                    }
                }
            }

            clearWebappCacheDir(mWebappInfo.mCachedDirPath);
            mWebappInfo = new WebappInfo(mDomains, mFullUrl, mAppId, mVerStr, mWebappInfo.mCachedDirPath, mWebappInfo.mCacheSize,
                    mWebappInfo.mPkgFilePath, mFullMD5, WebappInfo.WEBAPP_STATUS_OUT_OF_DATE);
            updateWebappInfo(mWebappInfo);
            return DIFF_ERROR_DIFFAPPLIED_FAILED;
        }

        // To clear out of date files.
        private void clearOldTmpFiles() {
            mTmpPkgFile = new File(mAppDir, "tmp" + mAppId + mPkgExtName);
            if (mTmpPkgFile.exists()) {
                mTmpPkgFile.delete();
            }
            mTmpCacheDir = new File(mAppDir, "tmpcache_" + mAppId);
            if (mTmpCacheDir.exists()) {
                FileUtils.deleteDir(mTmpCacheDir);
            }
            mTmpCacheDir.mkdirs();
        }

        private int applyDiff() {
            File patchFile = new File(mPatchFilePath);
            if (!Md5Utils.checkFileValidation(mWebappInfo.mPkgFilePath, mWebappInfo.mPkgFileMd5)) {
                WebcacheLog.d("%s", "Apply diff patch for " + mAppId + " failed: check local package md5 failed");
                deleteWebappResource(mAppId);
                mDatabaseManager.deleteWebappInfo(mAppId);
                patchFile.delete();

                return DIFF_ERROR_INVALID_LOCAL_FILE;
            }
            mDiffMD5 = EnDecryptionUtils.decode(mDiffMD5);
            if (!Md5Utils.checkFileValidation(mPatchFilePath, mDiffMD5)) {
                WebcacheLog.d("%s", "Apply diff patch for " + mAppId + " failed: check patch file md5 failed");
                patchFile.delete();
                return DIFF_ERROR_INVALID_DIFF_FILE;
            }

            Patcher patcher = new BsdiffPatcher();
            String tmpPkgFilePath = patcher.applyPatch(mWebappInfo.mPkgFilePath, mPatchFilePath, mTmpPkgFile.getAbsolutePath());

            patchFile.delete();

            if (tmpPkgFilePath == null) {
                WebcacheLog.d("%s", "Apply diff patch for " + mAppId + " failed: merge patch file failed");
                return DIFF_ERROR_DIFFAPPLIED_FAILED;
            }
            if (!Md5Utils.checkFileValidation(tmpPkgFilePath, mFullMD5)) {
                WebcacheLog.d("%s", "Apply diff patch for " + mAppId + " failed: patched(full package) file md5 check failed");
                File newFile = new File(tmpPkgFilePath);
                newFile.delete();
                return DIFF_ERROR_INVALID_PATCHED_FILE;
            }
            return DIFF_SUCCESS;
        }

        private void backupOldFiles() {
            File pkgFile = new File(mWebappInfo.mPkgFilePath);
            String primName = FileUtils.getPrimaryFileName(mWebappInfo.mPkgFilePath);
            File bakPkgFile = new File(mAppDir, primName + "_bak" + mPkgExtName);
            if (pkgFile.exists()) {
                pkgFile.renameTo(bakPkgFile);
            }


            File cacheFilesBackupDir = new File(mAppDir, mAppId + "_bak");
            File appFilesDir = getAppFilesDir(mAppId);
            if (appFilesDir.exists()) {
                appFilesDir.renameTo(cacheFilesBackupDir);
            }
        }

        private void deleteBackupedOldFiles() {
            String primName = FileUtils.getPrimaryFileName(mWebappInfo.mPkgFilePath);
            File bakPkgFile = new File(mAppDir, primName + "_bak" + mPkgExtName);
            if (bakPkgFile.exists()) {
                bakPkgFile.delete();
            }
            File cacheFilesBackupDir = new File(mAppDir, mAppId + "_bak");
            FileUtils.deleteDir(cacheFilesBackupDir);
        }

        public int applyPatch() {
            boolean newApp;
            synchronized(CacheManager.this) {
                mWebappInfo = getWebappInfoLocked(mAppId);
                if (mWebappInfo == null) {
                    if (mPkgType != PkgType.PKG_ZIP && mPkgType != PkgType.PKG_ZIP_WITH_GZIP && mPkgType != PkgType.PKG_ZIP_WITH_BROTLI) {
                        WebcacheLog.d("%s", "Apply patch for " + mAppId + " failed: the webapp is not existed, need full update.");
                        return DIFF_ERROR_INVALID_LOCAL_FILE;
                    }
                } else {
                    if (mWebappInfo.inProcessStatus()) {
                        WebcacheLog.d("%s", "Apply patch for " + mAppId + " failed: error status " + mWebappInfo.mStatus);
                        return DIFF_ERROR_UNKNOWN;
                    }
                    if (mWebappInfo.isInvalid()) {
                        if (mPkgType != PkgType.PKG_ZIP && mPkgType != PkgType.PKG_ZIP_WITH_GZIP && mPkgType != PkgType.PKG_ZIP_WITH_BROTLI) {
                            WebcacheLog.d("%s", "Apply patch for " + mAppId + " failed: status " + mWebappInfo.mStatus + " need full update");
                            return DIFF_ERROR_INVALID_LOCAL_FILE;
                        }
                    }
                }

                mFullMD5 = EnDecryptionUtils.decode(mFullMD5);
                newApp = enterUpdatingStatus();
                updateWebappInfo(mWebappInfo);
            }

            if (newApp) {
                return installNewApp();
            } else {
                clearOldTmpFiles();

                WebappInfo failedWebappInfo = new WebappInfo(mWebappInfo.mDomains, mWebappInfo.mFullUrl, mWebappInfo.mWebappName,
                        mWebappInfo.mVerStr, mWebappInfo.mCachedDirPath, mWebappInfo.mCacheSize, mWebappInfo.mPkgFilePath,
                        mWebappInfo.mPkgFileMd5, WebappInfo.WEBAPP_STATUS_OUT_OF_DATE, mWebappInfo.mFileInfos);
                if (mPkgType == PkgType.PKG_ZIP_WITH_BSDIFF) {
                    int res = applyDiff();
                    if (res != DIFF_SUCCESS) {
                        updateWebappInfo(failedWebappInfo);
                        return res;
                    }
                } else {
                    File fullPkgFile = new File(mPatchFilePath);
                    if (!fullPkgFile.renameTo(mTmpPkgFile)) {
                        WebcacheLog.d("%s", "Apply full package for " + mAppId + " failed: move downloaded file failed.");
                        fullPkgFile.delete();
                        updateWebappInfo(failedWebappInfo);
                        return DIFF_ERROR_DIFFAPPLIED_FAILED;
                    }
                    if (!Md5Utils.checkFileValidation(mTmpPkgFile.getAbsolutePath(), mFullMD5)) {
                        WebcacheLog.d("%s", "Apply full package for " + mAppId + " failed: full package md5 check failed.");
                        fullPkgFile.delete();
                        updateWebappInfo(failedWebappInfo);
                        return DIFF_ERROR_INVALID_PATCHED_FILE;
                    }
                }
                WebcacheLog.d("%s", "Unpack package " + mTmpPkgFile.getAbsolutePath() + " to " +
                        mTmpCacheDir.getAbsolutePath() + " start.");
                Map<String, String> fileMd5List = new HashMap<String, String>();
                if (!FileUtils.unpackPackage(mTmpCacheDir, mTmpPkgFile.getAbsolutePath(), fileMd5List)) {
                    WebcacheLog.d("%s", "Unpack package " + mTmpPkgFile.getAbsolutePath() + " to " +
                            mTmpCacheDir.getAbsolutePath() + " failed.");
                    FileUtils.deleteDir(mTmpCacheDir);
                    mTmpPkgFile.delete();
                    updateWebappInfo(failedWebappInfo);

                    return DIFF_ERROR_DIFFAPPLIED_FAILED;
                }

                backupOldFiles();

                File pkgFile = new File(mWebappInfo.mPkgFilePath);
                mTmpPkgFile.renameTo(pkgFile);

                File appFilesDir = getAppFilesDir(mAppId);
                mTmpCacheDir.renameTo(appFilesDir);

                long totalFileSize = createIndex(mAppId, mWebappInfo, fileMd5List);
                totalFileSize += pkgFile.length();
                mWebappInfo = new WebappInfo(new HashSet(mDomains), mFullUrl, mAppId, mVerStr, mAppDir.getAbsolutePath(), totalFileSize,
                        mWebappInfo.mPkgFilePath, mFullMD5, WebappInfo.WEBAPP_STATUS_AVAILABLE, mWebappInfo.mFileInfos);
                updateWebappInfo(mWebappInfo);

                deleteBackupedOldFiles();
                if (WebcacheLog.DEBUG) {
                    WebcacheLog.d("%s", "Apply patch for " + mAppId + " end successfully.");
                }
            }

            return DIFF_SUCCESS;
        }
    }

    public int applyPatch(String appid, PkgType type, String patchFilePath, String diffMd5, String fullMd5,
                          String fullUrl, String verStr, List<String> domains) {
        if (!checkDatabaseManager()) {
            WebcacheLog.d("%s", "Apply patch for " + appid + " failed: " + " CacheManager has not been initialized completed.");
            return DIFF_ERROR_UNKNOWN;
        }
        PatchApplyer applyer = new PatchApplyer(appid, type, patchFilePath, diffMd5, fullMd5, fullUrl, verStr, domains);
        return applyer.applyPatch();
    }

    public void updateWebappInfo(String appid, String fullUrl, String verStr, List<String> domains) {
        if (!checkDatabaseManager()) {
            return;
        }
        synchronized(this) {
            WebappInfo webappInfo = getWebappInfoLocked(appid);
            if (webappInfo == null) {
                return;
            }
            try {
                long verInt = Long.parseLong(verStr);
                if (verInt != webappInfo.mVerNum) {
                    return;
                }
            } catch (NumberFormatException e) {
                return;
            }
            Set<String> domainSet = new HashSet(domains);
            WebappInfo newWebappInfo = new WebappInfo(domainSet, fullUrl, webappInfo.mWebappName, webappInfo.mVerStr,
                    webappInfo.mCachedDirPath, webappInfo.mCacheSize, webappInfo.mPkgFilePath, webappInfo.mPkgFileMd5,
                    webappInfo.mStatus, webappInfo.mFileInfos);
            updateWebappInfo(newWebappInfo);
        }
    }

    private String getWithoutSchemaUrl(String urlstr) {
        String schemeSpecificPart = urlstr;
        if (schemeSpecificPart.startsWith("http://")) {
            schemeSpecificPart = schemeSpecificPart.substring("http://".length());
        } else if (schemeSpecificPart.startsWith("https://")) {
            schemeSpecificPart = schemeSpecificPart.substring("https://".length());
        }
        if (schemeSpecificPart.startsWith("//")) {
            schemeSpecificPart = schemeSpecificPart.substring("//".length());
        }
        return schemeSpecificPart;
    }

    private String getDomainFromUrlLocked(String schemeSpecificPart) {
        Iterator<String> allDomains = mDomainWebappInfos.keySet().iterator();
        String targetDomain = null;
        while (allDomains.hasNext()) {
            String domain = allDomains.next();
            if (schemeSpecificPart.startsWith(domain)) {
                targetDomain = domain;
                break;
            }
        }
        return targetDomain;
    }

    public String getAppidFromUrl(String urlStr) {
        String schemeSpecificPart = getWithoutSchemaUrl(urlStr);
        String appid = null;
        synchronized(this) {
            String domain;
            if (mUrlToDomain.containsKey(schemeSpecificPart)) {
                domain = mUrlToDomain.get(schemeSpecificPart);
            } else {
                domain = getDomainFromUrlLocked(schemeSpecificPart);
                mUrlToDomain.put(schemeSpecificPart, domain);
            }
            if (domain != null) {
                WebappInfo webappInfo = mDomainWebappInfos.get(domain);
                appid = webappInfo.mWebappName;
            }
        }
        return appid;
    }

    private Cache.Entry loadFile(FileInfo fileInfo) {
        Cache.Entry entry = null;
        DigestInputStream dis = null;
        try {
            File file = new File(fileInfo.mLocalPath);

            InputStream is = new FileInputStream(file);
            MessageDigest md5Digest = MessageDigest.getInstance("MD5");
            dis = new DigestInputStream(is, md5Digest);

            byte[] data = new byte[(int) file.length()];
            int readLength = dis.read(data);

            if (readLength == file.length()) {
                byte[] digest = dis.getMessageDigest().digest();
                String digestStr = Md5Utils.toHexString(digest);
                if (digestStr.equals(fileInfo.mMd5)) {
                    entry = new Cache.Entry();
                    entry.data = data;
                    String key = fileInfo.mAppname + File.separator + fileInfo.mUrl;
                    mCache.put(key, entry);
                } else {
                    if (checkDatabaseManager()) {
                        fileInfo.setStatus(FileInfo.FILE_STATUS_BROKEN);
                        mDatabaseManager.updateFileInfo(fileInfo);
                    }
                }
            }
        } catch (FileNotFoundException e) {
        } catch (NoSuchAlgorithmException e) {
        } catch (IOException e) {
        } finally {
            if (dis != null) {
                try {
                    dis.close();
                } catch (IOException e) {
                }
            }
        }
        return entry;
    }

    public InputStream getResource(String urlStr) {
        String domain;
        String schemeSpecificPart = getWithoutSchemaUrl(urlStr);
        synchronized(this) {
            if (mUrlToDomain.containsKey(schemeSpecificPart)) {
                domain = mUrlToDomain.get(schemeSpecificPart);
            } else {
                domain = getDomainFromUrlLocked(schemeSpecificPart);
                mUrlToDomain.put(schemeSpecificPart, domain);
            }
        }
        if (domain == null) {
            WebcacheLog.d("Unknown domain name.");
            return null;
        }

        String fileUrl = schemeSpecificPart.substring(domain.length() + 1);
        String appid;
        FileInfo fileInfo;
        synchronized(this) {
            WebappInfo webappInfo = mDomainWebappInfos.get(domain);
            if (webappInfo == null || webappInfo.mStatus != WebappInfo.WEBAPP_STATUS_AVAILABLE) {
                WebcacheLog.d("The webapp is not available now...");
                return null;
            }
            appid = webappInfo.mWebappName;
            fileInfo = webappInfo.mFileInfos.get(fileUrl);
        }
        if (fileInfo == null || fileInfo.getStatus() == FileInfo.FILE_STATUS_BROKEN) {
            WebcacheLog.d("%s", "File not exist or broken: " + urlStr);
            return null;
        }

        String extName = FileUtils.getExtName(fileInfo.mLocalPath);
        if (extName != null && mUncachedFileTypes.contains(extName)) {
            WebcacheLog.d("%s", "It is an uncachedFile: " + urlStr);
            return null;
        }

        ByteArrayInputStream bais = null;
        String key = appid + File.separator + fileInfo.mUrl;
        Cache.Entry entry = mCache.get(key);
        if (entry == null) {
            WebcacheLog.d("%s", "Load from file: " + urlStr);
            entry = loadFile(fileInfo);
        }
        if (entry != null) {
            WebcacheLog.i("%s", "Cache hit: " + urlStr);
            fileInfo.increaseAccessCount();
            bais = new ByteArrayInputStream(entry.data);
        }

        return bais;
    }

    private interface FileInfoFilter {
        boolean filter(FileInfo fileInfo);
    }

    private class AppidFileInfoFilter implements FileInfoFilter {
        private final String mAppname;

        public AppidFileInfoFilter(String appname) {
            mAppname = appname;
        }

        @Override
        public boolean filter(FileInfo fileInfo) {
            return mAppname.equals(fileInfo.mAppname);
        }
    }

    private class DefaultFileInfoFilter implements FileInfoFilter {

        @Override
        public boolean filter(FileInfo fileInfo) {
            return true;
        }
    }

    public synchronized void clearCache(String appid) {
        WebappInfo webappInfo = mWebappInfos.get(appid);
        if (webappInfo == null) {
            return;
        }
        for (String url : webappInfo.mFileInfos.keySet()) {
            mCache.remove(appid + File.separator + url);
        }
    }

    public synchronized void clearAllCache() {
        for (WebappInfo webappInfo : mWebappInfos.values()) {
            for (FileInfo fileInfo : webappInfo.mFileInfos.values()) {
                mCache.remove(fileInfo.mAppname + File.separator + fileInfo.mUrl);
            }
        }
    }

    private void deleteWebappResource(String appId) {
        WebappInfo webappInfo;
        clearCache(appId);
        synchronized(this) {
            webappInfo = mWebappInfos.get(appId);
            if (webappInfo == null) {
                return;
            }
            webappInfo.mFileInfos.clear();
            mDatabaseManager.deleteWebappFileInfos(appId);
        }
        String appLocalPath = webappInfo.mCachedDirPath;
        FileUtils.deleteDir(appLocalPath);
    }

    public void deleteWebapp(String appId) {
        WebcacheLog.d("%s", "Start deleting:" + appId);
        WebappInfo webappInfo;
        synchronized(this) {
            webappInfo = getWebappInfoLocked(appId);
            if (webappInfo == null) {
                WebcacheLog.d("%s", "Cannot find the webapp to delete:" + appId);
                return;
            }
            int status = webappInfo.mStatus;
            if (status == WebappInfo.WEBAPP_STATUS_DELETED || status == WebappInfo.WEBAPP_STATUS_LOCAL_LOAD_START ||
                    status == WebappInfo.WEBAPP_STATUS_UPDATE_START || status == WebappInfo.WEBAPP_STATUS_DELETE_START) {
                WebcacheLog.d("%s", "The webapp " + appId + " cannot be deleted because its status is " + status);
                return;
            }
            webappInfo = new WebappInfo(webappInfo.mDomains, webappInfo.mFullUrl, webappInfo.mWebappName,
                    webappInfo.mVerStr, webappInfo.mCachedDirPath, webappInfo.mCacheSize, webappInfo.mPkgFilePath,
                    webappInfo.mPkgFileMd5, WebappInfo.WEBAPP_STATUS_DELETE_START, webappInfo.mFileInfos);
            updateWebappInfo(webappInfo);
        }
        deleteWebappResource(appId);

        webappInfo = new WebappInfo(webappInfo.mDomains, webappInfo.mFullUrl, webappInfo.mWebappName, webappInfo.mVerStr,
                webappInfo.mCachedDirPath, webappInfo.mCacheSize, webappInfo.mPkgFilePath, webappInfo.mPkgFileMd5,
                WebappInfo.WEBAPP_STATUS_DELETED, webappInfo.mFileInfos);
        updateWebappInfo(webappInfo);
        WebcacheLog.d("%s", "Deleted:" + appId);
    }

    public boolean checkDatabaseValidation() {
        if (checkDatabaseManager()) {
            return mDatabaseManager.checkDatabaseValidation();
        }
        return true;
    }

    public void closeDB() {
        if (checkDatabaseManager()) {
            mDatabaseManager.closeDB();
        }
    }
}
