package com.netease.hearttouch.candywebcache;

import android.content.Context;
import android.net.ConnectivityManager;
import android.provider.Settings;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by hanpfei0306 on 17-2-14.
 */

public class StatisticLogger {
    private static final String TAG = "StatisticLogger";

    private static final String PLATFORM = "android";

    private static final int STATISTICS_TYPE_SUMMARY = 1;
    private static final int STATISTICS_TYPE_WIFI_SUMMARY = 2;
    private static final int STATISTICS_TYPE_MOBILE_SUMMARY = 3;

    private static final long STATISTICS_UPLOAD_PERIOD = 900000; // 15min

    private final String mNativeId;
    private final String mUserId;
    private final String mDeviceId;
    private final String mStatisticsDataUploadUrl;

    private ResourceSumStatisticRecord mMobileData;
    private ResourceSumStatisticRecord mWiFiData;

    private ResourceSumStatisticRecord mCurrentData;

    private int mCurNetworkType;

    private volatile long mLastAccessTime;
    private volatile boolean mStatisticUploadTaskStarted;

    private ScheduledExecutorService mScheduledService;

    StatisticLogger(Context context, String nativeId, String userId, String statisticsUrl) {
        mNativeId = nativeId;
        mUserId = userId;
        mDeviceId = Settings.Secure.getString(context.getContentResolver(),
                Settings.Secure.ANDROID_ID);
        mStatisticsDataUploadUrl = statisticsUrl;

        mScheduledService = new ScheduledThreadPoolExecutor(1);
        mStatisticUploadTaskStarted = false;
    }

    private void upload(String urlStr, String str) {
        URL url = null;
        try {
            url = new URL(urlStr);
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();

            urlConnection.setConnectTimeout(3000);
            urlConnection.setUseCaches(false);

            urlConnection.setInstanceFollowRedirects(true);
            urlConnection.setReadTimeout(3000);
            urlConnection.setDoInput(true);
            urlConnection.setDoOutput(true);
            urlConnection.setRequestMethod("POST");
            urlConnection.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
            urlConnection.connect();

            OutputStream out = urlConnection.getOutputStream();
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(out));
            bw.write(str);
            bw.flush();
            out.close();
            bw.close();

            WebcacheLog.d("Statistics = " + str);
            if (urlConnection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                InputStream in = urlConnection.getInputStream();
                BufferedReader br = new BufferedReader(new InputStreamReader(in));
                String resstr;
                StringBuffer buffer = new StringBuffer();
                while ((resstr = br.readLine()) != null) {
                    buffer.append(resstr);
                }
                in.close();
                br.close();
                JSONObject json = new JSONObject(buffer.toString());
                if (json.has("code")) {
                    int code = json.getInt("code");
                    if (code != 200) {
                        WebcacheLog.i(TAG, "Upload statistics fialed: " + code);
                    }
                }
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private boolean isAccessed(ResourceSumStatisticRecord dataRecord) {
        if (dataRecord.mTotalHitFileSize > 0) {
            return true;
        }
        if (dataRecord.mTotalAccessedFileNum > 0) {
            return true;
        }
        if (dataRecord.mTotalHitFileNum > 0) {
            return true;
        }
        if (dataRecord.mDownloadedResourceSize > 0) {
            return true;
        }
        return false;
    }

    private JSONObject getJSONObjectFromRecord(int type, ResourceSumStatisticRecord record) throws JSONException {
        JSONObject jsonObject = null;
        if (isAccessed(record)) {
            jsonObject = new JSONObject();
            jsonObject.put("recordType", type);
            jsonObject.put("totalFileNum", record.mTotalAccessedFileNum);
            jsonObject.put("hitFileNum", record.mTotalHitFileNum);
            jsonObject.put("totalHitFileSize", record.mTotalHitFileSize);
            jsonObject.put("resDownloadSize", record.mDownloadedResourceSize);
        }

        return jsonObject;
    }

    private void updateStatistics() {
        try {
            JSONArray jsonArray = new JSONArray();
            synchronized (this) {
                if (mWiFiData != null) {
                    JSONObject jsonObject = getJSONObjectFromRecord(STATISTICS_TYPE_WIFI_SUMMARY, mWiFiData);
                    if (jsonObject != null) {
                        jsonArray.put(jsonObject);
                    }
                    mWiFiData.reset();
                }
                if (mMobileData != null) {
                    JSONObject jsonObject = getJSONObjectFromRecord(STATISTICS_TYPE_MOBILE_SUMMARY, mMobileData);
                    if (jsonObject != null) {
                        jsonArray.put(jsonObject);
                    }
                    mMobileData.reset();
                }
            }

            if (mCurNetworkType != CandyWebCache.NETWORK_TYPE_NONE && jsonArray.length() > 0) {
                JSONObject jsonStatistics = new JSONObject();
                jsonStatistics.put("userId", mUserId);
                jsonStatistics.put("deviceId", mDeviceId);
                jsonStatistics.put("platform", PLATFORM);
                jsonStatistics.put("appId", mNativeId);
                jsonStatistics.put("data", jsonArray.toString());
                upload(mStatisticsDataUploadUrl, jsonStatistics.toString());
            }
        } catch (JSONException e) {
        }
    }

    private class StatisticsUploadTask implements Runnable {
        private final long mPeriod;

        private StatisticsUploadTask(long period) {
            mPeriod = period;
        }

        @Override
        public void run() {
            long now = System.currentTimeMillis();
            updateStatistics();

            if (now - mLastAccessTime > mPeriod) {
                synchronized (this) {
                    mStatisticUploadTaskStarted = false;
                }
            } else {
                mScheduledService.schedule(this, mPeriod, TimeUnit.MILLISECONDS);
            }
        }
    }

    private void startStatisticsUploadLocked(final long delay, final long period) {
        if (mStatisticsDataUploadUrl != null && !mStatisticUploadTaskStarted) {
            Runnable task = new StatisticsUploadTask(period);
            mScheduledService.schedule(task, delay, TimeUnit.MILLISECONDS);
            mStatisticUploadTaskStarted = true;
        }
    }

    public void logFileAccess() {
        synchronized (this) {
            if (mCurrentData != null) {
                ++mCurrentData.mTotalAccessedFileNum;
            }
            startStatisticsUploadLocked(STATISTICS_UPLOAD_PERIOD, STATISTICS_UPLOAD_PERIOD);
            mLastAccessTime = System.currentTimeMillis();
        }
    }

    public void logFileHit(long hitFileSize) {
        synchronized (this) {
            if (mCurrentData != null) {
                ++mCurrentData.mTotalHitFileNum;
                mCurrentData.mTotalHitFileSize += hitFileSize;
            }
        }
    }

    public void logResourceDownloaded(long resSize) {
        synchronized (this) {
            if (mCurrentData != null) {
                mCurrentData.mDownloadedResourceSize += resSize;
                startStatisticsUploadLocked(STATISTICS_UPLOAD_PERIOD, STATISTICS_UPLOAD_PERIOD);
            }
        }
    }

    public void connectivityChanged(int networkType) {
        synchronized (this) {
            mCurNetworkType = networkType;
            if (mCurNetworkType == ConnectivityManager.TYPE_WIFI) {
                if (mWiFiData == null) {
                    mWiFiData = new ResourceSumStatisticRecord();
                }
                mCurrentData = mWiFiData;
            } else if (mCurNetworkType == CandyWebCache.NETWORK_TYPE_NONE) {
                mCurrentData = null;
            } else {
                if (mMobileData == null) {
                    mMobileData = new ResourceSumStatisticRecord();
                }
                mCurrentData = mMobileData;
            }
        }
    }

    public void updateStatisticsStub() {
        synchronized (this) {
            startStatisticsUploadLocked(2000, 3000);
        }
    }

    private static class ResourceSumStatisticRecord {
        public int mTotalAccessedFileNum = 0;
        public int mTotalHitFileNum = 0;
        public long mTotalHitFileSize = 0;

        public long mDownloadedResourceSize = 0;

        public void reset() {
            mTotalAccessedFileNum = 0;
            mTotalHitFileNum = 0;
            mTotalHitFileSize = 0;

            mDownloadedResourceSize = 0;
        }
    }
}
