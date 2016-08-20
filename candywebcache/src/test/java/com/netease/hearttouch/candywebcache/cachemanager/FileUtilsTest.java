package com.netease.hearttouch.candywebcache.cachemanager;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Created by netease on 16/6/14.
 */
public class FileUtilsTest {

    @Before
    public void setUp() throws Exception {

    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void testGetExtName() throws Exception {
        String filePath1 = "/Users/netease/abcdefg.tar.gz";
        assertEquals(FileUtils.getExtName(filePath1), ".tar.gz");
        String filePath2 = "/Users/netease/.tar.gz";
        assertEquals(FileUtils.getExtName(filePath2), ".tar.gz");
        String filePath3 = "/Users/netease/abc.gz";
        assertEquals(FileUtils.getExtName(filePath3), ".gz");
        String filePath4 = "/Users/netease/abc.zip";
        assertEquals(FileUtils.getExtName(filePath4), ".zip");
        String filePath5 = "/Users/netease/abcgz";
        assertEquals(FileUtils.getExtName(filePath5), "");
        String filePath6 = "/Users/netease/.bashrc";
        assertEquals(FileUtils.getExtName(filePath6), "");
        String filePath7 = ".bashrc";
        assertEquals(FileUtils.getExtName(filePath7), "");
        String filePath8 = "/usr/bin";
        assertEquals(FileUtils.getExtName(filePath8), "");
    }

    @Test
    public void testGetPrimaryFileName() throws Exception {
        String filePath1 = "/Users/netease/abcdefg.tar.gz";
        assertEquals(FileUtils.getPrimaryFileName(filePath1), "abcdefg");
        String filePath2 = "/Users/netease/.tar.gz";
        assertEquals(FileUtils.getPrimaryFileName(filePath2), "");
        String filePath3 = "/Users/netease/abc.gz";
        assertEquals(FileUtils.getPrimaryFileName(filePath3), "abc");
        String filePath4 = "/Users/netease/abc.zip";
        assertEquals(FileUtils.getPrimaryFileName(filePath4), "abc");
        String filePath5 = "/Users/netease/abcgz";
        assertEquals(FileUtils.getPrimaryFileName(filePath5), "abcgz");
        String filePath6 = "/Users/netease/.bashrc";
        assertEquals(FileUtils.getPrimaryFileName(filePath6), ".bashrc");
        String filePath7 = ".bashrc";
        assertEquals(FileUtils.getPrimaryFileName(filePath7), ".bashrc");
        String filePath8 = "/usr/bin";
        assertEquals(FileUtils.getPrimaryFileName(filePath8), "bin");
    }

    @Test
    public void testUnpackPackage() throws Exception {
        File tmpoutdir = new File("/Users/netease/webcacheunittestfiles/", "tmpoutdir");
        FileUtils.deleteDir(tmpoutdir);
        assertTrue(tmpoutdir.mkdirs());

        String filePath = "/Users/netease/webcacheunittestfiles/web_app_new.tgz";

        Map<String, String> fileMd5List = new HashMap<String, String>();
        assertTrue(FileUtils.unpackPackage(tmpoutdir, filePath, fileMd5List));

        assertTrue(new File("/Users/netease/webcacheunittestfiles/tmpoutdir/web_app_new/core.css").exists());
        assertEquals(Md5Utils.calculateMd5("/Users/netease/webcacheunittestfiles/tmpoutdir/web_app_new/core.css"),
                Md5Utils.calculateMd5("/Users/netease/webcacheunittestfiles/web_app_new_full/core.css"));

        assertTrue(new File("/Users/netease/webcacheunittestfiles/tmpoutdir/web_app_new/core.js").exists());
        assertEquals(Md5Utils.calculateMd5("/Users/netease/webcacheunittestfiles/tmpoutdir/web_app_new/core.js"),
                Md5Utils.calculateMd5("/Users/netease/webcacheunittestfiles/web_app_new_full/core.js"));

        assertTrue(new File("/Users/netease/webcacheunittestfiles/tmpoutdir/web_app_new/pt_main.js").exists());
        assertEquals(Md5Utils.calculateMd5("/Users/netease/webcacheunittestfiles/tmpoutdir/web_app_new/pt_main.js"),
                Md5Utils.calculateMd5("/Users/netease/webcacheunittestfiles/web_app_new_full/pt_main.js"));

        assertTrue(new File("/Users/netease/webcacheunittestfiles/tmpoutdir/web_app_new/pt_projectView.js").exists());
        assertEquals(Md5Utils.calculateMd5("/Users/netease/webcacheunittestfiles/tmpoutdir/web_app_new/pt_projectView.js"),
                Md5Utils.calculateMd5("/Users/netease/webcacheunittestfiles/web_app_new_full/pt_projectView.js"));

        assertTrue(new File("/Users/netease/webcacheunittestfiles/tmpoutdir/web_app_new/tp_module_dashboard_index.js").exists());
        assertEquals(Md5Utils.calculateMd5("/Users/netease/webcacheunittestfiles/tmpoutdir/web_app_new/tp_module_dashboard_index.js"),
                Md5Utils.calculateMd5("/Users/netease/webcacheunittestfiles/web_app_new_full/tp_module_dashboard_index.js"));

        assertTrue(new File("/Users/netease/webcacheunittestfiles/tmpoutdir/web_app_new/tp_module_frames_head_index.js").exists());
        assertEquals(Md5Utils.calculateMd5("/Users/netease/webcacheunittestfiles/tmpoutdir/web_app_new/tp_module_frames_head_index.js"),
                Md5Utils.calculateMd5("/Users/netease/webcacheunittestfiles/web_app_new_full/tp_module_frames_head_index.js"));

        assertTrue(new File("/Users/netease/webcacheunittestfiles/tmpoutdir/web_app_new/tp_module_frames_layout_index.js").exists());
        assertEquals(Md5Utils.calculateMd5("/Users/netease/webcacheunittestfiles/tmpoutdir/web_app_new/tp_module_frames_layout_index.js"),
                Md5Utils.calculateMd5("/Users/netease/webcacheunittestfiles/web_app_new_full/tp_module_frames_layout_index.js"));

        assertTrue(new File("/Users/netease/webcacheunittestfiles/tmpoutdir/web_app_new/tp_module_migration_index.js").exists());
        assertEquals(Md5Utils.calculateMd5("/Users/netease/webcacheunittestfiles/tmpoutdir/web_app_new/tp_module_migration_index.js"),
                Md5Utils.calculateMd5("/Users/netease/webcacheunittestfiles/web_app_new_full/tp_module_migration_index.js"));

        assertTrue(FileUtils.deleteDir(tmpoutdir));
        assertFalse(tmpoutdir.exists());

        fileMd5List.clear();
        assertFalse(FileUtils.unpackPackage(tmpoutdir, filePath, fileMd5List));
    }

    @Test
    public void testUnpackPackage2() throws Exception {
        File tmpoutdir = new File("/Users/netease/webcacheunittestfiles/", "tmpoutdir");
        assertTrue(FileUtils.deleteDir(tmpoutdir));
        assertTrue(tmpoutdir.mkdirs());

        Map<String, String> fileMd5List = new HashMap<String, String>();
        String filePath = "/Users/netease/webcacheunittestfiles/web_app_new.zip";
        assertTrue(FileUtils.unpackPackage(tmpoutdir, filePath, fileMd5List));

        assertTrue(new File("/Users/netease/webcacheunittestfiles/tmpoutdir/web_app_new/core.css").exists());
        assertEquals(Md5Utils.calculateMd5("/Users/netease/webcacheunittestfiles/tmpoutdir/web_app_new/core.css"),
                Md5Utils.calculateMd5("/Users/netease/webcacheunittestfiles/web_app_new_full/core.css"));

        assertTrue(new File("/Users/netease/webcacheunittestfiles/tmpoutdir/web_app_new/core.js").exists());
        assertEquals(Md5Utils.calculateMd5("/Users/netease/webcacheunittestfiles/tmpoutdir/web_app_new/core.js"),
                Md5Utils.calculateMd5("/Users/netease/webcacheunittestfiles/web_app_new_full/core.js"));

        assertTrue(new File("/Users/netease/webcacheunittestfiles/tmpoutdir/web_app_new/pt_main.js").exists());
        assertEquals(Md5Utils.calculateMd5("/Users/netease/webcacheunittestfiles/tmpoutdir/web_app_new/pt_main.js"),
                Md5Utils.calculateMd5("/Users/netease/webcacheunittestfiles/web_app_new_full/pt_main.js"));

        assertTrue(new File("/Users/netease/webcacheunittestfiles/tmpoutdir/web_app_new/pt_projectView.js").exists());
        assertEquals(Md5Utils.calculateMd5("/Users/netease/webcacheunittestfiles/tmpoutdir/web_app_new/pt_projectView.js"),
                Md5Utils.calculateMd5("/Users/netease/webcacheunittestfiles/web_app_new_full/pt_projectView.js"));

        assertTrue(new File("/Users/netease/webcacheunittestfiles/tmpoutdir/web_app_new/tp_module_dashboard_index.js").exists());
        assertEquals(Md5Utils.calculateMd5("/Users/netease/webcacheunittestfiles/tmpoutdir/web_app_new/tp_module_dashboard_index.js"),
                Md5Utils.calculateMd5("/Users/netease/webcacheunittestfiles/web_app_new_full/tp_module_dashboard_index.js"));

        assertTrue(new File("/Users/netease/webcacheunittestfiles/tmpoutdir/web_app_new/tp_module_frames_head_index.js").exists());
        assertEquals(Md5Utils.calculateMd5("/Users/netease/webcacheunittestfiles/tmpoutdir/web_app_new/tp_module_frames_head_index.js"),
                Md5Utils.calculateMd5("/Users/netease/webcacheunittestfiles/web_app_new_full/tp_module_frames_head_index.js"));

        assertTrue(new File("/Users/netease/webcacheunittestfiles/tmpoutdir/web_app_new/tp_module_frames_layout_index.js").exists());
        assertEquals(Md5Utils.calculateMd5("/Users/netease/webcacheunittestfiles/tmpoutdir/web_app_new/tp_module_frames_layout_index.js"),
                Md5Utils.calculateMd5("/Users/netease/webcacheunittestfiles/web_app_new_full/tp_module_frames_layout_index.js"));

        assertTrue(new File("/Users/netease/webcacheunittestfiles/tmpoutdir/web_app_new/tp_module_migration_index.js").exists());
        assertEquals(Md5Utils.calculateMd5("/Users/netease/webcacheunittestfiles/tmpoutdir/web_app_new/tp_module_migration_index.js"),
                Md5Utils.calculateMd5("/Users/netease/webcacheunittestfiles/web_app_new_full/tp_module_migration_index.js"));

        assertTrue(FileUtils.deleteDir(tmpoutdir));
        assertFalse(tmpoutdir.exists());

        fileMd5List.clear();
        assertFalse(FileUtils.unpackPackage(tmpoutdir, filePath, fileMd5List));

        filePath = "/Users/netease/webcacheunittestfiles/web_app_zip.diff";
        assertTrue(tmpoutdir.mkdirs());
        fileMd5List.clear();
        assertFalse(FileUtils.unpackPackage(tmpoutdir, filePath, fileMd5List));

        assertTrue(FileUtils.deleteDir(tmpoutdir));
        assertFalse(tmpoutdir.exists());
    }

    @Test
    public void testDeleteDir() throws Exception {
        File dir = new File("/Users/netease/webcacheunittestfiles/web_app_new");
        if (!dir.exists()) {
            String shpath = "/Users/netease/webcacheunittestfiles/unzip.sh";
            Process ps = Runtime.getRuntime().exec(shpath);
            ps.waitFor();
        }

        assertTrue(dir.exists());
        assertTrue(dir.isDirectory());

        assertTrue(FileUtils.deleteDir("/Users/netease/webcacheunittestfiles/web_app_new"));
        dir = new File("/Users/netease/webcacheunittestfiles/web_app_new");
        assertTrue(!dir.exists());
    }
}