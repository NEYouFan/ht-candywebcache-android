package com.netease.hearttouch.candywebcache.cachemanager;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Created by netease on 16/6/12.
 */
public class DatabaseManager {
    private static final String TAG = "DatabaseManager";

    private final String mDatabaseDirPath;

    private Context mContext;
    private CacheDatabaseHelper mDatabaseHelper;

    public DatabaseManager(Context context, String databaseDirPath) {
        mContext = context;
        mDatabaseDirPath = databaseDirPath;
        mDatabaseHelper = new CacheDatabaseHelper(context, databaseDirPath);
    }

    public List<WebappInfo> getAllWebappInfo() {
        List<WebappInfo> allAppInfos = new ArrayList<>();
        SQLiteDatabase db = null;
        try {
            db = mDatabaseHelper.getReadableDatabase();
            Cursor cursor = db.rawQuery("SELECT * FROM " + CacheDatabaseHelper.WEBAPP_INFO_TABLE_NAME, null);
            if (cursor != null) {
                try {
                    while (cursor.moveToNext()) {
                        String domain = cursor.getString(cursor.getColumnIndex("domain"));
                        List<String> domainList = new ArrayList<>();
                        if (!TextUtils.isEmpty(domain)) {
                            String[] domains = domain.split(";");
                            for (String dom : domains) {
                                domainList.add(dom);
                            }
                        }

                        String appName = cursor.getString(cursor.getColumnIndex("name"));
                        String localVersion = cursor.getString(cursor.getColumnIndex("local_version"));

                        String md5 = cursor.getString(cursor.getColumnIndex("md5"));
                        md5 = EnDecryptionUtils.decode(md5);

                        String localPath = cursor.getString(cursor.getColumnIndex("local_path"));
                        int status = cursor.getInt(cursor.getColumnIndex("status"));
                        String fullUrl = cursor.getString(cursor.getColumnIndex("full_url"));
                        int update_percent = cursor.getInt(cursor.getColumnIndex("update_percent"));
                        long diskSize = cursor.getInt(cursor.getColumnIndex("disk_size"));
                        String pkgFilePath = cursor.getString(cursor.getColumnIndex("pkg_path"));

                        WebappInfo webappInfo = new WebappInfo(domainList, fullUrl, appName, localVersion, localPath,
                                diskSize, pkgFilePath, md5, status);
                        allAppInfos.add(webappInfo);
                    }
                } finally {
                    cursor.close();
                }
            }
        } catch (Exception e) {
            if (db != null) {
                db.close();
            }
            mDatabaseHelper = new CacheDatabaseHelper(mContext, mDatabaseDirPath);
            e.printStackTrace();
        }
        return allAppInfos;
    }

    private  ContentValues getContentValues(WebappInfo webappInfo) {
        ContentValues cv = new ContentValues();

        Set<String> domains = webappInfo.mDomains;
        String domain = "";
        if (domains.size() > 0) {
            StringBuilder domainBuilder = new StringBuilder();
            boolean firstOne = true;
            Iterator<String> iter = domains.iterator();
            while (iter.hasNext()) {
                String thedomain = iter.next();
                if (!firstOne) {
                    domainBuilder.append(";");
                } else {
                    firstOne = false;
                }
                domainBuilder.append(thedomain);
            }
            domain = domainBuilder.toString();
        }

        cv.put("domain", domain);
        cv.put("name", webappInfo.mWebappName);
        cv.put("local_version", webappInfo.mVerStr);
        cv.put("md5", EnDecryptionUtils.encode(webappInfo.mPkgFileMd5));
        cv.put("local_path", webappInfo.mCachedDirPath);
        cv.put("pkg_path", webappInfo.mPkgFilePath);
        cv.put("status", webappInfo.mStatus);
        cv.put("full_url", webappInfo.mFullUrl);
        cv.put("update_percent", 0);
        cv.put("disk_size", webappInfo.mCacheSize);
        return cv;
    }

    boolean insertWebappInfo(WebappInfo webappInfo) {
        ContentValues cv =getContentValues(webappInfo);
        SQLiteDatabase db = null;
        try {
            db = mDatabaseHelper.getWritableDatabase();
            long rowId = db.insert(CacheDatabaseHelper.WEBAPP_INFO_TABLE_NAME, null, cv);
            if (rowId < 0) {
                return false;
            }
        } catch (Exception e) {
            if (db != null) {
                db.close();
            }
            mDatabaseHelper = new CacheDatabaseHelper(mContext, mDatabaseDirPath);
            e.printStackTrace();
        }
        return true;
    }

    boolean updateWebappInfo(WebappInfo webappInfo) {
        ContentValues cv =getContentValues(webappInfo);
        SQLiteDatabase db = null;
        try {
            db = mDatabaseHelper.getWritableDatabase();
            int numRows = db.update(CacheDatabaseHelper.WEBAPP_INFO_TABLE_NAME, cv, "name = ?",
                    new String[]{webappInfo.mWebappName});

            if (numRows <= 0) {
                return false;
            }
        } catch (Exception e) {
            if (db != null) {
                db.close();
            }
            mDatabaseHelper = new CacheDatabaseHelper(mContext, mDatabaseDirPath);
            e.printStackTrace();
        }
        return true;
    }

    void deleteWebappInfo(String appid) {
        SQLiteDatabase db = null;
        try {
            db = mDatabaseHelper.getWritableDatabase();
            db.delete(CacheDatabaseHelper.WEBAPP_INFO_TABLE_NAME, "name = ?", new String[]{appid});
        } catch (Exception e) {
            if (db != null) {
                db.close();
            }
            mDatabaseHelper = new CacheDatabaseHelper(mContext, mDatabaseDirPath);
            e.printStackTrace();
        }
    }

    List<FileInfo> getFileInfos() {
        List<FileInfo> fileInfos = new ArrayList<>();
        SQLiteDatabase db = null;
        try {
            db = mDatabaseHelper.getReadableDatabase();
            Cursor cursor = db.rawQuery("SELECT * FROM " + CacheDatabaseHelper.FILE_INFO_TABLE_NAME, null);
            if (cursor != null) {
                try {
                    while (cursor.moveToNext()) {
                        String url = cursor.getString(cursor.getColumnIndex("url"));
                        String appname = cursor.getString(cursor.getColumnIndex("appname"));

                        String md5 = cursor.getString(cursor.getColumnIndex("md5"));
                        md5 = EnDecryptionUtils.decode(md5);

                        String localpath = cursor.getString(cursor.getColumnIndex("local_path"));
                        int accessCount = cursor.getInt(cursor.getColumnIndex("accessCount"));
                        int status = cursor.getInt(cursor.getColumnIndex("status"));
                        FileInfo fileInfo = new FileInfo(md5, localpath, appname, url, accessCount, status);
                        fileInfos.add(fileInfo);
                    }
                } finally {
                    cursor.close();
                }
            }
        } catch (Exception e) {
            if (db != null) {
                db.close();
            }
            mDatabaseHelper = new CacheDatabaseHelper(mContext, mDatabaseDirPath);
            e.printStackTrace();
        }
        return fileInfos;
    }

    private ContentValues getContentValues(FileInfo fileInfo) {
        ContentValues cv = new ContentValues();

        cv.put("url", fileInfo.mUrl);
        cv.put("appname", fileInfo.mAppname);
        cv.put("md5", EnDecryptionUtils.encode(fileInfo.mMd5));
        cv.put("local_path", fileInfo.mLocalPath);
        cv.put("status", fileInfo.getStatus());
        cv.put("accessCount", fileInfo.getmAccessCount());
        return cv;
    }

    boolean updateFileInfo(FileInfo fileInfo) {
        ContentValues cv =getContentValues(fileInfo);
        SQLiteDatabase db = null;
        try {
            db = mDatabaseHelper.getWritableDatabase();
            int numRows = db.update(CacheDatabaseHelper.FILE_INFO_TABLE_NAME, cv, "appname = ? AND local_path = ?",
                    new String[]{fileInfo.mAppname, fileInfo.mLocalPath});

            if (numRows <= 0) {
                return false;
            }
        } catch (Exception e) {
            if (db != null) {
                db.close();
            }
            mDatabaseHelper = new CacheDatabaseHelper(mContext, mDatabaseDirPath);
            e.printStackTrace();
        }
        return true;
    }

    boolean updateFileInfos(List<FileInfo> fileInfos) {
        SQLiteDatabase db = null;
        try {
            db = mDatabaseHelper.getWritableDatabase();
            db.beginTransaction();
            for (FileInfo fileInfo : fileInfos) {
                String sql = String.format("INSERT OR REPLACE INTO %s (url,appname,md5,local_path,status,accessCount) "
                                + "VALUES('%s','%s','%s','%s', '%d', '%d');",
                        CacheDatabaseHelper.FILE_INFO_TABLE_NAME, fileInfo.mUrl, fileInfo.mAppname,
                        EnDecryptionUtils.encode(fileInfo.mMd5), fileInfo.mLocalPath, fileInfo.getStatus(), fileInfo.getmAccessCount());
                db.execSQL(sql);
            }
            db.setTransactionSuccessful();
            db.endTransaction();
        } catch (Exception e) {
            if (db != null) {
                db.close();
            }
            mDatabaseHelper = new CacheDatabaseHelper(mContext, mDatabaseDirPath);
            e.printStackTrace();
        }
        return true;
    }

    void deleteWebappFileInfos(String appid) {
        SQLiteDatabase db = null;
        try {
            db = mDatabaseHelper.getWritableDatabase();
            db.delete(CacheDatabaseHelper.FILE_INFO_TABLE_NAME, "appname = ?", new String[]{appid});
        } catch (Exception e) {
            if (db != null) {
                db.close();
            }
            mDatabaseHelper = new CacheDatabaseHelper(mContext, mDatabaseDirPath);
            e.printStackTrace();
        }
    }

    boolean checkDatabaseValidation() {
        return mDatabaseHelper.checkDatabaseValidation();
    }

    void closeDB() {
        SQLiteDatabase db;
        try {
            db = mDatabaseHelper.getWritableDatabase();
            db.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
