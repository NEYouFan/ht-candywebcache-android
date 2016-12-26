package com.netease.hearttouch.candywebcache.demoapp;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.netease.hearttouch.candywebcache.CandyWebCache;
import com.netease.hearttouch.candywebcache.cachemanager.WebappInfo;

import java.util.List;

public class MainActivity extends Activity implements View.OnClickListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewById(R.id.btn_check_update).setOnClickListener(this);
        findViewById(R.id.btn_load_resource).setOnClickListener(this);
        findViewById(R.id.btn_clear).setOnClickListener(this);
        findViewById(R.id.btn_log_info).setOnClickListener(this);
        findViewById(R.id.upload_statistics).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        Intent intent = null;
        switch (v.getId()) {
            case R.id.btn_check_update:
                CandyWebCache.getsInstance().startCheckAndUpdate(1);
                Toast.makeText(MainActivity.this, "check update!", Toast.LENGTH_SHORT).show();
                break;
            case R.id.btn_load_resource:
                intent = new Intent(MainActivity.this, LoadResourceActivity.class);
                startActivity(intent);
                break;
            case R.id.btn_clear:
                deleteAllCache();
                Toast.makeText(MainActivity.this, "clear cache!", Toast.LENGTH_SHORT).show();
                break;
            case R.id.btn_log_info:
                intent = new Intent(MainActivity.this, InfomationActivity.class);
                startActivity(intent);
                break;
            case R.id.upload_statistics:
                CandyWebCache.getsInstance().updateStatisticsStub();
                break;
            default:
                break;
        }
    }

    private void deleteAllCache() {
        final List<WebappInfo> webAppInfos = CandyWebCache.getsInstance().getAllWebappInfo();
        if (webAppInfos != null && !webAppInfos.isEmpty()) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    for (WebappInfo webappInfo : webAppInfos) {
                        CandyWebCache.getsInstance().deleteCache(webappInfo.mWebappName);
                    }
                }
            }).start();
        }
    }
}
