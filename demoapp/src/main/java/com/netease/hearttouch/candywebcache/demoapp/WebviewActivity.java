package com.netease.hearttouch.candywebcache.demoapp;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.Toast;

import com.netease.hearttouch.candywebcache.CandyWebCache;
import com.netease.hearttouch.candywebcache.WebViewManager;
import com.netease.hearttouch.candywebcache.cachemanager.FileUtils;
import com.netease.hearttouch.candywebcache.cachemanager.WebappInfo;
import com.netease.hearttouch.htfiledownloader.DownloadTask;
import com.netease.hearttouch.htfiledownloader.DownloadTaskState;
import com.netease.hearttouch.htfiledownloader.ProgressInfo;
import com.netease.hearttouch.htfiledownloader.StateChangeListener;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

public class WebviewActivity extends Activity implements StateChangeListener {
    private long start;

    private WebView mWebView;
    private Button mBtnLoadPage;
    private Button mBtnLoadKaolaTestPage;
    private Button mBtnCheckAndUpdate;
    private Button mBtnDownload;
    private Button mBtnDeleteAll;
    private Button mBtnLoadLoginPage;

    private Toast mToast;

    private boolean needCache = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_webview);

        mWebView = (WebView) findViewById(R.id.webview);
        if (mWebView != null) {
            mWebView.getSettings().setJavaScriptEnabled(true);
            mWebView.setWebViewClient(new HybridWebviewClient());
            mWebView.addJavascriptInterface(this, "RecordTime");
        }
        WebViewManager.getsInstance().init(6);
        WebViewManager.getsInstance().getWebView(this);

        ClickListener listener = new ClickListener();

        mBtnLoadPage = (Button) findViewById(R.id.load_page_btn);
        mBtnLoadPage.setOnClickListener(listener);

        mBtnLoadLoginPage = (Button) findViewById(R.id.load_login_btn);
        mBtnLoadLoginPage.setOnClickListener(listener);

        mBtnLoadKaolaTestPage = (Button) findViewById(R.id.load_page_btn_k);
        mBtnLoadKaolaTestPage.setOnClickListener(listener);

        mBtnCheckAndUpdate = (Button) findViewById(R.id.check_and_update_btn);
        mBtnCheckAndUpdate.setOnClickListener(listener);

        mBtnDownload = (Button) findViewById(R.id.download_btn);
        mBtnDownload.setOnClickListener(listener);

        mBtnDeleteAll = (Button) findViewById(R.id.delete_all_btn);
        mBtnDeleteAll.setOnClickListener(listener);
    }

    private class ClickListener implements View.OnClickListener {
        @Override
        public void onClick(View view) {
            if (view == mBtnLoadPage) {
                mWebView.clearCache(true);
                mWebView.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
                mWebView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
                mWebView.loadUrl("http://www.m.163.com/public/webapp/pub/app.html");
            } else if (view == mBtnLoadKaolaTestPage) {
                mWebView.clearCache(true);
                mWebView.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
                mWebView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
                mWebView.loadUrl("http://m.kaola.com");
            } else if (view == mBtnCheckAndUpdate) {
                CandyWebCache.getsInstance().startCheckAndUpdate(1);
            } else if (view == mBtnDownload) {
                DownloadTask downloadTask = new DownloadTask.DownloadTaskBuilder()
                        .setDownloadUrl("https://commondatastorage.googleapis.com/androiddevelopers/shareables/icon_templates-v4.0.zip")
                        .setOnStateChangeListener(WebviewActivity.this)
                        .build();
                downloadTask.start();
            } else if (view == mBtnDeleteAll) {
                DeleteWebcacheTask task = new DeleteWebcacheTask("/sdcard/netease/webcache");
                task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            } else if (view == mBtnLoadLoginPage) {
                mWebView.clearCache(true);
                mWebView.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
                mWebView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
                mWebView.loadUrl("http://www.kaola.com/webapp/res/image/sprite.png");
            }
        }
    }

    private void postToast(String msg) {
        if (mToast == null) {
            mToast = Toast.makeText(this, msg, Toast.LENGTH_SHORT);
        }
        mToast.setText(msg);
        mToast.setDuration(Toast.LENGTH_SHORT);
        mToast.show();
    }

    private class DeleteWebcacheTask extends AsyncTask<Void, Void, Boolean> {
        private String mWebcacheRootPath;

        public DeleteWebcacheTask(String rootPath) {
            mWebcacheRootPath = rootPath;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            CandyWebCache webCache = CandyWebCache.getsInstance();
            List<WebappInfo> allWebappInfos = webCache.getAllWebappInfo();
            for (WebappInfo webappInfo : allWebappInfos) {
                webCache.deleteCache(webappInfo.mWebappName);
            }
            FileUtils.deleteDir(mWebcacheRootPath);

//            CacheConfig config = buildCacheConfig();
//            String versionCheckUrl = "http://10.165.124.46:8080/api/version_check";
//            CandyWebCache.getsInstance().init(WebviewActivity.this, config, "KaoLa", "1.0.1", versionCheckUrl);
            return true;
        }

        @Override
        protected void onPostExecute(Boolean deleteResult) {
            postToast("Delete dir " + mWebcacheRootPath + " " + (deleteResult ? "successfully!" : "failed!"));
        }
    }

    @Override
    public void onStateChanged(DownloadTask downloadTask, DownloadTaskState state) {
        Log.d("State", String.format("taskid:%d,state:%s",
                downloadTask.getDownloadTaskData().getTaskId(),
                state.toString()
        ));
    }

    @Override
    public void onProgressChanged(DownloadTask downloadTask, ProgressInfo progressInfo) {
        Log.d("Progress", String.format("taskid:%d,percent:%f,transferred:%d,total:%d,speed:%f",
                downloadTask.getDownloadTaskData().getTaskId(),
                progressInfo.getPercent(),
                progressInfo.getTransferredSize(),
                progressInfo.getTotalSize(),
                progressInfo.getTransferSpeed()));
    }

    private class HybridWebviewClient extends WebViewClient {
        @SuppressLint("NewApi")
        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
            WebResourceResponse response = CandyWebCache.getsInstance().getResponse(view, request);
            if (response == null) {
                Log.d("Load Resource","Remote:" + request.getUrl());
                response = super.shouldInterceptRequest(view, request);
            } else{
                Log.d("Load Resource","Local:" + request.getUrl());
            }
            return response;
        }

        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
            WebResourceResponse response = CandyWebCache.getsInstance().getResponse(view, url);
            if (response == null) {
                Log.d("Load Resource","Remote:" + url);
                response = super.shouldInterceptRequest(view, url);
            } else{
                Log.d("Load Resource","Local:" + url);
            }
            return response;
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
//            webviewStart = System.currentTimeMillis();
//            Log.d("test start", String.valueOf(System.currentTimeMillis() - webviewStart));
//            webviewStart = System.currentTimeMillis();
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);

            //间隔一下，不要每次都加载同一个页面
            if (!url.equals("about:blank")) {
                long pageFinishTime = System.currentTimeMillis();
                mWebView.loadUrl("javascript: function recordTime(pageFinishTime){" +
                        "window.RecordTime.setTime(window.performance.timing.navigationStart," +
                        "window.performance.timing.domLoading," +
                        "window.performance.timing.domInteractive," +
                        "window.performance.timing.domContentLoadedEventStart," +
                        "window.performance.timing.loadEventStart," +
                        "pageFinishTime);}");
                mWebView.loadUrl("javascript:recordTime(" + pageFinishTime + ")");
//                startLoad();
            } else {
                mWebView.loadUrl("about:blank");
            }
        }
    }

    @JavascriptInterface
    public void setTime(String type, String time) {
        long timeMS = Long.parseLong(time) - start;
        switch (type) {
            case "DomContentLoad":
                break;
            case "window.onload":
                break;
        }
    }

    @JavascriptInterface
    public void setTime(long startTime, long domLoadingTime,
                        long domInteractiveTime, long domContentLoadedEventTime,
                        long windowLoadTime, long pageFinishTime) {
        logToFile(String.valueOf(domLoadingTime - startTime) + " " +
                String.valueOf(domContentLoadedEventTime - startTime) + " " + String.valueOf(windowLoadTime - startTime) + " " +
                String.valueOf(pageFinishTime - startTime) + "\n");
    }

    private void startLoad() {
        if (mWebView != null) {
            if (!needCache) {
                mWebView.clearCache(true);
            }
            start = System.currentTimeMillis();
            mWebView.loadUrl("http://localhost/webapp/pub.nej/app.html");
        }
    }

    private void logToFile(String message) {
        File file = new File(Environment.getExternalStorageDirectory(), "webview.log");
        FileOutputStream fileOutputStream = null;
        try {
            if (!file.exists()) {
                file.createNewFile();
            }
            fileOutputStream = new FileOutputStream(file, true);
            fileOutputStream.write(message.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fileOutputStream != null) {
                try {
                    fileOutputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
