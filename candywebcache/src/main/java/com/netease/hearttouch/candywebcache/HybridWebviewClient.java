package com.netease.hearttouch.candywebcache;

import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;

/**
 * Created by netease on 16/7/7.
 */
public class HybridWebviewClient extends WebViewClient {
    @Override
    public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
        return CandyWebCache.getsInstance().getResponse(view, request);
    }

    @Override
    public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
        return CandyWebCache.getsInstance().getResponse(view, url);
    }
}
