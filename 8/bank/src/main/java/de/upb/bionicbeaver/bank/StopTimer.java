package de.upb.bionicbeaver.bank;

import org.apache.commons.lang3.time.StopWatch;

import java.util.concurrent.TimeUnit;

/**
 * @author Siddhartha Moitra
 */
public class StopTimer {

    private static final StopWatch stopwatch = new StopWatch();

    public static void start() {
        stopwatch.start();
    }

    public static void stop() {
        stopwatch.stop();
    }

    public static long getTime() {
        return stopwatch.getTime(TimeUnit.MILLISECONDS);
    }
}
