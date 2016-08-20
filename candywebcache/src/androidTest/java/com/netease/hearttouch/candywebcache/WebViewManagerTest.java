package com.netease.hearttouch.candywebcache;

import android.content.Context;
import android.test.InstrumentationTestCase;
import android.webkit.WebView;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Created by netease on 16/6/20.
 */
public class WebViewManagerTest extends InstrumentationTestCase {
    WebViewManager mWebViewManager;
    int mCreateCount;

    @Before
    public void setUp() throws Exception {
        mWebViewManager = WebViewManager.getsInstance();
        mCreateCount = 0;
    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void testGetsInstance() throws Exception {
        assertNotNull(mWebViewManager);
    }

    @Test
    public void testInit() throws Exception {
        mWebViewManager.init(5);
    }

    @Test
    public void testSetsWebViewFactoryAndGetWebView() throws Exception {
        WebViewManager.WebViewFactory factory = new WebViewManager.WebViewFactory() {
            @Override
            public WebView createWebView(Context context) {
//                WebView view = new WebView(context);
                ++mCreateCount;
                return null;
            }
        };
        mWebViewManager.setsWebViewFactory(factory);

        WebView view = new WebView(getInstrumentation().getContext());
        assertEquals(view.getContext(), getInstrumentation().getContext());

        assertEquals(mCreateCount, 0);

        for (int i = 0; i < 15; ++ i) {
            mWebViewManager.getWebView(getInstrumentation().getContext());
        }
        assertEquals(mCreateCount, 15);
    }

}