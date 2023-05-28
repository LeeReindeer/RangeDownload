package moe.leer.rangedownload.util;

import java.io.PrintStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@SuppressWarnings("rawtypes")
public final class Logger {

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static boolean isDebug = false;

    private final Class clazz;

    public Logger(Class clazz) {
        this.clazz = clazz;
    }

    public Logger(Class clazz, boolean debug) {
        this.clazz = clazz;
        isDebug = debug;
    }

    public static Logger getLogger(Class clazz) {
        return new Logger(clazz);
    }

    public void info(String msg, Object... args) {
        print(msg, "INFO", args);
    }

    public void error(String msg, Object... args) {
        print(msg, "ERROR", args);
    }

    public void debug(String msg, Object... args) {
        if (isDebug) {
            print(msg, "DEBUG", args);
            System.out.println(msg);
        }
    }

    private void print(String msg, String level, Object... args) {
        String prefix = LocalDateTime.now().format(formatter)
                + " [" + level + "]"
                + "[" + Thread.currentThread().getName() + "]"
                + "[" + clazz.getName() + "]" + ":";
        PrintStream out = System.out;
        if ("ERROR".equals(level)) {
            out = System.err;
        }
        if (args != null && args.length != 0) {
            out.println(prefix + String.format(msg, args));
        } else {
            out.println(prefix + msg);
        }
    }
}
