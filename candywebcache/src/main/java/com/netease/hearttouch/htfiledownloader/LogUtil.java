package com.netease.hearttouch.htfiledownloader;

import android.util.Log;

/**
 * Created by DING on 16/6/29.
 */

public class LogUtil {
    private static final String TAG = LogUtil.class.getSimpleName();
    public static final int VERBOSE = Log.VERBOSE;
    public static final int DEBUG = Log.DEBUG;
    public static final int INFO = Log.INFO;
    public static final int WARN = Log.WARN;
    public static final int ERROR = Log.ERROR;
    public static final int ASSERT = Log.ASSERT;

    //默认level Error
    private static int level = DEBUG;
    private static boolean enable = true;

    public static void setLevel(int level) {
        LogUtil.level = level;
    }

    public static void enableLog(boolean enable) {
        LogUtil.enable = enable;
    }

    /**
     * debug level
     *
     * @param tag  调试信息标签
     * @param info 信息
     * @return void
     */
    public static void d(String tag, String info) {
        if (enable && DEBUG >= level) {
            Log.d(tag, info);
        }
    }

    /**
     * information level
     *
     * @param tag  提示信息标签
     * @param info 信息
     * @return void
     */
    public static void i(String tag, String info) {
        if (enable && INFO >= level) {
            Log.i(tag, info);
        }
    }

    /**
     * error level
     *
     * @param tag  错误信息标签
     * @param info 信息
     * @return void
     */
    public static void e(String tag, String info) {
        if (enable && ERROR >= level) {
            Log.e(tag, info);
        }
    }

    /**
     * warning level
     *
     * @param tag  警告信息标签
     * @param info 信息
     * @return void
     */
    public static void w(String tag, String info) {
        if (enable && WARN >= level) {
            Log.w(tag, info);
        }
    }
}