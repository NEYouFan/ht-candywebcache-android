package com.netease.hearttouch.htfiledownloader;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by DING on 16/6/13.
 */
public class DownloadDBHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "download_info.db";
    private static final int DATABASE_VERSION = 1;
    //只返回一星期内的任务
    private static final long LATEST_TASK_TIME = 1000 * 60 * 60 * 24 * 7;

    private static volatile DownloadDBHelper sDownloaDBHelper;
    private static Context sContext;
    private static String sDBPath;

    static void init(Context context, String dbPath) {
        sContext = context;
        sDBPath = dbPath;
        getInstance();
        try {
            getInstance().getReadableDatabase();
        } catch (Exception e) {
            //吃掉异常，主要是文件被删除，下次打开应用会重建的
            e.printStackTrace();
        }
    }

    static DownloadDBHelper getInstance() {
        if (sDownloaDBHelper == null) {
            synchronized (DownloadDBHelper.class) {
                if (sDownloaDBHelper == null) {
                    sDownloaDBHelper = new DownloadDBHelper(sContext, sDBPath);
                }
            }
        }
        return sDownloaDBHelper;
    }

    private DownloadDBHelper(Context context, String databasePath) {
        super(context, databasePath + File.separator + DATABASE_NAME, null, DATABASE_VERSION);
    }


    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS download_tasks (" +
                "task_id INTEGER PRIMARY KEY NOT NULL, " +
                "url TEXT NOT NULL," +
                "download_path TEXT  NOT NULL," +
                "filename TEXT," +
                "state INTEGER NOT NULL," +
                "percent REAL NOT NULL," +
                "transferred_size INTEGER NOT NULL," +
                "total_size INTEGER NOT NULL," +
                "time INTEGER NOT NULL)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }

    DownloadTask.DownloadTaskData getByTaskId(int taskId) {
        List<DownloadTask.DownloadTaskData> result = query("SELECT * FROM download_tasks WHERE task_id=" + taskId);
        if (result == null || result.isEmpty()) {
            return null;
        } else {
            return result.get(0);
        }
    }

    void insertOrUpdate(DownloadTask.DownloadTaskData downloadTaskData) {
        try {
            String sql = String.format("INSERT OR REPLACE INTO download_tasks (task_id,url,download_path,filename,state,percent,transferred_size,total_size,time) " +
                            "VALUES(%d,'%s','%s','%s',%d,%f,%d,%d,%d);",
                    downloadTaskData.getTaskId(),
                    downloadTaskData.getUrl(),
                    downloadTaskData.getDownloadPath(),
                    downloadTaskData.getFilename(),
                    downloadTaskData.getState().ordinal(),
                    downloadTaskData.getProgressInfo().getPercent(),
                    downloadTaskData.getProgressInfo().getTransferredSize(),
                    downloadTaskData.getProgressInfo().getTotalSize(),
                    System.currentTimeMillis());
            getWritableDatabase().execSQL(sql);
        } catch (Exception e) {
            //吃掉异常，主要是文件被删除，下次打开应用会重建的
            close();
            e.printStackTrace();
        }
    }

    void deleteByTaskId(int taskId) {
        try {
            String sql = String.format("DELETE FROM download_tasks WHERE task_id=%d;", taskId);
            getWritableDatabase().execSQL(sql);
        } catch (Exception e) {
            //吃掉异常，主要是文件被删除，下次打开应用会重建的
            close();
            e.printStackTrace();
        }
    }

    List<DownloadTask.DownloadTaskData> getRemainingTasks() {
        String sql = String.format("SELECT * from download_tasks WHERE state<>%d AND time>=%d",
                DownloadTaskState.DONE.ordinal(),
                System.currentTimeMillis() - LATEST_TASK_TIME);
        return query(sql);
    }


    private List<DownloadTask.DownloadTaskData> query(String sql) {
        Cursor cursor = null;
        try {
            cursor = getReadableDatabase().rawQuery(sql, null);
            if (cursor.getCount() == 0) {
                return null;
            }
            List<DownloadTask.DownloadTaskData> resultList = new ArrayList<>();
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                DownloadTask.DownloadTaskData downloadTaskData = new DownloadTask.DownloadTaskData();
                downloadTaskData.setTaskId(cursor.getInt(0));
                downloadTaskData.setUrl(cursor.getString(1));
                downloadTaskData.setDownloadPath(cursor.getString(2));
                downloadTaskData.setFilename(cursor.getString(3));
                downloadTaskData.setState(DownloadTaskState.valueOf(cursor.getInt(4)));
                downloadTaskData.getProgressInfo().setPercent(cursor.getFloat(5));
                downloadTaskData.getProgressInfo().setTransferredSize(cursor.getLong(6));
                downloadTaskData.getProgressInfo().setTotalSize(cursor.getLong(7));
                resultList.add(downloadTaskData);
                cursor.moveToNext();
            }
            return resultList;
        } catch (Exception e) {
            //吃掉异常，主要是文件被删除，下次打开应用会重建的
            close();
            e.printStackTrace();
            return null;
        } finally {
            //为了防止上面提前返回导致的cursor泄露
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public boolean isDbValid() {
        File dbFile = new File(sDBPath + File.separator + DATABASE_NAME);
        return dbFile.exists();
    }
}
