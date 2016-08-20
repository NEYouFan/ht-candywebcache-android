package com.netease.hearttouch.candywebcache.cachemanager;

/**
 * Created by netease on 16/6/6.
 */
public class BsdiffPatcher implements Patcher {
    @Override
    public String applyPatch(String oldFilePath, String patchFilePath, String outFilePath) {
        if (nativeApplyPatch(oldFilePath, patchFilePath, outFilePath)) {
            return outFilePath;
        }
        return null;
    }

    private native boolean nativeApplyPatch(String oldFilePath, String patchFilePath, String outFilePath);
}
