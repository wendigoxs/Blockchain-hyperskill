package org.hyperskill.blockchain.utils;

import java.io.PrintStream;

public interface SoutLogging {
    void printf(String format, Object... args);

    void println(Object o);

    PrintStream getPrintStream();
}
