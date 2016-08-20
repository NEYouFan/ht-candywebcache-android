package com.netease.hearttouch.htfiledownloader;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by DING on 16/6/4.
 */
public enum FileDownloadManager implements InnerStateChangeListener {
    INSTANCE;

    private static String TAG = FileDownloadManager.class.getSimpleName();
    private static Context sContext;
    private int mConcurrentTaskNum = Runtime.getRuntime().availableProcessors() + 1;
    private ExecutorService mExecutorService = Executors.newFixedThreadPool(mConcurrentTaskNum);
    private ExecutorService mFileManagerExecutorService = Executors.newCachedThreadPool();
    //假设list的第一个是优先级最高的
    private List<DownloadTask> mDownloadTaskList = new LinkedList<>();
    private Map<Integer, FileDownloadRunnable> mTaskRunnables = new HashMap<>();
    private Map<Integer, Set<DownloadTask>> mSameTasks = new HashMap<>();

    private Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            //TODO 进度是否需要回调到UI线程，后面做个开关在这里
        }
    };

    /**
     * 初始化文件下载模块，应尽早调用
     *
     * @param context           推荐使用应用的ApplicationContext
     * @param downloadDbDirPath 下载模块数据库文件的保存路径
     * @param downloadPath      下载文件的保存路径
     */
    public static void init(Context context, String downloadDbDirPath, String downloadPath) {
        sContext = context;
        DownloadTask.initDefaultDownloadPath(downloadPath);
        DownloadDBHelper.init(context, downloadDbDirPath);
    }

    /**
     * 获取FileDownLoaderManager实例
     *
     * @return
     */
    public static FileDownloadManager getInstance() {
        if (sContext == null) {
            throw new IllegalArgumentException("must init and set application context first!");
        }
        return INSTANCE;
    }

    // TODO
    public void setMaxConcurrentTask(int taskNum) {
    }

    /**
     * 获取下载模块保存的当前非完成状态的任务,注意*同步接口*，请自己处理异步情况
     *
     * @return 下载任务的保存数据的列表
     */
    public List<DownloadTask.DownloadTaskData> getRemainingTasks() {
        final List<DownloadTask.DownloadTaskData> result = new ArrayList<>();
        List<DownloadTask.DownloadTaskData> tmpResult = DownloadDBHelper.getInstance().getRemainingTasks();
        if (tmpResult != null && !tmpResult.isEmpty()) {
            result.addAll(tmpResult);
        }

        if (result.isEmpty()) {
            return null;
        } else {
            return result;
        }
    }

    synchronized void syncWithDb(final DownloadTask downloadTask) {
        //已经包含了这个task，就别继续加入了,加入到相同任务的集合中,同时把状态什么的都同步起来
        mFileManagerExecutorService.submit(new Runnable() {
            @Override
            public void run() {
                DownloadTask.DownloadTaskData downloadTaskData = DownloadDBHelper.getInstance().getByTaskId(downloadTask.getDownloadTaskData().getTaskId());
                if (downloadTaskData != null) {
                    downloadTask.getDownloadTaskData().copyFrom(downloadTaskData);
                }
            }
        });
    }

    synchronized void addTask(final DownloadTask downloadTask) {
        final int taskId = downloadTask.getDownloadTaskData().getTaskId();
        //已经包含了这个task，就别继续加入了,加入到相同任务的集合中,同时把状态什么的都同步起来
        if (mSameTasks.containsKey(taskId)) {
            LogUtil.d(TAG, "already has same task,add to same task set...");
            mSameTasks.get(taskId).add(downloadTask);
            return;
        }
        Set<DownloadTask> downloadTasks = new HashSet<>();
        downloadTasks.add(downloadTask);
        mSameTasks.put(taskId, downloadTasks);
        if (mTaskRunnables.size() < mConcurrentTaskNum) {
            LogUtil.d(TAG, "add to thread pool...");
            FileDownloadRunnable taskRunnable = new FileDownloadRunnable(downloadTask, this);
            mTaskRunnables.put(taskId, taskRunnable);
            mExecutorService.submit(taskRunnable);
        } else {
            //当前没有空闲，先加入队列
            LogUtil.d(TAG, "thread all busy, add to waiting list...");
            mDownloadTaskList.add(downloadTask);
        }
    }

    void start(DownloadTask downloadTask) {
//        syncWithDb(downloadTask);
        addTask(downloadTask);
    }

    void pause(final DownloadTask downloadTask) {
        int taskId = downloadTask.getDownloadTaskData().getTaskId();
        //没有运行中
        if (!mTaskRunnables.containsKey(taskId)) {
            LogUtil.d(TAG, "pause task not running");
            //直接通知暂停
            if (mSameTasks.containsKey(taskId)) {
                mSameTasks.get(taskId).add(downloadTask);
                for (DownloadTask task : mSameTasks.get(taskId)) {
                    task.getDownloadTaskData().setState(DownloadTaskState.PAUSING);
                    StateChangeListener listener = task.getStateChangeListener();
                    if (listener != null) {
                        listener.onStateChanged(task, task.getDownloadTaskData().getState());
                    }
                }
            } else {
                downloadTask.getDownloadTaskData().setState(DownloadTaskState.PAUSING);
                StateChangeListener listener = downloadTask.getStateChangeListener();
                if (listener != null) {
                    listener.onStateChanged(downloadTask, downloadTask.getDownloadTaskData().getState());
                }
            }
            removeTask(taskId);
        } else {
            LogUtil.d(TAG, "pause task running, waiting...");
            if (mSameTasks.containsKey(taskId)) {
                mSameTasks.get(taskId).add(downloadTask);
            }
            FileDownloadRunnable fileDownloadRunnable = mTaskRunnables.get(taskId);
            if (fileDownloadRunnable != null) {
                fileDownloadRunnable.pause();
            }
        }
    }

    void cancel(final DownloadTask downloadTask) {
        final int taskId = downloadTask.getDownloadTaskData().getTaskId();
        //没有运行中
        if (!mTaskRunnables.containsKey(taskId)) {
            LogUtil.d(TAG, "cancel task not running...");
            //直接通知停止
            if (mSameTasks.containsKey(taskId)) {
                mSameTasks.get(taskId).add(downloadTask);
                for (DownloadTask task : mSameTasks.get(taskId)) {
                    task.getDownloadTaskData().setState(DownloadTaskState.CANCELLED);
                    StateChangeListener listener = task.getStateChangeListener();
                    if (listener != null) {
                        listener.onStateChanged(task, task.getDownloadTaskData().getState());
                    }
                }
            } else {
                downloadTask.getDownloadTaskData().setState(DownloadTaskState.CANCELLED);
                StateChangeListener listener = downloadTask.getStateChangeListener();
                if (listener != null) {
                    listener.onStateChanged(downloadTask, downloadTask.getDownloadTaskData().getState());
                }
            }
            removeTask(taskId);

            mFileManagerExecutorService.submit(new Runnable() {
                @Override
                public void run() {
                    LogUtil.d(TAG, "cancel task, deleting files and db record...");
                    //删除文件
                    String path = downloadTask.getDownloadTaskData().getDownloadPath();
                    String filename = downloadTask.getDownloadTaskData().getFilename();
                    if (!TextUtils.isEmpty(filename)) {
                        File file = new File(path, filename);
                        if (!file.isDirectory() && file.exists()) {
                            file.delete();
                        }
                    }
                    DownloadDBHelper.getInstance().deleteByTaskId(taskId);
                }
            });
        } else {
            LogUtil.d(TAG, "cancel task running, waiting...");
            if (mSameTasks.containsKey(taskId)) {
                mSameTasks.get(taskId).add(downloadTask);
            }
            FileDownloadRunnable fileDownloadRunnable = mTaskRunnables.get(taskId);
            if (fileDownloadRunnable != null) {
                fileDownloadRunnable.cancel();
            }
        }
    }

    synchronized private void removeTask(int taskId) {
        //从队列删除
        if (mDownloadTaskList != null) {
            //还没进入线程池，直接从队列删掉
            Iterator<DownloadTask> iterator = mDownloadTaskList.iterator();
            while (iterator.hasNext()) {
                DownloadTask tmpDownloadTask = iterator.next();
                if (tmpDownloadTask.getDownloadTaskData().getTaskId() == taskId) {
                    iterator.remove();
                }
            }
        }
        //从当前任务删除
        if (mTaskRunnables.containsKey(taskId)) {
            mTaskRunnables.remove(taskId);
            if (!mDownloadTaskList.isEmpty()) {
                FileDownloadRunnable taskRunnable = new FileDownloadRunnable(mDownloadTaskList.get(0), this);
                mTaskRunnables.put(mDownloadTaskList.get(0).getDownloadTaskData().getTaskId(), taskRunnable);
                mExecutorService.submit(taskRunnable);
                //加入到线程池中的需要从队列里面移除
                mDownloadTaskList.remove(0);
            }
        }

        if (mSameTasks.containsKey(taskId)) {
            mSameTasks.remove(taskId);
        }

    }

    /**
     * 提供关闭数据库接口
     * 由于删除数据库文件等可能会导致数据库是打开状态的，相关路径上的文件不能创建
     */
    public void closeDownloadDatabase() {
        try {
            DownloadDBHelper.getInstance().close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean isDownloadDbValid() {
        return DownloadDBHelper.getInstance().isDbValid();
    }

    public static void enableLog(boolean enable) {
        LogUtil.enableLog(enable);
    }

    public static void setLogLevel(int level) {
        LogUtil.setLevel(level);
    }

    @Override
    public void onProgressChanged(DownloadTask downloadTask, ProgressInfo progressInfo) {
        LogUtil.d(TAG, "Progress changed, task:" + downloadTask.getDownloadTaskData().getTaskId() + ", Progress:" + progressInfo.toString());
        //本任务也在这个set中
        Set<DownloadTask> sameTasks = mSameTasks.get(downloadTask.getDownloadTaskData().getTaskId());
        if (sameTasks != null) {
            for (DownloadTask task : sameTasks) {
                task.getDownloadTaskData().copyFrom(downloadTask.getDownloadTaskData());
                StateChangeListener stateChangeListener = task.getStateChangeListener();
                if (stateChangeListener != null) {
                    stateChangeListener.onProgressChanged(task, progressInfo);
                }
            }
        }
    }

    @Override
    public void onStateChanged(DownloadTask downloadTask, DownloadTaskState state) {
        LogUtil.d(TAG, "State changed task:" + downloadTask.getDownloadTaskData().getTaskId() + ", State:" + state);
        //本任务也在这个set中
        Set<DownloadTask> sameTasks = mSameTasks.get(downloadTask.getDownloadTaskData().getTaskId());
        if (sameTasks != null) {
            for (DownloadTask task : sameTasks) {
                task.getDownloadTaskData().copyFrom(downloadTask.getDownloadTaskData());
                StateChangeListener stateChangeListener = task.getStateChangeListener();
                if (stateChangeListener != null) {
                    stateChangeListener.onStateChanged(task, state);
                }
            }
        }

        if (state == DownloadTaskState.FAILED
                || state == DownloadTaskState.PAUSING
                || state == DownloadTaskState.CANCELLED
                || state == DownloadTaskState.DONE) {
            removeTask(downloadTask.getDownloadTaskData().getTaskId());
        }
    }

}
