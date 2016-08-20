package com.netease.hearttouch.candywebcache.cachemanager;

/**
 * Created by netease on 16/6/3.
 */
public interface Patcher {
    public String applyPatch(String oldFilePath, String patchFilePath, String outDirPath);
}
