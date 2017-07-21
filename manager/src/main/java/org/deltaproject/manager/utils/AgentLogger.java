package org.deltaproject.manager.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

/**
 * Created by jinwookim on 2017. 2. 16..
 */

public class AgentLogger {
    public final static String LOG_PATH = System.getenv("DELTA_ROOT") + "/log/";
    public final static String APP_AGENT = "app.log";
    public final static String CHANNEL_AGENT = "channel.log";
    public final static String HOST_AGENT = "host.log";

    public static ArrayList<Thread> loggerList = new ArrayList();
    public static String temp = "";

    public static LoggerThread getLoggerThreadInstance(Process proc, String name) {
        return new LoggerThread(proc, name);
    }

    public static RunWhenShuttingDown getShutdownInstance() {
        return new RunWhenShuttingDown();
    }

    public static void addLoggerThread(Thread loggerThread) {
        loggerList.add(loggerThread);
    }

    public static void stopAllLogger() {
        loggerList.forEach(loggerThread -> loggerThread.interrupt());
    }

    public static class LoggerThread implements Runnable {
        private String name;
        private BufferedReader stderrBr;
        private FileWriter output;

        LoggerThread(Process proc, String name) {
            this.stderrBr = new BufferedReader(new InputStreamReader(proc.getInputStream()));
            this.name = name;
        }

        public void run() {
            try {
                output = new FileWriter(LOG_PATH + name, true);
                checkLogDirectory();
                String line;
                output.write("=================================================================================\n");
                while ((line = stderrBr.readLine()) != null) {
                    temp += line;
                    output.write(line + "\n");
                    output.flush();
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    output.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    public static class RunWhenShuttingDown extends Thread {
        public void run() {
            System.out.println("Control-C caught. Removing all log files...");
            File f = new File(LOG_PATH + APP_AGENT);
            f.delete();
            f = new File(LOG_PATH + CHANNEL_AGENT);
            f.delete();
            f = new File(LOG_PATH + HOST_AGENT);
            f.delete();
        }
    }

    public static String readLogFile(String name) {
        String result = "";
        String path = LOG_PATH + name;
        // file check
        File file = new File(path);
        while (!file.exists()) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        try {
            result = new String(Files.readAllBytes(Paths.get(path)));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    public static void checkLogDirectory() {
        File dir = new File(LOG_PATH);

        if (!dir.exists()) {
            dir.mkdirs();
            System.out.println("Directory was created!");
        }
    }

    public static String getTemp() {
        return temp;
    }

    public static void setTemp(String temp) {
        AgentLogger.temp = temp;
    }
}
