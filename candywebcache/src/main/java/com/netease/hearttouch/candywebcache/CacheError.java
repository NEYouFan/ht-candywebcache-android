package com.netease.hearttouch.candywebcache;

/**
 * Created by netease on 16/6/3.
 */
public class CacheError extends Exception {
    public CacheError() {

    }

    public CacheError(String detailMessage) {
        super(detailMessage);
    }

    public CacheError(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }

    public CacheError(Throwable throwable) {
        super(throwable);
    }
}
