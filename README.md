# CandyWebCache

CandyWebCache是移动端web资源的本地缓存解决方案，能够拦截webview的请求，并优先使用本地缓存静态资源进行响应，以此来对webview加载页面性能进行优化。

特点：

* 协议层拦截请求，透明替换响应
* 静态资源版本控制及更新策略
* 资源防篡改策略
* 静态资源自动打包到应用，及首次安装解压处理

## CandyWebCache的使用

(1) CandyWebCache的配置及初始化

CandyWebCache在访问之前，首先需要进行配置及初始化。配置及初始化的动作通常建议放在包含了WebView，且该WebView访问的WebApp静态资源需由CandyWebCache管理的Activity的onCreate()方法中执行。具体的配置及初始化方法如下面代码所示：

```
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ......
        CandyWebCache.getsInstance().setDebugEnable(true);
        CacheConfig config = buildCacheConfig();
        String versionCheckUrl = "http://10.165.124.46:8080/api/version_check";
        CandyWebCache.getsInstance().init(this, config, "kaola", "3.0.1", versionCheckUrl);
    }

    private CacheConfig buildCacheConfig() {
        CacheConfig.ConfigBuilder builder = CacheConfig.createCofigBuilder();
        List<String> uncachedType = new ArrayList<>();
        uncachedType.add(".html");
        builder.setUncachedFileTypes(uncachedType);
        return builder.build();
    }
```

首先需要构造`CacheConfig`对象，CandyWebCache使用该类的对象来表述配置项。具体可以进行配置的项目如下：

 * 自动进行版本检查更新的周期（以毫秒为单位）
 * 本地缓存的WebApp的保存路径
 * CandyWebCache保护文件的保存路径
 * 内存缓存的最大大小（以字节为单位）
 * 不进行缓存的文件的类型的列表（文件的类型使用后缀名来描述）

其次，调用`CandyWebCache.getsInstance()`获取CandyWebCache对象。

最后，调用`CandyWebCache`对象的`init()`方法，传入`CacheConfig`对象，本地App的AppID，本地App的版本号，及进行版本检测的URL，对CandyWebCache进行初始化。

初始化完成之后，即可通过`CandyWebCache.getsInstance()`获取CandyWebCache对象并对CandyWebCache进行访问。

(2) 启动webapp的版本检测更新

本地App可以在适当的时候，如应用启动、Activity创建，前后台切换等主动触发WebApp的版本检查更新流程。具体的方法如下：

```
        CandyWebCache webcache = CandyWebCache.getsInstance();
        long delayMillis = 5;
        webcache.startCheckAndUpdate(delayMillis);
```

通过`CandyWebCache.getsInstance()`获取CandyWebCache对象，传入触发WebApp版本检查更新启动延迟的毫秒数，调用`startCheckAndUpdate()`方法即可。

(3) 访问资源

CandyWebCache初始化结束之后，即可对缓存的本地静态资源进行访问，及对本地缓存进行控制了。可通过如下的方法来获取本地缓存的静态资源：

```
        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
            return CandyWebCache.getsInstance().getResponse(view, request);
        }

        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
            return CandyWebCache.getsInstance().getResponse(view, url);
        }
```

通过`CandyWebCache.getsInstance()`获取CandyWebCache对象，传入请求资源的WebView对象及URL或request，调用`getResponse()`方法，获得资源对应的response。

(4) 版本检查更新监听回调及资源更新进度监听回调。

可以为版本检查及资源下载更新进度设置或添加回调，以便于对版本检查请求在请求发送前，或版本检查的服务端响应在被接收到后做进一步的处理，或实时地获取资源的下载更新进度。

通过CandyWebCache的如下方法来设置、添加或移除这些回调：

```
void setVersionCheckListener(CandyWebCache.VersionCheckListener versionCheckListener)

void addResourceUpdateListener(CandyWebCache.ResourceUpdateListener listener)

void removeResourceUpdateListener(CandyWebCache.ResourceUpdateListener listener)
```

(5) 完整示例代码

```
package com.netease.hearttouch.candywebcache.demoapp;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;

import com.netease.hearttouch.candywebcache.CacheConfig;
import com.netease.hearttouch.candywebcache.CandyWebCache;

import java.util.ArrayList;
import java.util.List;

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
            mWebview.setWebViewClient(new WebViewClient() {
                @Override
                public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                    return CandyWebCache.getsInstance().getResponse(view, request);
                }

                @Override
                public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
                    return CandyWebCache.getsInstance().getResponse(view, url);
                }
            });
        }
        mUrlEditText = (EditText) findViewById(R.id.et_url);
        findViewById(R.id.btn_load).setOnClickListener(this);

        CacheConfig config = buildCacheConfig();
        String versionCheckUrl = "http://10.165.124.46:8080/api/version_check";
        CandyWebCache.getsInstance().init(this, config, "KaoLa", "1.0.1", versionCheckUrl);
    }

    private CacheConfig buildCacheConfig() {
        CacheConfig.ConfigBuilder builder = CacheConfig.createCofigBuilder();
        List<String> uncachedType = new ArrayList<>();
        uncachedType.add(".html");
        builder.setUncachedFileTypes(uncachedType);
        builder.setCacheDirPath("/sdcard/netease/webcache");
        builder.setManifestDirPath("/sdcard/netease/webcache/manifests");
        builder.setMemCacheSize(5 * 1025 * 1024);
        return builder.build();
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
```

## 调试

CandyWebCache提供了调试开关，以方便开发者在开发期间获取更多CandyWebCache的运行信息。用户可通过`CandyWebCache`对象的`setDebugEnable()`方法来关闭调试开关，如：

```
CandyWebCache.getsInstance().setDebugEnable(true);
```

## 系统要求

该项目支持最低Android API Level 14。

## CandyWebCache客户端SDK对服务器的要求

提供给客户端SDK的接口：

* 版本检测接口，返回信息包括
	* 请求的webapp对应的增量包和全量包信息：版本号、下载地址、md5、url、domains
	* 请求中不包含的webapp则返回全量包信息：版本号、下载地址、md5、url、domains

提供给应用服务器的接口：

* 更新全量包
	* 根据全量包和历史N(N可配置)个版本的包进行diff包计算
	* 计算各个资源包的md5，并加密md5值
	* 上传增量包和全量包到文件服务，并记录各个包的md5、资源url、版本号信息、domains

服务端功能要求：

* 计算资源包diff包（使用bsdiff）
* 上传资源到文件服务器
* 资源md5计算与加密（加密算法:DES + base64，客户端对称加密秘钥目前是埋在客户端代码中）
* webapp domains的配置

## CandyWebCache客户端SDK对打包方式的要求

* 打包资源包目录路径要跟url能够对应，如 `http://m.kaola.com/public/r/js/core_57384232.js` ，资源的存放路径需要是 `public/r/js/core_57384232.js` 或者 `r/js/core_57384232.js`。
* 资源缓存不支持带“?”的url，如果有版本号信息需要打到文件名中。对于为了解决缓存问题所采用的后缀形式url，如 `http://m.kaola.com/public/r/js/core.js?v=57384232` ,需要调整打包方式，采用文件名来区分版本号。


