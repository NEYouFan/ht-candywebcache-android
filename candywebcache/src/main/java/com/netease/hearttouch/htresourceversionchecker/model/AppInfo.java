package com.netease.hearttouch.htresourceversionchecker.model;

import android.text.TextUtils;

import com.netease.hearttouch.htresourceversionchecker.Constants;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

/**
 * Created by DING on 16/6/8.
 */
public class AppInfo {
    private String version = Constants.PROTOCOL_VERSION_0_2;
    private String appID;
    private String appVersion;
    private String platform;
    private boolean isDiff = false;
    private boolean autoFill = false;
    private List<RequestResInfo> resInfos;
    private String userData;

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getAppID() {
        return appID;
    }

    public void setAppID(String appID) {
        this.appID = appID;
    }

    public String getAppVersion() {
        return appVersion;
    }

    public void setAppVersion(String appVersion) {
        this.appVersion = appVersion;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public boolean isDiff() {
        return isDiff;
    }

    public void setIsDiff(boolean isDiff) {
        this.isDiff = isDiff;
    }

    public boolean isAutoFill() {
        return autoFill;
    }

    public void setAutoFill(boolean autoFill) {
        this.autoFill = autoFill;
    }

    public List<RequestResInfo> getResInfos() {
        return resInfos;
    }

    public void setResInfos(List<RequestResInfo> resInfos) {
        this.resInfos = resInfos;
    }

    public String getUserData() {
        return userData;
    }

    public void setUserData(String userData) {
        this.userData = userData;
    }

    public String toJsonString() throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("version", version);
        jsonObject.put("appID", appID);
        jsonObject.put("appVersion", appVersion);
        jsonObject.put("platform", platform);
        jsonObject.put("isDiff", isDiff);
        jsonObject.put("autoFill", autoFill);

        if (resInfos != null && !resInfos.isEmpty()) {
            JSONArray appInfosJsonArray = new JSONArray();
            for (RequestResInfo requestResInfo : resInfos) {
                appInfosJsonArray.put(requestResInfo.toJsonObject());
            }
            jsonObject.put("resInfos", appInfosJsonArray);
        }

        if (!TextUtils.isEmpty(userData)) {
            jsonObject.put("userData", userData);
        }
        return jsonObject.toString();
    }
}
