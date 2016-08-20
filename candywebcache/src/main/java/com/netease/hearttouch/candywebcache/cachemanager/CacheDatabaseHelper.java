package com.netease.hearttouch.candywebcache.cachemanager;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.io.File;

/**
 * Created by netease on 16/6/12.
 */
public class CacheDatabaseHelper extends SQLiteOpenHelper {
    public static final String WEBAPP_INFO_TABLE_NAME = "webappinfo";
    public static final String FILE_INFO_TABLE_NAME = "fileinfo";
    private static final String DATABASE_NAME = "cache_info.db";
    private static final int DATABASE_VERSION = 1;

    private final String mDatabaseFilePath;

    public CacheDatabaseHelper(Context context, String databasePath) {
        super(context, databasePath + File.separator + DATABASE_NAME, null, DATABASE_VERSION);
        mDatabaseFilePath = databasePath + File.separator + DATABASE_NAME;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS webappinfo");
        db.execSQL("CREATE TABLE IF NOT EXISTS webappinfo" +
                "(_id INTEGER PRIMARY KEY AUTOINCREMENT, domain VARCHAR, name VARCHAR, " +
                "local_version VARCHAR, md5 VARCHAR, local_path VARCHAR, pkg_path VARCHAR, status INTEGER, " +
                "full_url VARCHAR, update_percent INTEGER, disk_size INTEGER)");

        db.execSQL("DROP TABLE IF EXISTS fileinfo");
        db.execSQL("CREATE TABLE IF NOT EXISTS fileinfo" +
                "(_id INTEGER PRIMARY KEY AUTOINCREMENT, url VARCHAR, appname VARCHAR, " +
                "md5 VARCHAR, local_path VARCHAR, status INTEGER, accessCount INTEGER)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    boolean checkDatabaseValidation() {
        File databaseFile = new File(mDatabaseFilePath);
        return databaseFile.exists();
    }
}
