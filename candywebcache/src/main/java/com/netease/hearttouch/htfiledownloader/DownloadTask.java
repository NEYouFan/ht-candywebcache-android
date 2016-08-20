package com.netease.hearttouch.htfiledownloader;

import android.text.TextUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by DING on 16/6/4.
 */
public class DownloadTask {
    public static long CHUNKED_TOTAL_SIZE = -1;
    public static float CHUNKED_PERCENT = -1f;
    private static String sDefaultDownloadPath;
    private DownloadTaskData mDownloadTaskData;
    private StateChangeListener mStateChangeListener;

    public static void initDefaultDownloadPath(String downloadPath) {
        sDefaultDownloadPath = downloadPath;
    }

    public DownloadTaskData getDownloadTaskData() {
        return mDownloadTaskData;
    }

    void setDownloadTaskData(DownloadTaskData downloadTaskData) {
        mDownloadTaskData = downloadTaskData;
    }

    public StateChangeListener getStateChangeListener() {
        return mStateChangeListener;
    }

    public void setStateChangeListener(StateChangeListener stateChangeListener) {
        mStateChangeListener = stateChangeListener;
    }

    /**
     * 比较两个任务是否相等
     *
     * @param downloadTask
     * @return 相等返回true，否则返回false
     */
    public boolean equals(DownloadTask downloadTask) {
        return mDownloadTaskData.taskId == downloadTask.mDownloadTaskData.taskId;
    }

    /**
     * 开始任务，根据情况可能是加入队列等待
     * 或者是已经有相同ID任务在执行，则直接会获得状态的更新回调
     */
    public void start() {
        FileDownloadManager.getInstance().start(this);
    }

    /**
     * 任务的暂停
     */
    public void pause() {
        FileDownloadManager.getInstance().pause(this);
    }

    /**
     * 任务的继续
     *
     * @see #start()
     */
    public void resume() {
        start();
    }

    /**
     * 任务的取消，将会删除数据库记录和文件记录
     */
    public void cancel() {
        FileDownloadManager.getInstance().cancel(this);
    }

    static public class DownloadTaskData {
        private int taskId = Constants.INIT_DOWNLOAD_TASK_ID;
        private String url;
        private Map<String, String> headers;
        private String body;
        // TODO: 16/6/27 参数现在没用起来呀
        private Map<String, String> params;
        private String downloadPath;
        private String filename;
        private ProgressInfo progressInfo;
        private DownloadTaskState state;
        private int errCode;
        private String errMsg;
        private int priority;

        public DownloadTaskData() {
            progressInfo = new ProgressInfo();
            state = DownloadTaskState.READY;
        }

        public DownloadTaskData(DownloadTaskData downloadTaskData) {
            taskId = downloadTaskData.taskId;
            url = downloadTaskData.url;
            headers = new HashMap<>(downloadTaskData.headers);
            body = downloadTaskData.body;
            params = new HashMap<>(downloadTaskData.params);
            downloadPath = downloadTaskData.downloadPath;
            progressInfo = new ProgressInfo(downloadTaskData.progressInfo);
            state = downloadTaskData.state;
            errCode = downloadTaskData.errCode;
            errMsg = downloadTaskData.errMsg;
            priority = downloadTaskData.priority;
        }

        //只拷贝进度和状态
        void copyFrom(DownloadTaskData downloadTaskData) {
            downloadPath = downloadTaskData.downloadPath;
            filename = downloadTaskData.filename;
            progressInfo.copyFrom(downloadTaskData.progressInfo);
            state = downloadTaskData.state;
        }

        public int getTaskId() {
            return taskId;
        }

        void setTaskId(int taskId) {
            this.taskId = taskId;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public Map<String, String> getHeaders() {
            return headers;
        }

        public void setHeaders(Map<String, String> headers) {
            this.headers = new HashMap<>(headers);
        }

        public Map<String, String> getParams() {
            return params;
        }

        public void setParams(Map<String, String> params) {
            this.params = new HashMap<>(params);
        }

        public String getDownloadPath() {
            return downloadPath;
        }

        public void setDownloadPath(String downloadPath) {
            this.downloadPath = downloadPath;
        }

        public String getFilename() {
            return filename;
        }

        void setFilename(String filename) {
            this.filename = filename;
        }

        public ProgressInfo getProgressInfo() {
            return progressInfo;
        }

        void setProgressInfo(ProgressInfo progressInfo) {
            this.progressInfo = new ProgressInfo(progressInfo);
        }

        public DownloadTaskState getState() {
            return state;
        }

        void setState(DownloadTaskState state) {
            this.state = state;
        }

        public int getErrCode() {
            return errCode;
        }

        public void setErrCode(int errCode) {
            this.errCode = errCode;
        }

        public String getErrMsg() {
            return errMsg;
        }

        public void setErrMsg(String errMsg) {
            this.errMsg = errMsg;
        }

        public int getPriority() {
            return priority;
        }

        public String getBody() {
            return body;
        }

        public void setBody(String body) {
            this.body = body;
        }

        public void setPriority(int priority) {
            this.priority = priority;
        }
    }

    static public class DownloadTaskBuilder {
        private DownloadTaskData mDownloadTaskData = new DownloadTaskData();
        private StateChangeListener mStateChangeListener;

        public DownloadTaskBuilder() {
        }

        //目的是不持有外面的引用，外面修改设置进来的downloadTaskData不会影响内部的数据
        public DownloadTaskBuilder setDownLoadTaskData(DownloadTaskData downLoadTaskData) {
            mDownloadTaskData = new DownloadTaskData(downLoadTaskData);
            return this;
        }

        public DownloadTaskBuilder setDownloadUrl(String url) {
            mDownloadTaskData.setUrl(url);
            return this;
        }

        public DownloadTaskBuilder setOnStateChangeListener(StateChangeListener stateChangeListener) {
            mStateChangeListener = stateChangeListener;
            return this;
        }

        public DownloadTaskBuilder setHeaders(Map<String, String> headers) {
            mDownloadTaskData.setHeaders(headers);
            return this;
        }

        public DownloadTaskBuilder setBody(String body) {
            mDownloadTaskData.setBody(body);
            return this;
        }

        public DownloadTaskBuilder setParams(Map<String, String> params) {
            mDownloadTaskData.setParams(params);
            return this;
        }

        public DownloadTaskBuilder setDownloadPath(String downloadPath) {
            mDownloadTaskData.setDownloadPath(downloadPath);
            return this;
        }

        public DownloadTaskBuilder setPriority(int priority) {
            mDownloadTaskData.setPriority(priority);
            return this;
        }

        /**
         * 根据前面设定的参数创建{@link DownloadTask}实例
         *
         * @return DownloadTask实例
         */
        public DownloadTask build() {
            if (!checkValid()) {
                throw new IllegalArgumentException("must set download url...");
            }

            DownloadTask downloadTask = new DownloadTask();
            if (mDownloadTaskData.downloadPath == null) {
                mDownloadTaskData.downloadPath = DownloadTask.sDefaultDownloadPath;
            }
            if (mDownloadTaskData.getTaskId() == Constants.INIT_DOWNLOAD_TASK_ID) {
                mDownloadTaskData.setTaskId(FileDownloaderUtil.generateTaskId(mDownloadTaskData.url, mDownloadTaskData.downloadPath));
            }
            downloadTask.setStateChangeListener(mStateChangeListener);
            downloadTask.setDownloadTaskData(mDownloadTaskData);

            clear();
            return downloadTask;
        }

        private boolean checkValid() {
            if (mDownloadTaskData == null || TextUtils.isEmpty(mDownloadTaskData.getUrl())) {
                return false;
            }
            return true;
        }

        private void clear() {
            mDownloadTaskData = new DownloadTaskData();
            mStateChangeListener = null;
        }
    }
}
