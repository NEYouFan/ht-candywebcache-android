package com.netease.hearttouch.htfiledownloader;

/**
 * Created by DING on 16/6/12.
 */
interface InnerStateChangeListener {
    void onProgressChanged(DownloadTask downloadTask, ProgressInfo progressInfo);

    void onStateChanged(DownloadTask downloadTask, DownloadTaskState state);
}
