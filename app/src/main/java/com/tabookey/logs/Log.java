package com.tabookey.logs;

import android.os.Process;

import com.tabookey.bizpoc.BuildConfig;
import com.tabookey.bizpoc.api.Global;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;


/**
 * wrapper for android log, to save all logs to a file
 */
public class Log {

    public static String levels = "??VDIWEA";
    public static final int VERBOSE = 2;
    public static final int DEBUG = 3;
    public static final int INFO = 4;
    public static final int WARN = 5;
    public static final int ERROR = 6;
    public static final int ASSERT = 7;

    private static File logFolder;
    static PrintStream logfile;
    private static PrintStream debugLogFile;

    //logcat-style date
    static SimpleDateFormat dateFormat = new SimpleDateFormat("MM-dd HH:mm:ss.SSS");
    private static boolean dumpToLogcat;
    private static Thread.UncaughtExceptionHandler flushLogsHandler;

    static String timeStr(Date date) {
        return dateFormat.format(date);
    }

    static long maxAgeToRemoveMs = 12 * 3600 * 1000;

    private static void removeOldLogs() {
        long oldestToKeep = System.currentTimeMillis() - maxAgeToRemoveMs;
        for (File f : logFolder.listFiles(f -> f.lastModified() < oldestToKeep)) {

            Log.d("logs", "removing old log file " + f);
            f.delete();
        }
    }

    /**
     * re-initialize the logs, so all current logs are flushed.
     * important before sending logs (and when app crashes), since our logs are compressed
     * and encrypted, both require keeping latest logs in memory
     */
    public static void restartLogs() {
        initLogger(logFolder.getAbsolutePath(), dumpToLogcat);
    }


    /**
     * create a ZIP of log files to send.
     * NOTE: first, it close and re-open the log, so last log data is written.
     *
     * @param appinfo   - application info, to write as "appinfo.txt" zip entry.
     * @param maxAgeSec - max age of log to send.
     * @return a zip containing all latest logs.
     */
    public static File getZipLogsToSend(String appinfo, long maxAgeSec) {
        try {
            //re-initialize the log, to flush all logs.
            restartLogs();
            long fromTime = System.currentTimeMillis() - maxAgeSec * 1000;
            //return all logs, except "log.log" (or log.log.txt), since it was just created, and contains nothing.

            File zipfile = new File(logFolder.getParentFile(), "logs.zip");
            ZipOutputStream ziplog = new ZipOutputStream(new FileOutputStream(zipfile));
            if (appinfo != null) {
                ziplog.putNextEntry(new ZipEntry("appinfo.txt"));
                ziplog.write(appinfo.getBytes());
                ziplog.closeEntry();
            }

            File[] logs = logFolder.listFiles(f -> f.lastModified() > fromTime && !f.getName().startsWith("log.log"));
            Arrays.sort(logs, (a, b) -> a.getName().compareTo(b.getName()));
            for (File f : logs) {
                try (InputStream in = new FileInputStream(f)) {
                    ziplog.putNextEntry(new ZipEntry(f.getName()));
                    IOUtils.copy(in, ziplog);
                }
            }
            ziplog.close();
            return zipfile;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    interface OneLiner {
        String getLine();
    }

    static void append(StringBuilder b, String tag, Object line) {
        if (line != null)
            b.append(tag + ": " + line + "\n");
    }

    static void append(StringBuilder b, String tag, OneLiner line) {
        try {
            String l = line.getLine();
            append(b, tag, l);
        } catch (Exception ignored) {
        }
    }

    public static String getAppInfo() {
        StringBuilder info = new StringBuilder("Application info:");

        append(info, "pkg", BuildConfig.APPLICATION_ID);

        append(info, "version", BuildConfig.VERSION_NAME);
        append(info, "version-code", BuildConfig.VERSION_CODE);

        append(info, "name", () -> Global.ent.getMe().name);
        return info.toString();
    }

    /**
     * initialize log. current file is always called log.log
     * previous file is renamed based on its creation time to log-{date}.log
     * <p>
     * NOTE: in debug mode, unencrypted, ".txt" files are also created.
     *
     * @param folder    folder to put files in
     * @param debugLogs true in debug mode: create log.log.txt in clear text, and also dump everything to real logcat
     * @throws IOException
     */
    public static void initLogger(String folder, boolean debugLogs) {

        if ( flushLogsHandler==null ) {
            flushLogsHandler = new Thread.UncaughtExceptionHandler() {
                Thread.UncaughtExceptionHandler prevHandler = Thread.getDefaultUncaughtExceptionHandler();
                @Override
                public void uncaughtException(Thread t, Throwable e) {

                    //flush all current logs to file, before letting app to die...
                    try {
                        restartLogs();
                    } catch (Throwable ignores) {}
                    prevHandler.uncaughtException(t,e);
                }
            };
            Thread.setDefaultUncaughtExceptionHandler(flushLogsHandler);
        }

        if (logfile != null) {
            try {
                logfile.close();
                if (debugLogFile != null)
                    debugLogFile.close();
            } catch (Exception ignored) {
            }
            logfile = null;
            debugLogFile = null;
        }

        if (folder == null)
            return; //only closing logs

        logFolder = new File(folder);
        logFolder.mkdirs();

        dumpToLogcat = debugLogs;

        File logfileName = new File(logFolder, "log.log");
        if (logfileName.exists()) {
            //rename old logfile:

            String date = new SimpleDateFormat("MMdd-HHmmss").format(new Date(logfileName.lastModified()));
            logfileName.renameTo(new File(folder, "log-" + date + ".log"));
            if (debugLogs) {
                new File(logfileName + ".txt").renameTo(new File("log-" + date + ".log.txt"));
            }
        }

        try {
            logfile = EncStream.createEncLog(logfileName.getAbsolutePath());
            if (debugLogs)
                debugLogFile = new PrintStream(new FileOutputStream(logfileName + ".txt"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        removeOldLogs();
        //TODO: test can't call this, because its not mocked..
        android.util.Log.d("logs", "=== Initialized log to " + logfileName + " debug=" + debugLogs);
    }

    public static int println(int priority, String tag, String msg, Throwable tr) {
        if (!isLoggable(tag, priority))
            return 0;

        msg = msg.replaceAll("\\bv2x\\S+\\b", "v2x...");

        if (dumpToLogcat) {
            android.util.Log.println(priority, tag, "*" + msg);
            if (tr != null)
                android.util.Log.println(priority, tag, android.util.Log.getStackTraceString(tr));
        }

        String now = timeStr(new Date());
        char prioChar = priority < 0 || priority >= levels.length() ? '-' : levels.charAt(priority);
        dumpToLog(now, prioChar, tag, msg);
        if (tr != null) {
            StringWriter w = new StringWriter();
            tr.printStackTrace(new PrintWriter(w));
            for (String s : w.toString().split("\n")) {
                dumpToLog(now, prioChar, tag, s);
            }
        }
        return 1;
    }

    //helper, to make sure the stream is not nullified between check and usage..
    //(better to lose 1-2 log lines than crash)
    private static void ifNotNull(PrintStream log, Consumer<PrintStream> func) {
        if (log != null)
            func.accept(log);
    }

    private static void dumpToLog(String now, char prioChar, String tag, String msg) {
        long tid = Process.myTid();
        long pid = Process.myPid();
        ifNotNull(logfile, f -> {
            f.printf("%s %5d %5d %c %-8s: %s\n", now, pid, tid, prioChar, tag, msg);
        });
        ifNotNull(debugLogFile, f -> {
            f.printf("%s %5d %5d %c %-8s: %s\n", now, pid, tid, prioChar, tag, msg);
        });
    }

    public static int v(String tag, String msg) {
        return println(VERBOSE, tag, msg, null);
    }

    public static int v(String tag, String msg, Throwable tr) {
        return println(VERBOSE, tag, msg, tr);
    }

    public static int d(String tag, String msg) {
        return println(DEBUG, tag, msg, null);
    }

    public static int d(String tag, String msg, Throwable tr) {
        return println(DEBUG, tag, msg, tr);
    }

    public static int i(String tag, String msg) {
        return println(INFO, tag, msg, null);
    }

    public static int i(String tag, String msg, Throwable tr) {
        return println(INFO, tag, msg, tr);
    }

    public static int w(String tag, String msg) {
        return println(WARN, tag, msg, null);
    }

    public static int w(String tag, String msg, Throwable tr) {
        return println(WARN, tag, msg, tr);
    }

    public static boolean isLoggable(String tag, int level) {
        return true;
    }

    public static int w(String tag, Throwable tr) {
        return println(WARN, tag, "", null);
    }

    public static int e(String tag, String msg) {
        return println(ERROR, tag, msg, null);
    }

    public static int e(String tag, String msg, Throwable tr) {
        return println(ERROR, tag, msg, tr);
    }

}
