package com.netease.hearttouch.htfiledownloader;

import android.text.TextUtils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;

/**
 * Created by DING on 16/6/8.
 */
public class FileDownloadRunnable implements Runnable {
    private static final String TAG = FileDownloadManager.class.getSimpleName();
    private DownloadTask mDownloadTask;
    private InnerStateChangeListener mInnerStateChangeListener;
    private volatile boolean mNeedPause = false;
    private volatile boolean mNeedCancel = false;
    private boolean mUseExistFile = false;
    private DownloadDBHelper mDownloadDBHelper = null;
    private DownloadTask.DownloadTaskData mDownloadTaskData;
    private String mFilename;
    private boolean mSupportRanges = false;
    private BufferedInputStream mBis = null;
    private RandomAccessFile mRaFile = null;

    public void pause() {
        this.mNeedPause = true;
    }

    public void cancel() {
        this.mNeedCancel = true;
    }

    public FileDownloadRunnable(DownloadTask downloadTask, InnerStateChangeListener innerStateChangeListener) {
        mDownloadTask = downloadTask;
        mInnerStateChangeListener = innerStateChangeListener;
    }

    //TODO 获取文件名策略，先看有没有302重定向，然后从url获取，然后从Content-Disposition获取
    @Override
    public void run() {
        mDownloadDBHelper = DownloadDBHelper.getInstance();
        mDownloadTaskData = mDownloadDBHelper.getByTaskId(mDownloadTask.getDownloadTaskData().getTaskId());
        //检查数据库，存在则从数据库恢复，不存在则写入数据库
        if(checkDbRecordAndFile()){
            return;
        }

        String downloadUrl = mDownloadTask.getDownloadTaskData().getUrl();
        mFilename = getDownloadFileNameByUrl(downloadUrl);
        //取得的文件名为空，构造一个taskid+时间的文件名，做个保护
        if (TextUtils.isEmpty(mFilename)) {
            mFilename = String.valueOf(mDownloadTask.getDownloadTaskData().getTaskId()) + System.currentTimeMillis();
        }
        mDownloadTask.getDownloadTaskData().setFilename(mFilename);
        mDownloadDBHelper.insertOrUpdate(mDownloadTask.getDownloadTaskData());
        LogUtil.d(TAG, "file name:" + mFilename);
        try {
            HttpURLConnection httpConnection = initConnection(downloadUrl);
            final int responseCode = httpConnection.getResponseCode();
            LogUtil.d(TAG, "response code: " + responseCode);
            //返回成功200或者因为Range会返回206
            if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_PARTIAL) {
                Map<String, List<String>> headers = httpConnection.getHeaderFields();
                //检查是否支持Ranges
                mSupportRanges = supportRanges(headers);
                LogUtil.d(TAG, "support ranges:" + mSupportRanges);

                //下载前检查文件是否可用，设置可用标记
                checkFileAvailabe();
                //设置文件长度
                setContentLength(headers);
                //根据标记和是否支持range来新建或复用文件
                File createFile = createFile();
                if (!processData(httpConnection, createFile)) {
                    return;
                }
            } else {
                error(responseCode, httpConnection.getResponseMessage());
            }
        } catch (IOException e) {
            //各种被打断的错误
            error(Constants.INNER_ERROR_CODE, e.getMessage());
        } finally {
            if (mBis != null) {
                try {
                    mBis.close();
                } catch (IOException e) {
                    error(Constants.INNER_ERROR_CODE, e.getMessage());
                }
            }
            if (mRaFile != null) {
                try {
                    mRaFile.close();
                } catch (IOException e) {
                    error(Constants.INNER_ERROR_CODE, e.getMessage());
                }
            }
        }
    }

    //TODO 文件名没有后缀的情况怎么办，另外需要对文件名的长度做个限制，看下系统支持
    private String getDownloadFileNameByUrl(String url) {
        int positionSeperator = url.lastIndexOf(Constants.URL_PATH_SEPERATOR);
        int positionParams = url.indexOf(Constants.URL_PARAMS_SERPERATOR);
        if (positionParams != -1 && positionParams <= positionSeperator) {
            return null;
        }
        return url.substring(positionSeperator + 1, positionParams == -1 ? url.length() : positionParams);
    }

    private void error(int errorCode, String errorMsg) {
        mDownloadTask.getDownloadTaskData().setErrCode(errorCode);
        mDownloadTask.getDownloadTaskData().setErrMsg(errorMsg);
        mDownloadTask.getDownloadTaskData().setState(DownloadTaskState.FAILED);
        if (mInnerStateChangeListener != null) {
            mInnerStateChangeListener.onStateChanged(mDownloadTask, DownloadTaskState.FAILED);
        }
        DownloadDBHelper.getInstance().insertOrUpdate(mDownloadTask.getDownloadTaskData());
        LogUtil.w(TAG, "task failed, code:" + errorCode + ",errorMsg:" + errorMsg);
    }

    private boolean checkDbRecordAndFile() {
        if (mDownloadTaskData == null) {
            LogUtil.d(TAG, "new downloading task...");
            //任务不存在，则当前任务写入数据库
            mDownloadDBHelper.insertOrUpdate(mDownloadTask.getDownloadTaskData());
        } else {
            //任务存在，但是如果文件名未设置，则无法进行文件的存在性和可用性校验，直接走到下载流程中
            //可以理解为还没有进行过下载
            if (!TextUtils.isEmpty(mDownloadTaskData.getFilename())) {
                LogUtil.d(TAG, "a record task...");
                File tmpFile = new File(mDownloadTaskData.getDownloadPath(), mDownloadTaskData.getFilename());
                long transferredSize = mDownloadTaskData.getProgressInfo().getTransferredSize();
                if (tmpFile.exists()) {
                    //文件存在，任务是完成状态，文件大小对（目前只能校验文件大小）
                    if (mDownloadTaskData.getState() == DownloadTaskState.DONE && tmpFile.length() == transferredSize) {
                        LogUtil.d(TAG, "a record DONE task...");
                        //鉴定为可用，通知上层
                        mDownloadTask.getDownloadTaskData().copyFrom(mDownloadTaskData);
                        if (mInnerStateChangeListener != null) {
                            mInnerStateChangeListener.onProgressChanged(mDownloadTask, mDownloadTask.getDownloadTaskData().getProgressInfo());
                            mInnerStateChangeListener.onStateChanged(mDownloadTask, mDownloadTask.getDownloadTaskData().getState());
                        }
                        //直接完成了，工作线程退出
                        return true;
                    } else if (tmpFile.length() < transferredSize) {
                        LogUtil.d(TAG, "a record task with error progress, need to be re-downloaded...");
                        //文件大小不对，重新下载，删除已有文件，新任务插入到数据库
                        tmpFile.delete();
                        //清理一下下载进度，其他不变
                        mDownloadTask.getDownloadTaskData().getProgressInfo().reset();
                        mDownloadDBHelper.insertOrUpdate(mDownloadTask.getDownloadTaskData());
                    } else if (tmpFile.length() == transferredSize
                            && transferredSize == mDownloadTaskData.getProgressInfo().getTotalSize()) {
                        //如果文件大小和已下载大小以及总大小都相等，则当做已经完成
                        mDownloadTask.getDownloadTaskData().copyFrom(mDownloadTaskData);
                        mDownloadTask.getDownloadTaskData().setState(DownloadTaskState.DONE);
                        if (mInnerStateChangeListener != null) {
                            mInnerStateChangeListener.onProgressChanged(mDownloadTask, mDownloadTask.getDownloadTaskData().getProgressInfo());
                            mInnerStateChangeListener.onStateChanged(mDownloadTask, mDownloadTask.getDownloadTaskData().getState());
                        }
                        mDownloadDBHelper.insertOrUpdate(mDownloadTask.getDownloadTaskData());
                        //直接完成了，工作线程退出
                        return true;
                    } else {
                        LogUtil.d(TAG, "a record task and can continue...");
                        // 文件存在，大小正常，可以继续使用
                        // tmpFile.length() > transferredSize的情况，可以从transferredSize的情况的地方继续写，覆盖后面的
                        mUseExistFile = true;
                        //其他可用情况，继续下载，这个状态和进度就不通知了，等下载开始了再通知
                        mDownloadTask.getDownloadTaskData().copyFrom(mDownloadTaskData);
                    }
                } else {
                    LogUtil.d(TAG, "a record task but maybe file is deleted, need re-download...");
                    //如果文件不存在，直接当做新的任务开始做了
                    mDownloadTask.getDownloadTaskData().getProgressInfo().reset();
                    mDownloadDBHelper.insertOrUpdate(mDownloadTask.getDownloadTaskData());
                }
            }
        }
        return false;
    }

    private boolean supportRanges(Map<String, List<String>> headers) {
        if (headers.containsKey("Accept-Ranges") || headers.containsKey("Content-Range")) {
            List<String> acRangeList = headers.get("Accept-Ranges");
            List<String> contentRangeList = headers.get("Content-Range");
            if (acRangeList != null && !acRangeList.isEmpty() && acRangeList.get(0).equals("bytes")
                    || contentRangeList != null && !contentRangeList.isEmpty() && contentRangeList.get(0).contains("bytes")) {
                //支持Range
                return true;
            }
        }
        return false;
    }

    private void checkFileAvailabe() {
        //表示已经进行过下载，文件已经创建，那么需要删掉这些文件，重新来过
        File file = new File(mDownloadTask.getDownloadTaskData().getDownloadPath(), mFilename);
        if (!mSupportRanges) {
            //不支持Range的情况，需要删除文件，清除进度
            file.delete();
            //清理进度
            ProgressInfo progressInfo = mDownloadTask.getDownloadTaskData().getProgressInfo();
            progressInfo.setTotalSize(0);
            progressInfo.setTransferSpeed(0);
            progressInfo.setTransferredSize(0);
            progressInfo.setPercent(0f);
            //需要把新的进度信息写入数据库
            if (mInnerStateChangeListener != null) {
                mInnerStateChangeListener.onProgressChanged(mDownloadTask, progressInfo);
            }
            mDownloadDBHelper.insertOrUpdate(mDownloadTask.getDownloadTaskData());
        } else if (mSupportRanges && file.exists() && mDownloadTaskData != null) {
            //支持range 文件存在 且之前数据库可以查到数据，可以继续使用原文件
            //查不到的情况下文件即使存在，也不继续使用了
            mUseExistFile = true;
        }
    }

    private void setContentLength(Map<String, List<String>> headers) {
        //获取长度
        if (mDownloadTask.getDownloadTaskData().getProgressInfo().getTotalSize() == 0) {
            if (headers.containsKey("Content-Length")) {
                List<String> contentLengthList = headers.get("Content-Length");
                if (contentLengthList != null && !contentLengthList.isEmpty()) {
                    //保存文件大小，更新到数据库
                    mDownloadTask.getDownloadTaskData().getProgressInfo().setTotalSize(Long.valueOf(contentLengthList.get(0)));
                    mDownloadDBHelper.insertOrUpdate(mDownloadTask.getDownloadTaskData());
                }
            } else {
                //保存文件大小，更新到数据库
                mDownloadTask.getDownloadTaskData().getProgressInfo().setTotalSize(DownloadTask.CHUNKED_TOTAL_SIZE);
                mDownloadDBHelper.insertOrUpdate(mDownloadTask.getDownloadTaskData());
            }
        }
    }

    private File createFile() throws IOException {
        //新建文件夹
        File dirFile = new File(mDownloadTask.getDownloadTaskData().getDownloadPath());
        dirFile.mkdirs();
        int dotPosition = mFilename.lastIndexOf('.');
        String prefix = mFilename.substring(0, dotPosition == -1 ? mFilename.length() : dotPosition);
        String postfix = dotPosition == -1 ? "" : mFilename.substring(dotPosition);
        File createFile = new File(dirFile, mFilename);
        if (!createFile.exists()) {
            //文件不存在直接新建
            createFile.createNewFile();
        } else if (!mUseExistFile) {
            //文件存在看下是否继续使用，不继续使用的需要搞出个不同名字的
            int count = 1;
            while (createFile.exists()) {
                String tmpFilename = prefix + "-" + String.valueOf(count++) + postfix;
                createFile = new File(dirFile, tmpFilename);
            }
            createFile.createNewFile();
            mDownloadTask.getDownloadTaskData().setFilename(createFile.getName());
            mDownloadDBHelper.insertOrUpdate(mDownloadTask.getDownloadTaskData());
        }

        mRaFile = new RandomAccessFile(createFile, "rw");
        mRaFile.seek(mDownloadTask.getDownloadTaskData().getProgressInfo().getTransferredSize());

        mDownloadTask.getDownloadTaskData().setState(DownloadTaskState.DOWNLOADING);
        if (mInnerStateChangeListener != null) {
            mInnerStateChangeListener.onStateChanged(mDownloadTask, DownloadTaskState.DOWNLOADING);
        }
        mDownloadDBHelper.insertOrUpdate(mDownloadTask.getDownloadTaskData());
        return createFile;
    }

    private HttpURLConnection initConnection(String downloadUrl) throws IOException {
        HttpURLConnection httpConnection;

        URL requestUrl = new URL(downloadUrl);

        httpConnection = (HttpURLConnection) requestUrl.openConnection();

        httpConnection.setConnectTimeout(Constants.CONNECT_TIMEOUT);
        httpConnection.setReadTimeout(Constants.READ_TIMEOUT);
        httpConnection.setUseCaches(false);

        String body = mDownloadTask.getDownloadTaskData().getBody();
        //TODO 有body就是POST，需要下载服务器支持,加body有风险，如果服务器不支持，这个线程就卡在这里了，也不会超时，有点奇怪
        //感觉有需要传递的东西可以放到header里面
        if (!TextUtils.isEmpty(body)) {
            httpConnection.setRequestMethod("POST");
            // 发送POST请求必须设置如下两行
            httpConnection.setDoOutput(true);
            httpConnection.setDoInput(true);
            // 获取URLConnection对象对应的输出流
            PrintWriter printWriter = new PrintWriter(httpConnection.getOutputStream());
            // 发送请求参数
            printWriter.write(body);
            // flush输出流的缓冲
            printWriter.flush();
            printWriter.close();
        } else {
            httpConnection.setRequestMethod("GET");
        }

        //加入Range参数
        httpConnection.setRequestProperty("Range", String.format("bytes=%d-", mDownloadTask.getDownloadTaskData().getProgressInfo().getTransferredSize()));
        //加入额外的header
        Map<String, String> extraHeaders = mDownloadTask.getDownloadTaskData().getHeaders();
        if (extraHeaders != null) {
            for (String key : extraHeaders.keySet()) {
                httpConnection.setRequestProperty(key, extraHeaders.get(key));
            }
        }
        return httpConnection;
    }

    private boolean processData(HttpURLConnection httpConnection, File downloadFile) throws IOException {
        long lastTransferredTimestamp = System.currentTimeMillis();
        int size = 0;

        mBis = new BufferedInputStream(httpConnection.getInputStream());
        ProgressInfo progressInfo = mDownloadTask.getDownloadTaskData().getProgressInfo();
        long lastTransferredSize = progressInfo.getTransferredSize();
        long currentTransferTimestamp;
        byte[] buf = new byte[Constants.DATA_BUFFER_SIZE];
        //读取数据,直到数据读取完毕或者被暂停和取消
        while ((size = mBis.read(buf)) != -1 && !mNeedCancel && !mNeedPause) {
            mRaFile.write(buf, 0, size);
            currentTransferTimestamp = System.currentTimeMillis();
            progressInfo.setTransferredSize(progressInfo.getTransferredSize() + size);
            //设置进度
            if (progressInfo.getTotalSize() != DownloadTask.CHUNKED_TOTAL_SIZE) {
                progressInfo.setPercent((float) progressInfo.getTransferredSize() / progressInfo.getTotalSize());
            }

            //限制一下进度更新速度，一方面是性能原因，另一方面是太快的情况下，计算出来的速度容易不正常
            if (currentTransferTimestamp - lastTransferredTimestamp >= Constants.DOWNLOAD_SPEED_CAL_INTERVAL) {
                progressInfo.setTransferSpeed((float) (progressInfo.getTransferredSize() - lastTransferredSize)
                        / ((currentTransferTimestamp - lastTransferredTimestamp) / 1000f));

                if (mInnerStateChangeListener != null) {
                    mInnerStateChangeListener.onProgressChanged(mDownloadTask, progressInfo);
                }
                mDownloadDBHelper.insertOrUpdate(mDownloadTask.getDownloadTaskData());
                lastTransferredTimestamp = System.currentTimeMillis();
                lastTransferredSize = progressInfo.getTransferredSize();
            }

        }
        //处理暂停或者取消动作，需要更新状态
        if (size != -1 && (mNeedPause || mNeedCancel)) {
            if (mNeedPause) {
                LogUtil.d(TAG, "task paused...");
                mDownloadTask.getDownloadTaskData().setState(DownloadTaskState.PAUSING);
                mDownloadDBHelper.insertOrUpdate(mDownloadTask.getDownloadTaskData());
                if (mInnerStateChangeListener != null) {
                    mInnerStateChangeListener.onStateChanged(mDownloadTask, DownloadTaskState.PAUSING);
                }
            } else if (mNeedCancel) {
                LogUtil.d(TAG, "task cancelled...");
                downloadFile.delete();
                mDownloadDBHelper.deleteByTaskId(mDownloadTask.getDownloadTaskData().getTaskId());
                if (mInnerStateChangeListener != null) {
                    mInnerStateChangeListener.onStateChanged(mDownloadTask, DownloadTaskState.CANCELLED);
                }
            }
            return false;
        }

        //防止发生除零错误
        currentTransferTimestamp = System.currentTimeMillis();
        if (currentTransferTimestamp - lastTransferredSize > 0) {
            progressInfo.setTransferSpeed((float) (progressInfo.getTransferredSize() - lastTransferredSize)
                    / ((currentTransferTimestamp - lastTransferredTimestamp) / 1000f));
            if (mInnerStateChangeListener != null) {
                mInnerStateChangeListener.onProgressChanged(mDownloadTask, progressInfo);
            }
        }

        //读取数据完毕，下载完成
        mDownloadTask.getDownloadTaskData().setState(DownloadTaskState.DONE);
        if (mInnerStateChangeListener != null) {
            mInnerStateChangeListener.onStateChanged(mDownloadTask, DownloadTaskState.DONE);
        }
        mDownloadDBHelper.insertOrUpdate(mDownloadTask.getDownloadTaskData());
        LogUtil.d(TAG, "task completed...");
        return true;
    }
}
