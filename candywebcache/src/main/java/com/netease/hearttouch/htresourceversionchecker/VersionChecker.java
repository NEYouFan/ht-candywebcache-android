package com.netease.hearttouch.htresourceversionchecker;

import android.text.TextUtils;
import android.util.Log;

import com.netease.hearttouch.htresourceversionchecker.model.AppInfo;
import com.netease.hearttouch.htresourceversionchecker.model.ResponseResInfo;
import com.netease.hearttouch.htresourceversionchecker.model.VersionCheckResponseData;
import com.netease.hearttouch.htresourceversionchecker.model.VersionCheckResponseModel;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.List;
import java.util.Map;

/**
 * Created by DING on 16/6/8.
 */
public class VersionChecker {
    public static final String PACKAGE_MODE_ZIP = "zip";
    public static final String PACKAGE_MODE_DEFAULT = PACKAGE_MODE_ZIP;

    public static final String COMPRESS_MODE_NONE = "none";
    public static final String COMPRESS_MODE_GZIP = "gzip";
    public static final String COMPRESS_MODE_BRO = "bro";
    public static final String COMPRESS_MODE_DEFAULT = COMPRESS_MODE_NONE;

    public static final String DIFF_MDOE_BSDIFF = "bsdiff";
    public static final String DIFF_MODE_COURGETTE = "courgette";
    public static final String DIFF_MODE_DEFAULT = DIFF_MDOE_BSDIFF;


    /**
     * 进行版本检测，同步接口
     *
     * @param url                检测的目标url
     * @param appInfo            检测的AppInfo信息
     * @param extraHeaders       额外的Header数据 可以为{@code null}
     * @param onResponseListener 响应的监听，可以进行数据的解密等操作 可以为{@code null}
     * @return 检测的结果，如果有错误，也包含在返回的{@link VersionCheckResponseModel}中
     */
    static public VersionCheckResponseModel checkVersion(String url, AppInfo appInfo, Map<String, String> extraHeaders, OnResponseListener onResponseListener) {
        if (TextUtils.isEmpty(url) || appInfo == null
                || TextUtils.isEmpty(appInfo.getAppID())
                || TextUtils.isEmpty(appInfo.getAppVersion())
                || TextUtils.isEmpty(appInfo.getPlatform())) {
            return new VersionCheckResponseModel(ResultCode.PARAMS_ERROR, "please check params...");
        }

        String body = null;
        try {
            body = appInfo.toJsonString();
            Log.d("%s", "Request body: " + body);
        } catch (JSONException e) {
            e.printStackTrace();
            return new VersionCheckResponseModel(ResultCode.JSON_EXCEPTION, "parse request json string error...");
        }

        BufferedInputStream bis = null;
        ByteArrayOutputStream baos = null;

        try {
            HttpURLConnection httpConnection;
            URL requestUrl = new URL(url);
            httpConnection = (HttpURLConnection) requestUrl.openConnection();
            httpConnection.setRequestMethod("POST");
            httpConnection.setConnectTimeout(Constants.CONNECT_TIMEOUT);
            httpConnection.setReadTimeout(Constants.READ_TIMEOUT);
            httpConnection.setUseCaches(false);

            //加入header
            if (extraHeaders != null && !extraHeaders.isEmpty()) {
                for (String key : extraHeaders.keySet()) {
                    httpConnection.setRequestProperty(key, extraHeaders.get(key));
                }
            }
            httpConnection.setRequestProperty("Content-Type", "application/json");

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

            final int responseCode = httpConnection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                bis = new BufferedInputStream(httpConnection.getInputStream());
                //按行读取数据，拼接到结果字符串中
                baos = new ByteArrayOutputStream();
                byte[] data = new byte[Constants.DATA_BUFFER_SIZE];
                int size = 0;
                while ((size = bis.read(data)) != -1) {
                    baos.write(data, 0, size);
                }
                byte[] result = baos.toByteArray();
                if (result != null && result.length > 0) {
                    Log.d("%s", "Response result = " + baos.toString());
                    if (onResponseListener != null) {
                        result = onResponseListener.onResponse(result);
                    }
                    //解析字符串
                    try {
                        return VersionCheckResponseModel.fromJsonString(new String(result, "UTF-8"));
                    } catch (JSONException e) {
                        e.printStackTrace();
                        return new VersionCheckResponseModel(ResultCode.JSON_EXCEPTION, "parse response json string error...");
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                        return new VersionCheckResponseModel(ResultCode.INNER_ERROR_UNSUPPORT_CHARSET, "unsupported charset...");
                    }
                } else {
                    return new VersionCheckResponseModel(ResultCode.HTTP_ERROR_NOTHING_READ, "read nothing from network...");
                }
            } else {
                //其他的错误的response code
                return new VersionCheckResponseModel(responseCode, httpConnection.getResponseMessage());
            }
        } catch (SocketTimeoutException e) {
            e.printStackTrace();
            return new VersionCheckResponseModel(ResultCode.HTTP_ERROR_TIMEOUT, "socket timeout...");
        } catch (IOException e) {
            e.printStackTrace();
            return new VersionCheckResponseModel(ResultCode.INNER_ERROR_IO, "io exception...");
        } finally {
            if (bis != null) {
                try {
                    bis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.w("VersionChecker", e.toString());
                }
            }
            if (baos != null) {
                try {
                    baos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.w("VersionChecker", e.toString());
                }
            }
        }
    }

    /**
     * 根据输入的版本信息，解析出对应的版本检测结果信息数据
     * 输入的应该是List<ResponseResInfo>变成json的字符串，否则解析出错
     *
     * @param jsonStr 客户端自行获取到的版本信息的json字符串
     * @return 根据输入解析出的版本检测结果信息数据
     */
    static public List<ResponseResInfo> getVersionInfo(String jsonStr) {
        try {
            VersionCheckResponseData versionCheckResponseData = VersionCheckResponseData.fromJsonArray(new JSONArray(jsonStr));
            return versionCheckResponseData.getResInfos();
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    public interface ResultCode {
        //http错误或者内部错误
        /** 内部IO错误 */
        int INNER_ERROR_IO = -100;
        /** 内部无法解析的字符集 */
        int INNER_ERROR_UNSUPPORT_CHARSET = -101;
        /** 网络请求超时 */
        int HTTP_ERROR_TIMEOUT = -102;
        /** 网络请求没有读取到数据 */
        int HTTP_ERROR_NOTHING_READ = -103;
        /** 参数设置错误 */
        int PARAMS_ERROR = -104;
        /** JSON解析或序列化错误 */
        int JSON_EXCEPTION = -105;

        //服务器的返回码，默认从0开始
        /** 请求成功返回 */
        int SUCCESS = 200;
        /** 协议版本不支持 */
        int ERR_PROTOCOL_VERSION_NOT_SUPPORT = 401;
        /** appID不支持 */
        int ERR_APPID = 402;
        /** 服务端错误 */
        int ERR_SERVER = 501;
        /** 表示native id在服务器检索不到错误 */
        int ERR_UNKNOWN = 601;
    }

    public interface StatusCode {
        /** 资源包最新 */
        int LATEST = 0;
        /** 资源包有更新 */
        int NEED_UPDATE = 1;
        /** 资源包在服务器不存在 */
        int RESOURCE_NOT_EXIST = 2;
        /** 自动补全的资源信息 */
        int RESOURCE_AUTO_FILL = 3;
    }
}
