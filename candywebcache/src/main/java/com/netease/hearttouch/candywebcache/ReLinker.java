package com.netease.hearttouch.candywebcache;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.text.TextUtils;
import android.util.Log;

import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
/**
 * Created by chenjava on 2015/12/17.
 */

/**
 * ReLinker is a small library to help alleviate {@link UnsatisfiedLinkError} exceptions thrown due
 * to Android's inability to properly install / load native libraries for Android versions before
 * API 21
 */
@SuppressWarnings("deprecation")
public class ReLinker {
    private static final String LIB_DIR = "lib";
    private static final int MAX_TRIES = 5;
    private static final int COPY_BUFFER_SIZE = 4096;

    private ReLinker() {
        // No instances
    }

    /**
     * Utilizes the regular system call to attempt to load a native library. If a failure occurs,
     * then the function extracts native .so library out of the app's APK and attempts to load it.
     * <p>
     *     <strong>Note: This is a synchronous operation</strong>
     */
    public static boolean loadLibrary(final Context context, final String library) {
        if (context == null || TextUtils.isEmpty(library)) {
            return false;
        }

        try {
            System.loadLibrary(library);
            return true;
        } catch (final UnsatisfiedLinkError ignored) {
        }

        final File workaroundFile = getWorkaroundLibFile(context, library);
        if (!workaroundFile.exists()) {
            unpackLibrary(context, library, workaroundFile);
        }
        try {
            System.load(workaroundFile.getAbsolutePath());
            return true;
        } catch (final UnsatisfiedLinkError ignored) {
        }
        return false;
    }

    /**
     * @param context {@link Context} to describe the location of it's private directories
     * @return A {@link File} locating the directory that can store extracted libraries
     * for later use
     */
    private static File getAppWorkaroundLibDir(final Context context) {
        return context.getDir(LIB_DIR, Context.MODE_PRIVATE);
    }

    private static File getExternalWorkaroundLibDir(final Context context) {
        File externalStorageDir = Environment.getExternalStorageDirectory();
        File externalAppDir = new File(externalStorageDir, context.getPackageName());
        File externalAppLibDir = new File(externalAppDir, "libs");
        if (!externalAppLibDir.exists()) {
            externalAppLibDir.mkdirs();
        }
        return externalAppLibDir;
    }

    private static File getApkFile(final Context context) {
        final ApplicationInfo appInfo = context.getApplicationInfo();
        return new File(appInfo.sourceDir);
    }

    /**
     * @param context {@link Context} to retrieve the workaround directory from
     * @param library The name of the library to load
     * @return A {@link File} locating the workaround library file to load
     */
    private static File getWorkaroundLibFile(final Context context, final String library) {
        final String libName = System.mapLibraryName(library);
        File appWorkaroundLibDir = getAppWorkaroundLibDir(context);
        File libFile = new File(appWorkaroundLibDir, libName);
        if (libFile.exists()) {
            return libFile;
        }
        StatFs dataFs = new StatFs(appWorkaroundLibDir.getAbsolutePath());
        long sizes = (long) dataFs.getFreeBlocks() * (long) dataFs.getBlockSize();

        File apkFile = getApkFile(context);
        if (sizes < apkFile.length()) {
            File externalWorkaroundFile = getExternalWorkaroundLibDir(context);
            libFile = new File(externalWorkaroundFile, libName);
        }
        return libFile;
    }

    /**
     * Attempts to unpack the given library to the workaround directory. Implements retry logic for
     * IO operations to ensure they succeed.
     *
     * @param context {@link Context} to describe the location of the installed APK file
     * @param library The name of the library to load
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    private static void unpackLibrary(final Context context, final String library, final File outputFile) {
        ZipFile zipFile = null;
        try {
            final ApplicationInfo appInfo = context.getApplicationInfo();
            zipFile = new ZipFile(new File(appInfo.sourceDir), ZipFile.OPEN_READ);

            if (zipFile == null) {
                return;
            }

            String jniNameInApk = null;
            ZipEntry libraryEntry = null;

            if (Build.VERSION.SDK_INT >= 21 && Build.SUPPORTED_ABIS.length > 0) {
                for (final String ABI : Build.SUPPORTED_ABIS) {
                    jniNameInApk = "lib/" + ABI + "/" + System.mapLibraryName(library);
                    libraryEntry = zipFile.getEntry(jniNameInApk);

                    if (libraryEntry != null) {
                        break;
                    }
                }
            } else {
                //noinspection deprecation
                jniNameInApk = "lib/" + Build.CPU_ABI + "/" + System.mapLibraryName(library);
                libraryEntry = zipFile.getEntry(jniNameInApk);
            }

            if (libraryEntry == null) {
                // Does not exist in the APK
                if (jniNameInApk != null) {
                    throw new MissingLibraryException(jniNameInApk);
                } else {
                    throw new MissingLibraryException(library);
                }
            }

            outputFile.delete(); // Remove any old file that might exist

            if (outputFile.createNewFile()) {
                InputStream inputStream = null;
                FileOutputStream fileOut = null;
                try {
                    inputStream = zipFile.getInputStream(libraryEntry);
                    fileOut = new FileOutputStream(outputFile);
                    copy(inputStream, fileOut);

                    // Change permission to rwxr-xr-x
                    outputFile.setReadable(true, false);
                    outputFile.setExecutable(true, false);
                    outputFile.setWritable(true);
                } catch (FileNotFoundException e) {
                } catch (IOException e) {
                } finally {
                    closeSilently(inputStream);
                    closeSilently(fileOut);
                }
            }
        } catch (IOException ioe) {

        } finally {
            try {
                if (zipFile != null) {
                    zipFile.close();
                }
            } catch (IOException ignored) {
            }
        }
    }

    /**
     * Copies all data from an {@link InputStream} to an {@link OutputStream}.
     *
     * @param in The stream to read from.
     * @param out The stream to write to.
     * @throws IOException when a stream operation fails.
     */
    private static void copy(InputStream in, OutputStream out) throws IOException {
        byte[] buf = new byte[COPY_BUFFER_SIZE];
        while (true) {
            int read = in.read(buf);
            if (read == -1) {
                break;
            }
            out.write(buf, 0, read);
        }
    }

    /**
     * Closes a {@link Closeable} silently (without throwing or handling any exceptions)
     * @param closeable {@link Closeable} to close
     */
    private static void closeSilently(final Closeable closeable) {
        try {
            if (closeable != null) {
                closeable.close();
            }
        } catch (IOException ignored) {}
    }

    private static class MissingLibraryException extends RuntimeException {
        public MissingLibraryException(final String library) {
            super(library);
        }
    }
}
