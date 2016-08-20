package com.netease.hearttouch.candywebcache.demoapp;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.netease.hearttouch.candywebcache.CandyWebCache;
import com.netease.hearttouch.candywebcache.cachemanager.FileInfo;
import com.netease.hearttouch.candywebcache.cachemanager.WebappInfo;

import java.util.List;

public class InfomationActivity extends Activity {
    private TextView mTvInformation;
    private boolean needAll = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_infomation);
        final Button btnSwitch = (Button) findViewById(R.id.btn_switch);
        btnSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                needAll = !needAll;
                if (needAll) {
                    btnSwitch.setText("不显示文件信息");
                    print();
                } else {
                    btnSwitch.setText("显示全部信息");
                    print();
                }
            }
        });
        mTvInformation = (TextView) findViewById(R.id.tv_information);
        print();
    }

    private void print() {
        mTvInformation.setText("");
        List<WebappInfo> webappInfos = CandyWebCache.getsInstance().getAllWebappInfo();
        if (webappInfos != null && !webappInfos.isEmpty()) {
            for (WebappInfo webappInfo : webappInfos) {
                print(webappInfo);
            }
        } else {
            mTvInformation.setText("nothing found!!!");
        }
    }

    private void print(WebappInfo webappInfo) {
        StringBuilder sb = new StringBuilder();
        sb.append("=====").append(webappInfo.mWebappName).append("=====\n")
                .append(webappInfo.mVerStr).append('\n')
                .append(webappInfo.mCachedDirPath).append('\n')
                .append(webappInfo.mCacheSize).append('\n')
                .append(webappInfo.mPkgFilePath).append('\n')
                .append(webappInfo.mPkgFileMd5).append('\n')
                .append(webappInfo.mFullUrl).append('\n')
                .append(webappInfo.mStatus).append('\n')
                .append(webappInfo.mDomains).append('\n');
        Log.d("WebAppInformation", sb.toString());
        mTvInformation.append(sb.toString());
        if (needAll) {
            if (webappInfo.mFileInfos != null && !webappInfo.mFileInfos.isEmpty()) {
                Log.d("WebAppInformation", "*****FileInfo*****\n");
                mTvInformation.append("*****FileInfo*****\n");
                for (String key : webappInfo.mFileInfos.keySet()) {
                    StringBuilder fileInfoSb = new StringBuilder();
                    fileInfoSb.append("#####").append(key).append("#####\n").append(toString(webappInfo.mFileInfos.get(key))).append('\n');
                    Log.d("WebAppInformation", fileInfoSb.toString());
                    mTvInformation.append(fileInfoSb.toString());
                }
            }
        }
    }

    private String toString(FileInfo fileInfo) {
        StringBuilder sb = new StringBuilder();
        sb.append(fileInfo.mAppname).append('\n')
                .append(fileInfo.mUrl).append('\n')
                .append(fileInfo.mLocalPath).append('\n')
                .append(fileInfo.mMd5).append('\n')
                .append(fileInfo.getStatus()).append('\n')
                .append(fileInfo.getmAccessCount()).append('\n');
        return sb.toString();
    }
}
