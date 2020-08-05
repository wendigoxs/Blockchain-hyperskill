package org.hyperskill.blockchain.utils;

import java.io.PrintStream;

public class Logger implements SoutLogging {

    private static final boolean isLogDebug = true;

    private static final Logger DEV_NULL_LOGGER = new Logger(new PrintStream(PrintStream.nullOutputStream()));
    private static final Logger SOUT_LOGGER = new Logger(System.out);

    private final PrintStream printStream;

    public Logger(PrintStream printStream) {
        this.printStream = printStream;
    }

    public PrintStream getPrintStream() {
        return printStream;
    }

    public enum LogType {
        NONE,
        ALL
    }

    public static SoutLogging getInstance(LogType logType) {
        switch (logType) {
            case ALL:
                return SOUT_LOGGER;
            default:
                return DEV_NULL_LOGGER;
        }
    }

    public static SoutLogging getDebugLogger() {
        return isLogDebug ? getInstance(LogType.ALL) : getInstance(LogType.NONE);
    }

    @Override
    public void printf(String format, Object... args) {
        printStream.printf(format, args);
    }

    @Override
    public void println(Object o) {
        printStream.println(o);
    }

}
