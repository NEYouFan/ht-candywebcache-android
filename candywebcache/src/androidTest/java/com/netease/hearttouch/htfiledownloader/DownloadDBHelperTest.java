package com.netease.hearttouch.htfiledownloader;

import android.test.InstrumentationTestCase;

import org.junit.Before;

import java.io.File;
import java.util.List;

/**
 * Created by DING on 16/7/16.
 */
public class DownloadDBHelperTest extends InstrumentationTestCase {

    @Before
    public void setUp() throws Exception {
        File file = new File("/sdcard/netease/unit_test/download");
        file.mkdirs();
        DownloadDBHelper.init(getInstrumentation().getContext(),file.getAbsolutePath());
    }

    public void testGetByTaskId() throws Exception {
        DownloadTask downloadTask = new DownloadTask.DownloadTaskBuilder()
                .setDownloadUrl("https://iterm2.com/downloads/stable/iTerm2-2_1_4.zip")
                .build();
        DownloadDBHelper.getInstance().insertOrUpdate(downloadTask.getDownloadTaskData());
        DownloadTask.DownloadTaskData downloadTaskData = DownloadDBHelper.getInstance().getByTaskId(downloadTask.getDownloadTaskData().getTaskId());
        assertNotNull(downloadTaskData);
        assertEquals(downloadTask.getDownloadTaskData().getTaskId(), downloadTaskData.getTaskId());
        assertEquals(downloadTask.getDownloadTaskData().getState(),downloadTaskData.getState());

    }

    public void testInsertOrUpdate() throws Exception {
        //这个在上面也就测到了
    }

    public void testDeleteByTaskId() throws Exception {
        DownloadTask downloadTask = new DownloadTask.DownloadTaskBuilder()
                .setDownloadUrl("https://iterm2.com/downloads/stable/iTerm2-2_1_4.zip")
                .build();
        DownloadDBHelper.getInstance().insertOrUpdate(downloadTask.getDownloadTaskData());
        DownloadTask.DownloadTaskData downloadTaskData = DownloadDBHelper.getInstance().getByTaskId(downloadTask.getDownloadTaskData().getTaskId());
        assertNotNull(downloadTaskData);

        DownloadDBHelper.getInstance().deleteByTaskId(downloadTask.getDownloadTaskData().getTaskId());

        downloadTaskData = DownloadDBHelper.getInstance().getByTaskId(downloadTask.getDownloadTaskData().getTaskId());
        assertNull(downloadTaskData);

    }

    public void testGetRemainingTasks() throws Exception {
        DownloadTask downloadTask = new DownloadTask.DownloadTaskBuilder()
                .setDownloadUrl("https://iterm2.com/downloads/stable/iTerm2-2_1_4.zip")
                .build();
        DownloadDBHelper.getInstance().insertOrUpdate(downloadTask.getDownloadTaskData());
        List<DownloadTask.DownloadTaskData> downloadTaskDatas = DownloadDBHelper.getInstance().getRemainingTasks();
        assertNotNull(downloadTaskDatas);
        assertTrue(!downloadTaskDatas.isEmpty());

        for(DownloadTask.DownloadTaskData downloadTaskData : downloadTaskDatas){
            if(downloadTaskData.getTaskId() == downloadTask.getDownloadTaskData().getTaskId()){
                return;
            }
        }
        throw new Exception("cannot find task in result list...");
    }}