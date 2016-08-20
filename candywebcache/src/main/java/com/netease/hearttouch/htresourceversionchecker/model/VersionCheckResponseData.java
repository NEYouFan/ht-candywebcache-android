package com.netease.hearttouch.htresourceversionchecker.model;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by DING on 16/6/8.
 */
public class VersionCheckResponseData {
    private List<ResponseResInfo> resInfos;

    public List<ResponseResInfo> getResInfos() {
        return resInfos;
    }

    public void setResInfos(List<ResponseResInfo> resInfos) {
        this.resInfos = resInfos;
    }

    public static VersionCheckResponseData fromJsonString(String jsonStr) throws JSONException {
        return fromJsonObject(new JSONObject(jsonStr));
    }

    public static VersionCheckResponseData fromJsonObject(JSONObject jsonObject) throws JSONException {
        if (jsonObject == null) {
            return null;
        }
        JSONArray appVersionInfosJsonArray = jsonObject.getJSONArray("resInfos");
        return fromJsonArray(appVersionInfosJsonArray);
    }

    public static VersionCheckResponseData fromJsonArray(JSONArray jsonArray) throws JSONException {
        VersionCheckResponseData versionCheckResponseData = new VersionCheckResponseData();
        if (jsonArray != null) {
            List<ResponseResInfo> resInfosList = new ArrayList<>();
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject object = jsonArray.getJSONObject(i);
                ResponseResInfo responseResInfo = ResponseResInfo.fromJsonObject(object);
                resInfosList.add(responseResInfo);
            }
            versionCheckResponseData.resInfos = resInfosList;
        }
        return versionCheckResponseData;
    }
}
