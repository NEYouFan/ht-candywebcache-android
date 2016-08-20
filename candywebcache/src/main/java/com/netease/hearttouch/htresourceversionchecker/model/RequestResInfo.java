package com.netease.hearttouch.htresourceversionchecker.model;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by DING on 16/6/8.
 */
public class RequestResInfo {
    private String resID;
    private String resVersion;

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

    public JSONObject toJsonObject() throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("resID", resID);
        jsonObject.put("resVersion", resVersion);
        return jsonObject;
    }
}
