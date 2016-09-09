package com.netease.hearttouch.candywebcache;

import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.MimeTypeMap;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;

import com.netease.hearttouch.candywebcache.cachemanager.CacheManager;
import com.netease.hearttouch.candywebcache.cachemanager.FileUtils;
import com.netease.hearttouch.candywebcache.cachemanager.WebappInfo;
import com.netease.hearttouch.htresourceversionchecker.VersionChecker;
import com.netease.hearttouch.htresourceversionchecker.model.AppInfo;
import com.netease.hearttouch.htresourceversionchecker.model.RequestResInfo;
import com.netease.hearttouch.htresourceversionchecker.model.ResponseResInfo;
import com.netease.hearttouch.htresourceversionchecker.model.VersionCheckResponseData;
import com.netease.hearttouch.htresourceversionchecker.model.VersionCheckResponseModel;
import com.netease.hearttouch.htfiledownloader.DownloadTask;
import com.netease.hearttouch.htfiledownloader.DownloadTaskState;
import com.netease.hearttouch.htfiledownloader.FileDownloadManager;
import com.netease.hearttouch.htfiledownloader.ProgressInfo;
import com.netease.hearttouch.htfiledownloader.StateChangeListener;
import com.netease.hearttouch.htresourceversionchecker.OnResponseListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

/**
 * Created by netease on 16/6/3.
 */
public class CandyWebCache {
    private static final String TAG = "CandyWebCache";

    private static final String PLATFORM = "android";
    private static final String WEBAPP_INFOS_FILENAME = "webapps.xml";
    private static final int START_CHECK_AND_UPDATE = 1;

    private static final String DEFAULT_CACHE_FILES_DIR_PATH = Environment.getExternalStorageDirectory().getAbsolutePath()
            + File.separator + "netease" + File.separator + "webcache";
    private static final String DOWNLOAD_DIR_NAME = "download";
    private static final String WEBAPPS_DIR_NAME = "webapps";

    private String DEFAULT_PROTECTED_FILES_DIR_PATH;

    private static final long FIRST_VERSION_CHECK_DELAY_MILLIS = 3500;
    private static final int DEFAULT_MAX_CACHE_SIZE_IN_BYTES = (int) Runtime.getRuntime().maxMemory() / 8;

    private static CandyWebCache sInstance;

    private volatile boolean mWebcacheEnabled = true;

    private Context mContext;
    private String mNativeId;
    private String mNativeVersion;
    private volatile boolean mInitialized = false;

    private VersionCheckListener mVersionCheckListener;
    private Vector<ResourceUpdateListener> mListeners = new Vector<ResourceUpdateListener>();
    ;

    private LoadLocalPackageTask mLoadLocalPackageTask;
    private Timer mVersionCheckTimer;
    private CheckAndUpdateTask mCheckAndUpdateTask;

    private volatile CacheManager mCacheManager;

    private Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case START_CHECK_AND_UPDATE: {
                    synchronized (CandyWebCache.this) {
                        if (mCheckAndUpdateTask == null) {
                            mCheckAndUpdateTask = new CheckAndUpdateTask();
                            mCheckAndUpdateTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                        }
                    }
                    break;
                }
            }
        }
    };

    private String mCheckUrl;

    private CandyWebCache() {
    }

    public static synchronized CandyWebCache getsInstance() {
        if (sInstance == null) {
            sInstance = new CandyWebCache();
        }
        return sInstance;
    }

    public void setDebugEnabled(boolean enabled) {
        WebcacheLog.DEBUG = enabled;
    }

    public void setWebcacheEnabled(boolean enabled) {
        mWebcacheEnabled = enabled;
    }

    private static class AppDigest {
        public String mAppName;
        public String mMd5;
        public String mVersion;
        public List<String> mDomains;
    }

    private class LoadLocalPackageTask extends AsyncTask<Void, Void, Void> {
        private long mCheckPeriod;
        private CacheConfig mConfig;

        public LoadLocalPackageTask(CacheConfig config) {
            mConfig = config;
        }

        private boolean createDirs(String webappsDirPath, String protectedFilesDirPath, String downloadTmpDirPath) {
            File webappDir = new File(webappsDirPath);
            if (!webappDir.exists()) {
                if (!webappDir.mkdirs()) {
                    Log.w(TAG, "Create webcacheDir failed: " + webappDir.getAbsolutePath());
                    return false;
                }
            }

            File protectedFilesDir = new File(protectedFilesDirPath);
            if (!protectedFilesDir.exists()) {
                if (!protectedFilesDir.mkdirs()) {
                    WebcacheLog.d("%s", "Create protected files dir failed: " + protectedFilesDir.getAbsolutePath());
                    return false;
                }
            }

            File downloadDir = new File(downloadTmpDirPath);
            if (!downloadDir.exists()) {
                if (!downloadDir.mkdir()) {
                    WebcacheLog.d("%s", "Create downloadDir failed: " + downloadDir.getAbsolutePath());
                    return false;
                }
            }

            return true;
        }

        private boolean loadConfig(CacheConfig config) {
            if (WebcacheLog.DEBUG) {
                WebcacheLog.d("Load config start");
            }
            List<String> uncachedFileTypes = null;

            String cacheFilesDirPath = null;
            String protectedFilesDirPath = null;
            String downloadTmpDirPath;
            String webappsDirPath;
            if (config != null) {
                protectedFilesDirPath = config.getManifestDirPath();
                cacheFilesDirPath = config.getCacheDirPath();

                uncachedFileTypes = config.getUncachedFileType();
                mCheckPeriod = config.getUpdateCheckCycle();
                WebcacheLog.d("Check period = " + mCheckPeriod);
            }
            if (protectedFilesDirPath == null) {
                protectedFilesDirPath = DEFAULT_PROTECTED_FILES_DIR_PATH;
            }
            if (cacheFilesDirPath == null) {
                cacheFilesDirPath = DEFAULT_CACHE_FILES_DIR_PATH + File.separator + mContext.getPackageName();
            }
            downloadTmpDirPath = cacheFilesDirPath + File.separator + DOWNLOAD_DIR_NAME;
            webappsDirPath = cacheFilesDirPath + File.separator + WEBAPPS_DIR_NAME;
            FileDownloadManager.init(mContext, protectedFilesDirPath + File.separator + "database", downloadTmpDirPath);

            if (!createDirs(webappsDirPath, protectedFilesDirPath, downloadTmpDirPath)) {
                return false;
            }

            if (uncachedFileTypes == null) {
                uncachedFileTypes = new ArrayList<>();
            }
            int maxCacheSizeInBytes = 0;
            if (config != null) {
                maxCacheSizeInBytes = config.getMemCacheSize();
            }
            if (maxCacheSizeInBytes <= 0) {
                maxCacheSizeInBytes = DEFAULT_MAX_CACHE_SIZE_IN_BYTES;
            }

            synchronized (CandyWebCache.this) {
                mCacheManager = new CacheManager(mContext, protectedFilesDirPath, webappsDirPath,
                        uncachedFileTypes, maxCacheSizeInBytes);
            }
            if (WebcacheLog.DEBUG) {
                WebcacheLog.d("Load config end");
            }
            return true;
        }

        private Map<String, AppDigest> loadWebappInfos(InputStream stream) {
            if (WebcacheLog.DEBUG) {
                WebcacheLog.d("Parse config file start.");
            }
            Map<String, AppDigest> digests = new HashMap<String, AppDigest>();
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

            try {
                DocumentBuilder builder = factory.newDocumentBuilder();
                Document document = builder.parse(stream);
                Element root = document.getDocumentElement();
                NodeList webapps = root.getElementsByTagName("webapp");
                for (int i = 0; i < webapps.getLength(); ++i) {
                    Element appElem = (Element) webapps.item(i);

                    AppDigest digest = new AppDigest();

                    digest.mAppName = appElem.getAttribute("name");
                    digest.mMd5 = appElem.getAttribute("md5");
                    digest.mVersion = appElem.getAttribute("version");
                    digest.mDomains = new ArrayList<String>();

                    NodeList domainNodes = appElem.getElementsByTagName("domain");
                    for (int j = 0; j < domainNodes.getLength(); ++j) {
                        Element domainElem = (Element) domainNodes.item(j);
                        String domain = domainElem.getFirstChild().getNodeValue();
                        digest.mDomains.add(domain);
                    }
                    digests.put(digest.mAppName, digest);
                }
            } catch (ParserConfigurationException e) {
            } catch (SAXException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    stream.close();
                } catch (IOException e) {
                }
            }
            if (WebcacheLog.DEBUG) {
                WebcacheLog.d("Parse config file end.");
            }
            return digests;
        }

        private void startLoadLocalPackage() {
            try {
                InputStream webappsIs = null;
                String[] fileNames = mContext.getAssets().list("webapps");
                for (String filename : fileNames) {
                    if (filename.equals(WEBAPP_INFOS_FILENAME)) {
                        webappsIs = mContext.getAssets().open("webapps/" + WEBAPP_INFOS_FILENAME);
                        break;
                    }
                }
                if (webappsIs != null) {
                    Map<String, AppDigest> digests = loadWebappInfos(webappsIs);

                    for (String filename : fileNames) {
                        if (!filename.equals(WEBAPP_INFOS_FILENAME)) {
                            String appname = FileUtils.getPrimaryFileName(filename);
                            AppDigest appDigest = digests.get(appname);
                            if (appDigest == null) {
                                continue;
                            }
                            mCacheManager.loadLocalPackage(filename, appDigest.mVersion, appDigest.mMd5,
                                    appDigest.mDomains, mContext.getAssets().open("webapps/" + filename));
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void setCheckUrl() {
            if (mCheckPeriod > 0) {
                synchronized (CandyWebCache.this) {
                    if (mVersionCheckTimer == null) {
                        mVersionCheckTimer = new Timer();
                        TimerTask task = new TimerTask() {
                            @Override
                            public void run() {
                                startCheckAndUpdate(1);
                            }
                        };
                        mVersionCheckTimer.schedule(task, FIRST_VERSION_CHECK_DELAY_MILLIS, mCheckPeriod);
                    }
                }
            } else {
                startCheckAndUpdate(FIRST_VERSION_CHECK_DELAY_MILLIS);
            }
        }

        @Override
        protected Void doInBackground(Void... params) {
            if (WebcacheLog.DEBUG) {
                WebcacheLog.d("Load local package start");
            }
            if (!loadConfig(mConfig)) {
                return null;
            }
            startLoadLocalPackage();
            synchronized (CandyWebCache.this) {
                mInitialized = true;
                mLoadLocalPackageTask = null;
            }
            setCheckUrl();
            if (WebcacheLog.DEBUG) {
                WebcacheLog.d("Load local package end");
            }
            return null;
        }
    }

    public synchronized void init(Context context, CacheConfig config, String nativeId, String nativeVersion, String checkUrl) {
        if (mLoadLocalPackageTask != null || mCheckAndUpdateTask != null || !mWebcacheEnabled) {
            return;
        }
        if (mInitialized) {
            if (mCacheManager.checkDatabaseValidation()
                    && FileDownloadManager.getInstance().isDownloadDbValid()) {
                return;
            }
            FileDownloadManager.getInstance().closeDownloadDatabase();
            mCacheManager.closeDB();
            mCacheManager = null;
            mInitialized = false;
        }
        mContext = context.getApplicationContext();
        mNativeId = nativeId;
        mNativeVersion = nativeVersion;
        mCheckUrl = checkUrl;
        DEFAULT_PROTECTED_FILES_DIR_PATH = mContext.getFilesDir() + File.separator + "webcache";
        mLoadLocalPackageTask = new LoadLocalPackageTask(config);
        mLoadLocalPackageTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public abstract class VersionCheckListener {
        /*
         * headers: out
         * params: out
         * body: out
         */
        public void onVersionCheckStart(String url, Map<String, String> headers, Map<String, String> params,
                                        StringBuilder extraBody) {
        }

        public byte[] onVersionCheckCompleted(String url, byte[] data) {
            return data;
        }

        public abstract void onVersionCheckFailed(String url, CacheError error);
    }

    public synchronized void setVersionCheckListener(VersionCheckListener versionCheckListener) {
        mVersionCheckListener = versionCheckListener;
    }

    public abstract class ResourceUpdateListener {
        public abstract void onResourceUpdateProgress(ResourceUpdateListener listener, String appid, float percent);

        public abstract void onResourceUpdateSuccess(ResourceUpdateListener listener, String appid);

        public abstract void onResourceUpdateFailed(ResourceUpdateListener listener, String appid, CacheError error);
    }

    public synchronized void removeResourceUpdateListener(ResourceUpdateListener listener) {
        mListeners.remove(listener);
    }

    public synchronized void addResourceUpdateListener(ResourceUpdateListener listener) {
        mListeners.add(listener);
    }

    private synchronized void notifyVersionCheckFailed(CacheError error) {
        if (mVersionCheckListener != null) {
            mVersionCheckListener.onVersionCheckFailed(mCheckUrl, error);
        }
    }

    private synchronized void notifyWebappsUpdateProgress(String appid, float percent) {
        for (ResourceUpdateListener listener : mListeners) {
            listener.onResourceUpdateProgress(listener, appid, percent);
        }
    }

    private synchronized void notifyWebappsUpdateSuccess(String appid) {
        for (ResourceUpdateListener listener : mListeners) {
            listener.onResourceUpdateSuccess(listener, appid);
        }
    }

    private synchronized void notifyWebappsUpdateFailed(String appid, CacheError error) {
        for (ResourceUpdateListener listener : mListeners) {
            listener.onResourceUpdateFailed(listener, appid, error);
        }
    }

    public String constructUrl(String url, Map<String, String> params) {
        StringBuilder paramStr = new StringBuilder(url);
        if (params.size() > 0 && paramStr.toString().indexOf('?') < 0) {
            paramStr.append('?');
        }
        try {
            for (String paramName : params.keySet()) {
                if (paramStr.length() > 0 && paramStr.charAt(paramStr.length() - 1) != '?') {
                    paramStr.append("&");
                }
                paramStr.append(URLEncoder.encode(paramName, "UTF-8"));
                paramStr.append("=");
                paramStr.append(URLEncoder.encode(params.get(paramName), "UTF-8"));
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return paramStr.toString();
    }

    private class PatchTask extends AsyncTask<Void, Void, Integer> {
        private ResponseResInfo mVersionInfo;
        private CacheManager.PkgType mPkgType;
        private String mDiffFilePath;

        public PatchTask(ResponseResInfo versionInfo, CacheManager.PkgType pkgType, String diffFilePath) {
            mVersionInfo = versionInfo;
            mPkgType = pkgType;
            mDiffFilePath = diffFilePath;
        }

        @Override
        protected Integer doInBackground(Void... params) {
            WebcacheLog.d("%s", "Apply patch for " + mVersionInfo.getResID() + " start.");
            return mCacheManager.applyPatch(mVersionInfo.getResID(), mPkgType, mDiffFilePath, mVersionInfo.getDiffMd5(),
                    mVersionInfo.getFullMd5(), mVersionInfo.getFullUrl(), mVersionInfo.getResVersion(), getDomainsFromUserData(mVersionInfo.getUserData()));
        }

        @Override
        protected void onPostExecute(Integer errCode) {
            if (errCode == CacheManager.DIFF_SUCCESS) {
                notifyWebappsUpdateSuccess(mVersionInfo.getResID());
            } else {
                if (mPkgType == CacheManager.PkgType.PkgDiff) {
                    String fullUrl = mVersionInfo.getFullUrl();
                    if (fullUrl != null) {
                        WebappDownloadListener listener = new WebappDownloadListener(mVersionInfo, CacheManager.PkgType.PkgFull);
                        DownloadTask.DownloadTaskBuilder taskBuilder = new DownloadTask.DownloadTaskBuilder();
                        taskBuilder.setOnStateChangeListener(listener);
                        DownloadTask downloadTask = taskBuilder.setDownloadUrl(fullUrl).build();

                        downloadTask.start();
                    }
                } else {
                    notifyWebappsUpdateFailed(mVersionInfo.getResID(), new CacheError("Diff apply failed."));
                }
            }
        }
    }

    private class WebappDownloadListener implements StateChangeListener {
        private ResponseResInfo mVersionInfo;
        private CacheManager.PkgType mPkgType;

        public WebappDownloadListener(ResponseResInfo versionInfo, CacheManager.PkgType pkgType) {
            mVersionInfo = versionInfo;
            mPkgType = pkgType;
        }

        @Override
        public void onProgressChanged(DownloadTask downloadTask, ProgressInfo progressInfo) {
            WebcacheLog.d("%s", "Download task " + downloadTask.getDownloadTaskData().getFilename() + " for "
                    + mVersionInfo.getResID() + " progress = " + progressInfo.getPercent());
            notifyWebappsUpdateProgress(mVersionInfo.getResID(), progressInfo.getPercent());
        }

        @Override
        public void onStateChanged(DownloadTask downloadTask, DownloadTaskState state) {
            if (state == DownloadTaskState.DONE) {
                WebcacheLog.d("%s", "Download task " + downloadTask.getDownloadTaskData().getFilename() + " for "
                        + mVersionInfo.getResID() + " completed");
                String fileDirPath = downloadTask.getDownloadTaskData().getDownloadPath();
                String filePath = fileDirPath + File.separator + downloadTask.getDownloadTaskData().getFilename();
                WebcacheLog.d("%s", "File path = " + filePath);
                PatchTask task = new PatchTask(mVersionInfo, mPkgType, filePath);
                task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }
        }
    }

    private void downloadPatches(List<ResponseResInfo> versionInfos) {
        for (ResponseResInfo versionInfo : versionInfos) {
            String appId = versionInfo.getResID();
            int statusCode = versionInfo.getState();
            if (statusCode == VersionChecker.StatusCode.LATEST) { // The local is the latest
            } else if (statusCode == VersionChecker.StatusCode.NEED_UPDATE ||
                    statusCode == VersionChecker.StatusCode.RESOURCE_AUTO_FILL) { // Need update
                WebappInfo webappInfo = mCacheManager.getWebappInfo(appId);
                String ver = versionInfo.getResVersion();
                long verInt = Long.parseLong(ver);
                WebcacheLog.d("%s", "Update app " + appId + " server version " + ver + " local version "
                        + (webappInfo == null ? 0 : webappInfo.mVerStr));
                WebappDownloadListener listener = null;
                String url = null;
                if (webappInfo == null) {
                    url = versionInfo.getFullUrl();
                    WebcacheLog.d("%s", "To download full pkg " + url);
                    listener = new WebappDownloadListener(versionInfo, CacheManager.PkgType.PkgFull);
                } else {
                    if (webappInfo.inProcessStatus()) {
                        continue;
                    }
                    if (verInt != webappInfo.mVerNum || webappInfo.isInvalid()) {
                        url = versionInfo.getDiffUrl();
                        WebcacheLog.d("%s", "To download diff pkg " + url);
                        if (url != null) {
                            listener = new WebappDownloadListener(versionInfo, CacheManager.PkgType.PkgDiff);
                        } else {
                            url = versionInfo.getFullUrl();
                            WebcacheLog.d("%s", "Try to download full pkg " + url);
                            if (url != null) {
                                listener = new WebappDownloadListener(versionInfo, CacheManager.PkgType.PkgFull);
                            }
                        }
                    } else if (verInt == webappInfo.mVerNum) {
                        mCacheManager.updateWebappInfo(versionInfo.getResID(), versionInfo.getFullUrl(),
                                versionInfo.getResVersion(), getDomainsFromUserData(versionInfo.getUserData()));
                    }
                }
                if (url != null) {
                    WebcacheLog.d("%s", "Start to download pkg " + url);
                    DownloadTask.DownloadTaskBuilder taskBuilder = new DownloadTask.DownloadTaskBuilder();
                    taskBuilder.setOnStateChangeListener(listener);
                    DownloadTask downloadTask = taskBuilder.setDownloadUrl(url).build();

                    downloadTask.start();
                }
            } else if (statusCode == VersionChecker.StatusCode.RESOURCE_NOT_EXIST) { // Need to be removed.
                mCacheManager.deleteWebapp(appId);
            }
        }
    }

    private class CheckAndUpdateTask extends AsyncTask<Void, Object, Void> {
        private static final int PROGRESS_TYPE_VERSION_CHECK_COMPLETED = 1;
        private static final int PROGRESS_TYPE_VERSION_CHECK_FAILED = 2;

        private String mUrl;
        private Map<String, String> mExtraHeaders = new HashMap<>();
        private String mUserData;

        private byte[] mDecryptedData;
        private CountDownLatch mVersionCheckDataLatch;

        @Override
        protected void onPreExecute() {
            Map<String, String> extraParams = new HashMap<>();
            StringBuilder extraBody = new StringBuilder();
            synchronized (CandyWebCache.this) {
                if (mVersionCheckListener != null) {
                    mVersionCheckListener.onVersionCheckStart(mCheckUrl, mExtraHeaders, extraParams, extraBody);
                }
                mUrl = constructUrl(mCheckUrl, extraParams);
            }
            mUserData = extraBody.toString();
            WebcacheLog.d("%s", "Check version start, url = " + mUrl);
        }

        @Override
        protected void onProgressUpdate(Object... values) {
            int progressType = (Integer) values[0];
            if (progressType == PROGRESS_TYPE_VERSION_CHECK_COMPLETED) {
                synchronized (CandyWebCache.this) {
                    if (mVersionCheckListener != null) {
                        byte[] result = (byte[]) values[1];
                        mDecryptedData = mVersionCheckListener.onVersionCheckCompleted(mUrl, result);
                    }
                }
                if (mVersionCheckDataLatch != null) {
                    mVersionCheckDataLatch.countDown();
                }
            } else if (progressType == PROGRESS_TYPE_VERSION_CHECK_FAILED) {
                int resCode = (Integer) values[1];
                String errMsg = (String) values[2];
                notifyVersionCheckFailed(new CacheError(errMsg));
            }
        }

        @Override
        protected Void doInBackground(Void... params) {
            List<WebappInfo> webappInfos = mCacheManager.getAllWebappInfo();
            List<RequestResInfo> requestResInfos = new ArrayList<>();
            if (webappInfos != null) {
                for (WebappInfo appInfo : webappInfos) {
                    if (!appInfo.isInvalid()) {
                        RequestResInfo requestResInfo = new RequestResInfo();
                        requestResInfo.setResID(appInfo.mWebappName);
                        requestResInfo.setResVersion(appInfo.mVerStr);

                        requestResInfos.add(requestResInfo);
                    }
                }
            }

            AppInfo appInfo = new AppInfo();
            appInfo.setResInfos(requestResInfos);
            appInfo.setAppID(mNativeId);
            appInfo.setAppVersion(mNativeVersion);
            appInfo.setPlatform(PLATFORM);
            appInfo.setIsDiff(true);
            appInfo.setAutoFill(true);
            appInfo.setUserData(mUserData);

            VersionCheckResponseModel response = VersionChecker.checkVersion(mUrl, appInfo, mExtraHeaders, new OnResponseListener() {

                @Override
                public byte[] onResponse(byte[] result) {
                    mVersionCheckDataLatch = new CountDownLatch(1);
                    publishProgress(PROGRESS_TYPE_VERSION_CHECK_COMPLETED, result);
                    try {
                        mVersionCheckDataLatch.await(2, TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    if (mDecryptedData != null) {
                        return mDecryptedData;
                    }
                    return result;
                }
            });
            if (response != null) {
                int resCode = response.getCode();
                if (resCode != VersionChecker.ResultCode.SUCCESS) {
                    WebcacheLog.d("%s", "Check response failed, code " + resCode + " err msg " + response.getErrMsg());
                    publishProgress(PROGRESS_TYPE_VERSION_CHECK_COMPLETED, resCode, response.getErrMsg());
                    return null;
                }
                WebcacheLog.d("%s", "Check response code " + resCode + " err msg " + response.getErrMsg());
                VersionCheckResponseData resData = response.getData();
                List<ResponseResInfo> versionInfos = resData.getResInfos();
                downloadPatches(versionInfos);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            synchronized (CandyWebCache.this) {
                mCheckAndUpdateTask = null;
            }
        }
    }

    public void startCheckAndUpdate(long delayMillis) {
        if (WebcacheLog.DEBUG) {
            WebcacheLog.d("%s", "Check url = " + mCheckUrl);
        }
        synchronized (this) {
            if (checkCacheManager() && mCheckUrl != null) {
                mHandler.sendEmptyMessageDelayed(START_CHECK_AND_UPDATE, delayMillis);
            }
        }
    }

    public void updateWebapp(String appInfo) {
        if (!checkCacheManager()) {
            return;
        }
        List<ResponseResInfo> responseResInfos = VersionChecker.getVersionInfo(appInfo);
        downloadPatches(responseResInfos);
    }

    // Receive pushed message.
    public synchronized void recvServerMsg(String msg) {

    }

    private boolean checkCacheManager() {
        if (!mWebcacheEnabled) {
            WebcacheLog.d("CandyWebCache was disabled...");
            return false;
        }
        if (!mInitialized || mCacheManager == null) {
            WebcacheLog.d("CandyWebCache need init first...");
            return false;
        }
        return true;
    }

    public WebappInfo getWebappInfo(String appid) {
        if (!checkCacheManager()) {
            return null;
        }
        return mCacheManager.getWebappInfo(appid);
    }

    public List<WebappInfo> getAllWebappInfo() {
        if (!checkCacheManager()) {
            return null;
        }
        return mCacheManager.getAllWebappInfo();
    }

    public void clearAllCache() {
        if (!checkCacheManager()) {
            return;
        }
        mCacheManager.clearAllCache();
    }

    public void clearCache(String appid) {
        if (!checkCacheManager()) {
            return;
        }
        mCacheManager.clearCache(appid);
    }

    public void deleteCache(String appid) {
        if (!checkCacheManager()) {
            return;
        }
        mCacheManager.deleteWebapp(appid);
    }

    public WebResourceResponse getResponse(WebView view, String urlStr) {
        WebcacheLog.d("%s", "Get resource " + urlStr);
        if (!checkCacheManager()) {
            return null;
        }
        WebResourceResponse response = null;
        InputStream is = mCacheManager.getResource(urlStr);
        if (is != null) {
            String fileExtension = MimeTypeMap.getFileExtensionFromUrl(urlStr);
            String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension);

            response = new WebResourceResponse(mimeType, "UTF-8", is);
        }
        return response;
    }

    public WebResourceResponse getResponse(WebView view, WebResourceRequest request) {
        Uri uri = request.getUrl();
        return getResponse(view, uri.toString());
    }

    private List<String> getDomainsFromUserData(String userData) {
        List<String> domains = null;
        try {
            domains = new ArrayList<>();
            if (!TextUtils.isEmpty(userData)) {
                JSONObject jsonObject = new JSONObject(userData);
                if (jsonObject.has("domains")) {
                    JSONArray domainsJsonArray = jsonObject.getJSONArray("domains");
                    if (domainsJsonArray != null) {
                        for (int i = 0; i < domainsJsonArray.length(); i++) {
                            domains.add(domainsJsonArray.getString(i));
                        }
                    }
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return domains;
    }
}
