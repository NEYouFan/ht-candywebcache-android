package com.netease.hearttouch.htfiledownloader;

/**
 * Created by DING on 16/6/4.
 */
public enum DownloadTaskState {
    READY, DOWNLOADING, PAUSING, CANCELLED, DONE, FAILED;

    public static DownloadTaskState valueOf(int ordinal) {
        if (ordinal < 0 || ordinal >= values().length) {
            throw new IndexOutOfBoundsException("Invalid ordinal");
        }
        return values()[ordinal];
    }
}