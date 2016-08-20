package com.netease.hearttouch.htresourceversionchecker;

import com.netease.hearttouch.htresourceversionchecker.model.ResponseResInfo;

import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by DING on 16/7/16.
 */
public class VersionCheckerTest extends TestCase {
    public void testCheckVersion() throws Exception {

    }

    public void testGetVersionInfo() throws Exception {
        String errorJsonStr = "dededawdwefrfa";
        List<ResponseResInfo> errorResult = VersionChecker.getVersionInfo(errorJsonStr);
        assertTrue(errorResult == null);


        List<ResponseResInfo> expectResult = new ArrayList<>();
        ResponseResInfo responseResInfo1 = new ResponseResInfo();
        ResponseResInfo responseResInfo2 = new ResponseResInfo();

        String userData = "{\"domains\":[\"www.163.com\",\"www.126.com\"]}";

        responseResInfo1.setResID("123");
        responseResInfo1.setResVersion("123");
        responseResInfo1.setDiffUrl("testDiffUrl");
        responseResInfo1.setDiffMd5("deafwerf24345fref");
        responseResInfo1.setFullUrl("testFullUrl");
        responseResInfo1.setFullMd5("dewf23k4j546#$5");
        responseResInfo1.setState(1);
        responseResInfo1.setUserData(userData);


        responseResInfo2.setResID("321");
        responseResInfo2.setResVersion("321");
        responseResInfo2.setDiffUrl("testDiffUrl111");
        responseResInfo2.setDiffMd5("deafwerf24345fref1111");
        responseResInfo2.setFullUrl("testFullUrl111");
        responseResInfo2.setFullMd5("dewf23k4j546#$51111");
        responseResInfo2.setState(2);
        responseResInfo2.setUserData(userData);

        expectResult.add(responseResInfo1);
        expectResult.add(responseResInfo2);

        String jsonStr = "[{\"appId\":\"123\",\"version\":\"123\",\"updateTime\":123456789,\"diffUrl\":\"testDiffUrl\",\"diffMd5\":\"deafwerf24345fref\",\"fullUrl\":\"testFullUrl\",\"fullMd5\":\"dewf23k4j546#$5\",\"statusCode\":1,\"domains\":[\"www.163.com\",\"www.126.com\"]},{\"appId\":\"321\",\"version\":\"321\",\"updateTime\":987654321,\"diffUrl\":\"testDiffUrl111\",\"diffMd5\":\"deafwerf24345fref1111\",\"fullUrl\":\"testFullUrl111\",\"fullMd5\":\"dewf23k4j546#$51111\",\"statusCode\":2,\"domains\":[\"www.163.com\",\"www.126.com\"]}]";
        List<ResponseResInfo> result = VersionChecker.getVersionInfo(jsonStr);
        assertEquals(expectResult.toString(), result.toString());

    }
}