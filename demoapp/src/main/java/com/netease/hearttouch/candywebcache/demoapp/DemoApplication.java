package com.netease.hearttouch.candywebcache.demoapp;

import android.app.Application;

import com.netease.hearttouch.candywebcache.CacheConfig;
import com.netease.hearttouch.candywebcache.CandyWebCache;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by DING on 16/6/30.
 */
public class DemoApplication extends Application{
    @Override
    public void onCreate() {
        super.onCreate();
        CacheConfig config = buildCacheConfig();
//            String versionCheckUrl = "http://10.242.27.37:9001/api/version_check";
        String versionCheckUrl = "http://webcache-sp.kaola.com/api/version_check/webapp";
        String statisticsDataUploadUrl = "http://webcache-sp.kaola.com/api/statistics/webapp";
        CandyWebCache.getsInstance().setWebcacheEnabled(true);
        CandyWebCache.getsInstance().setDebugEnabled(true);
        CandyWebCache.getsInstance().init(this, config, null, "kaola", "1.0.1",
                versionCheckUrl, statisticsDataUploadUrl);
        CandyWebCache.getsInstance().setStatisticUploadUrl(statisticsDataUploadUrl);
        CandyWebCache.getsInstance().setUserId("hanpfei");
    }

    private CacheConfig buildCacheConfig() {
        CacheConfig.ConfigBuilder builder = CacheConfig.createCofigBuilder();
        List<String> uncachedType = new ArrayList<>();
//        uncachedType.add(".html");
        builder.setUncachedFileTypes(uncachedType);
        builder.setMemCacheSize(5 * 1025 * 1024);
        return builder.build();
    }
}
