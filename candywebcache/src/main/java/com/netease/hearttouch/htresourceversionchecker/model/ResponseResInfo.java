package com.netease.hearttouch.htresourceversionchecker.model;

import com.netease.hearttouch.htresourceversionchecker.VersionChecker;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by DING on 16/6/8.
 */
public class ResponseResInfo {
    private String resID;
    private int state;
    private String resVersion;
    private String diffUrl;
    private String diffMd5;
    private String fullUrl;
    private String fullMd5;

    private String packageMode = VersionChecker.PACKAGE_MODE_DEFAULT;
    private String compressMode = VersionChecker.COMPRESS_MODE_DEFAULT;
    private String diffMode  = VersionChecker.DIFF_MODE_DEFAULT;

    private String userData;

    public String getResID() {
        return resID;
    }

    public void setResID(String resID) {
        this.resID = resID;
    }

    public String getResVersion() {
        return resVersion;
    }

    public void setResVersion(String resVersion) {
        this.resVersion = resVersion;
    }

    public String getDiffUrl() {
        return diffUrl;
    }

    public void setDiffUrl(String diffUrl) {
        this.diffUrl = diffUrl;
    }

    public String getDiffMd5() {
        return diffMd5;
    }

    public void setDiffMd5(String diffMd5) {
        this.diffMd5 = diffMd5;
    }

    public String getFullUrl() {
        return fullUrl;
    }

    public void setFullUrl(String fullUrl) {
        this.fullUrl = fullUrl;
    }

    public String getFullMd5() {
        return fullMd5;
    }

    public void setFullMd5(String fullMd5) {
        this.fullMd5 = fullMd5;
    }

    public void setPackageMode(String packageMode) {
        this.packageMode = packageMode;
    }

    public String getPackageMode() {
        return packageMode;
    }

    public void setCompressMode(String compressMode) {
        this.compressMode = compressMode;
    }

    public String getCompressMode() {
        return compressMode;
    }

    public void setDiffMode(String diffMode) {
        this.diffMode = diffMode;
    }

    public String getDiffMode() {
        return diffMode;
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }

    public String getUserData() {
        return userData;
    }

    public void setUserData(String userData) {
        this.userData = userData;
    }

    public static ResponseResInfo fromJsonString(String jsonStr) throws JSONException {
        return fromJsonObject(new JSONObject(jsonStr));
    }

    public static ResponseResInfo fromJsonObject(JSONObject jsonObject) throws JSONException {
        if (jsonObject == null) {
            return null;
        }
        ResponseResInfo result = new ResponseResInfo();

        //必须有的字段
        result.resID = jsonObject.getString("resID");
        //必须有的字段
        result.state = jsonObject.getInt("state");

        if (jsonObject.has("resVersion")) {
            result.resVersion = jsonObject.getString("resVersion");
        }
        if (jsonObject.has("diffUrl")) {
            result.diffUrl = jsonObject.getString("diffUrl");
        }
        if (jsonObject.has("diffMd5")) {
            result.diffMd5 = jsonObject.getString("diffMd5");
        }
        if (jsonObject.has("fullUrl")) {
            result.fullUrl = jsonObject.getString("fullUrl");
        }
        if (jsonObject.has("fullMd5")) {
            result.fullMd5 = jsonObject.getString("fullMd5");
        }
        if (jsonObject.has("userData")) {
            result.userData = jsonObject.getString("userData");
        }
        if (jsonObject.has("packageMode")) {
            result.packageMode = jsonObject.getString("packageMode");
        }
        if (jsonObject.has("compressMode")) {
            result.compressMode = jsonObject.getString("compressMode");
        }
        if (jsonObject.has("diffMode")) {
            result.diffMode = jsonObject.getString("diffMode");
        }
        return result;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("resID:").append(resID != null ? resID : "null").append(',')
                .append("resVersion:").append(resVersion != null ? resVersion : "null").append(',')
                .append("diffUrl:").append(diffUrl != null ? diffUrl : "null").append(',')
                .append("diffMd5:").append(diffMd5 != null ? diffMd5 : "null").append(',')
                .append("fullUrl:").append(fullUrl != null ? fullUrl : "null").append(',')
                .append("fullMd5:").append(fullMd5 != null ? fullMd5 : "null").append(',')
                .append("state:").append(state).append(',')
                .append("packageMode").append(packageMode).append(",")
                .append("compressMode").append(compressMode).append(",")
                .append("diffMode").append(diffMode).append(",")
                .append("userData:").append(userData != null ? userData : "null");
        return sb.toString();
    }
}
