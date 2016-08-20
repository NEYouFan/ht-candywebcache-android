package com.netease.hearttouch.htfiledownloader;

/**
 * Created by DING on 16/6/4.
 */
public interface StateChangeListener {
    void onProgressChanged(DownloadTask downloadTask, ProgressInfo progressInfo);

    void onStateChanged(DownloadTask downloadTask, DownloadTaskState state);
}
