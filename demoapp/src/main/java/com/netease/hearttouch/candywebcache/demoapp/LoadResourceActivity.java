package com.netease.hearttouch.candywebcache.demoapp;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;

import com.netease.hearttouch.candywebcache.CandyWebCache;
import com.netease.hearttouch.candywebcache.HybridWebviewClient;

public class LoadResourceActivity extends Activity implements View.OnClickListener{
    private WebView mWebview;
    private EditText mUrlEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_load_resource);
        mWebview = (WebView) findViewById(R.id.webview);
        if (mWebview != null) {
            mWebview.getSettings().setJavaScriptEnabled(true);
            mWebview.setWebViewClient(new HybridWebviewClient());
        }
        mUrlEditText = (EditText) findViewById(R.id.et_url);
        findViewById(R.id.btn_load).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_load:
                mWebview.clearCache(true);
                mWebview.loadUrl(mUrlEditText.getText().toString());
                break;
            default:
                break;
        }
    }
}
