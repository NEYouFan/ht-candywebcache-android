package com.netease.hearttouch.candywebcache.cachemanager;

//import com.ice.tar.TarEntry;
//import com.ice.tar.TarInputStream;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedList;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Created by netease on 16/6/14.
 */
public class FileUtils {
    private enum CompressMode {
        NONE, TAR_GZ, ZIP
    }

    public static String getPrimaryFileName(String filepath) {
        File file = new File(filepath);
        if (file.isDirectory()) {
            return file.getName();
        }
        String filename = file.getName();
        String primaryFileName = filename;
        if (filename.endsWith(".tgz")) {
            primaryFileName = filename.substring(0, filename.indexOf(".tgz"));
        } else if (filename.endsWith(".tar.gz")) {
            primaryFileName = filename.substring(0, filename.indexOf(".tar.gz"));
        } else if (filename.endsWith(".zip")) {
            primaryFileName = filename.substring(0, filename.indexOf(".zip"));
        } else if (filename.lastIndexOf('.') > 0) {
            primaryFileName = filename.substring(0, filename.lastIndexOf('.'));
        }
        return primaryFileName;
    }

    public static String getExtName(String filepath) {
        File file = new File(filepath);
        String filename = file.getName();
        if (filename.endsWith(".tar.gz")) {
            return ".tar.gz";
        }
        if (filename.lastIndexOf('.') > 0) {
            return filename.substring(filename.lastIndexOf('.'));
        }
        return "";
    }

    private static boolean unpackTgzPkg(File ourDir, InputStream is, Map<String, String> md5List) {
//        TarInputStream tis = null;
//        try {
//            GZIPInputStream gis = new GZIPInputStream(is);
//            tis = new TarInputStream(gis);
//            TarEntry te;
//            while ((te = tis.getNextEntry()) != null) {
//                String filename = te.getName();
//                if (te.isDirectory()) {
//                    File directory = new File(ourDir, filename);
//                    if (!directory.exists()) {
//                        directory.mkdir();
//
//                    }
//                    if (!directory.exists()) {
//                        return false;
//                    }
//                    continue;
//                } else {
//                    File file = new File(ourDir, filename);
//                    if (file.exists()) {
//                        file.delete();
//                    }
//                    FileOutputStream fos = new FileOutputStream(file);
//                    DigestOutputStream dos = new DigestOutputStream(fos, MessageDigest.getInstance("MD5"));
//                    byte[] buffer = new byte[4096];
//                    int readCount = 0;
//
//                    while ((readCount = tis.read(buffer)) != -1) {
//                        dos.write(buffer, 0, readCount);
//                    }
//
//                    dos.flush();
//                    byte[] digest = dos.getMessageDigest().digest();
//                    String digestStr = Md5Utils.toHexString(digest);
//                    md5List.put(filename, digestStr);
//                    dos.close();
//                }
//            }
//            return true;
//        } catch (IOException e) {
//        } catch (NoSuchAlgorithmException e) {
//        } finally {
//            if (tis != null) {
//                try {
//                    tis.close();
//                } catch (IOException e) {
//                }
//            }
//        }
        return false;
    }

    private static boolean unpackZipPkg(File ourDir, InputStream is, Map<String, String> md5List) {
        ZipInputStream zis = new ZipInputStream(new BufferedInputStream(is));
        try {
            ZipEntry ze;
            while ((ze = zis.getNextEntry()) != null) {
                String filename = ze.getName();
                if (ze.isDirectory()) {
                    File directory = new File(ourDir, filename);
                    if (!directory.exists()) {
                        directory.mkdir();

                    }
                    zis.closeEntry();
                    if (!directory.exists()) {
                        return false;
                    }
                    continue;
                } else {
                    File file = new File(ourDir, filename);
                    File parent = file.getParentFile();
                    if (!parent.exists()) {
                        parent.mkdirs();
                    }
                    if (file.exists()) {
                        file.delete();
                    }
                    FileOutputStream fos = new FileOutputStream(file);
                    DigestOutputStream dos = new DigestOutputStream(fos, MessageDigest.getInstance("MD5"));

                    byte[] buffer = new byte[4096];
                    int readCount = 0;

                    while ((readCount = zis.read(buffer)) != -1) {
                        dos.write(buffer, 0, readCount);
                    }
                    dos.flush();
                    byte[] digest = dos.getMessageDigest().digest();
                    String digestStr = Md5Utils.toHexString(digest);
                    md5List.put(filename, digestStr);
                    dos.close();
                    zis.closeEntry();
                }
            }
            return true;
        } catch (IOException ioe) {
        } catch (NoSuchAlgorithmException e) {
        } finally {
            try {
                zis.close();
            } catch (IOException e) {
            }
        }
        return false;
    }

    private static CompressMode getCompressModeFromFilepath(String filepath) {
        String extName = FileUtils.getExtName(filepath);
        CompressMode mode = CompressMode.NONE;
        if (".tgz".equalsIgnoreCase(extName) || ".tar.gz".equalsIgnoreCase(extName)) {
            mode = CompressMode.TAR_GZ;
        } else if (".zip".equalsIgnoreCase(extName)) {
            mode = CompressMode.ZIP;
        }
        return mode;
    }

    private static boolean unpackPackage(File ourDir, InputStream is, Map<String, String> md5List,
                                         CompressMode mode) {
        if (!ourDir.exists()) {
            return false;
        }

        boolean uncompressResult = false;
        if (mode == CompressMode.ZIP) {
            uncompressResult = FileUtils.unpackZipPkg(ourDir, is, md5List);
        } else if (mode == CompressMode.TAR_GZ) {
            uncompressResult = FileUtils.unpackTgzPkg(ourDir, is, md5List);
        }

        return uncompressResult;
    }

    public static boolean unpackPackage(File ourDir, String filePath, Map<String, String> md5List) {
        CompressMode mode = getCompressModeFromFilepath(filePath);
        if (mode == CompressMode.NONE) {
            return false;
        }

        File pkgFile = new File(filePath);
        try {
            FileInputStream fis = new FileInputStream(pkgFile);
            return unpackPackage(ourDir, fis, md5List, mode);
        } catch (FileNotFoundException e) {
        }
        return false;
    }

    public static String copyFile(InputStream fis, File outFile) {
        FileOutputStream fos = null;
        DigestInputStream dis = null;
        String strDigest = null;
        try {
            if (!outFile.exists()) {
                outFile.createNewFile();
            }
            fos = new FileOutputStream(outFile);
            dis = new DigestInputStream(fis, MessageDigest.getInstance("MD5"));

            byte[] buffer = new byte[4096];
            int numRead = 0;
            while ((numRead = dis.read(buffer)) > 0) {
                fos.write(buffer, 0, numRead);
            }
            byte[] digest = dis.getMessageDigest().digest();
            strDigest = Md5Utils.toHexString(digest);
        } catch (FileNotFoundException e) {
        } catch (IOException ioe) {
        } catch (NoSuchAlgorithmException e) {
        } finally {
            try {
                if (fos != null) {
                    fos.close();
                }
                if (dis != null) {
                    dis.close();
                }
            } catch (IOException e) {
            }
        }
        return strDigest;
    }

    public static boolean deleteDir(File dir) {
        if (!dir.exists()) {
            return true;
        }
        LinkedList<File> deleteCandidatFiles = new LinkedList<>();
        deleteCandidatFiles.addFirst(dir);
        while (!deleteCandidatFiles.isEmpty()) {
            File file = deleteCandidatFiles.remove();
            if (file.isFile()) {
                if (!file.delete()) {
                    System.err.println("Cannot delete file: " + file.getAbsolutePath());
                    return false;
                }
            } else if (file.isDirectory()) {
                File[] subFiles = file.listFiles();
                if (subFiles == null || subFiles.length == 0) {
                    if (!file.delete()) {
                        System.err.println("Cannot delete directory: " + file.getAbsolutePath());
                        return false;
                    }
                } else {
                    deleteCandidatFiles.addFirst(file);
                    for (File subFile : subFiles) {
                        deleteCandidatFiles.addFirst(subFile);
                    }
                }
            }
        }
        return true;
    }

    public static boolean deleteDir(String dirPath) {
        File dir = new File(dirPath);
        return deleteDir(dir);
    }
}
