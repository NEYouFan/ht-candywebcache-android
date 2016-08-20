package com.netease.hearttouch.htresourceversionchecker.model;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by DING on 16/6/8.
 */
public class VersionCheckResponseModel {
    private int code;
    private VersionCheckResponseData data;
    private String errMsg;

    public VersionCheckResponseModel() {

    }

    public VersionCheckResponseModel(int code, String errMsg) {
        this.code = code;
        this.errMsg = errMsg;
        data = null;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public VersionCheckResponseData getData() {
        return data;
    }

    public void setData(VersionCheckResponseData data) {
        this.data = data;
    }

    public String getErrMsg() {
        return errMsg;
    }

    public void setErrMsg(String errMsg) {
        this.errMsg = errMsg;
    }

    public static VersionCheckResponseModel fromJsonString(String jsonStr) throws JSONException {
        JSONObject jsonObject = new JSONObject(jsonStr);
        VersionCheckResponseModel result = new VersionCheckResponseModel();
        result.code = jsonObject.getInt("code");
        if (jsonObject.has("data")) {
            result.data = VersionCheckResponseData.fromJsonObject(jsonObject.getJSONObject("data"));
        }
        if (jsonObject.has("errMsg")) {
            result.errMsg = jsonObject.getString("errMsg");
        }
        return result;
    }
}
