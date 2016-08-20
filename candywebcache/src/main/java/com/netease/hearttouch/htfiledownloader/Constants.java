package com.netease.hearttouch.htfiledownloader;

/**
 * Created by DING on 16/6/14.
 */
public interface Constants {
    int INNER_ERROR_CODE = -100;
    int INIT_DOWNLOAD_TASK_ID = -1;
    String URL_PATH_SEPERATOR = "/";
    String URL_PARAMS_SERPERATOR = "?";
    int DATA_BUFFER_SIZE = 8192;
    int READ_TIMEOUT = 8000;
    int CONNECT_TIMEOUT = 8000;
    int DOWNLOAD_SPEED_CAL_INTERVAL = 100;
}
