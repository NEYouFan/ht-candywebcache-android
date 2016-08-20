package com.netease.hearttouch.candywebcache;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.webkit.WebView;

import java.util.LinkedList;

/**
 * Created by netease on 16/6/6.
 */
public class WebViewManager {
    public interface WebViewFactory {
        WebView createWebView(Context context);
    }

    private static WebViewManager sInstance;

    private WebViewFactory mDefaultWebViewFactory;
    private WebViewFactory mWebViewFactory;

    private Context mContext;
    private LinkedList<WebView> mWebViewCache;
    private int mMaxWebViewCount;

    private WebViewManager() {
    }

    public static synchronized WebViewManager getsInstance() {
        if (sInstance == null) {
            sInstance = new WebViewManager();
        }
        return sInstance;
    }

    public synchronized void init(int maxCount) {
        mMaxWebViewCount = maxCount;
        mDefaultWebViewFactory = new DefaultWebviewFactory();
        mWebViewFactory = mDefaultWebViewFactory;
        mWebViewCache = new LinkedList<>();
    }

    public synchronized void setsWebViewFactory(WebViewFactory factory) {
        mWebViewFactory = factory;
    }

    private class WebViewCreationTask implements Runnable {
        private Context mContext;

        public WebViewCreationTask(Context context) {
            mContext = context;
        }

        @Override
        public void run() {
            synchronized (WebViewManager.this) {
                while (mWebViewCache.size() < mMaxWebViewCount) {
                    WebView view;
                    if (mWebViewFactory != null) {
                        view = mWebViewFactory.createWebView(mContext);
                    } else {
                        view = new WebView(mContext);
                    }
                    mWebViewCache.add(view);
                }
            }
        }
    }

    public synchronized WebView getWebView(Context context) {
        WebView view = null;
        if (mContext != context) {
            mWebViewCache.clear();
            mContext = context;
            Handler handler = new Handler(Looper.getMainLooper());
            WebViewCreationTask task = new WebViewCreationTask(context);
            handler.post(task);
        } else {
            if (mWebViewCache.size() > 0) {
                view = mWebViewCache.remove();
            }
        }
        if (view == null) {
            if (mWebViewFactory != null) {
                view = mWebViewFactory.createWebView(context);
            } else {
                view = new WebView(context);
            }
        }
        return view;
    }

    private static class DefaultWebviewFactory implements WebViewFactory {
        @Override
        public WebView createWebView(Context context) {
            WebView webView = new WebView(context);
            return webView;
        }
    }
}
