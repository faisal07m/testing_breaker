package de.upb.bionicbeaver.bank.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

/**
 * @author Siddhartha Moitra
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ThreadPoolProvider implements Supplier<ExecutorService> {

    private static final ThreadPoolProvider INSTANCE = new ThreadPoolProvider();

    public static ThreadPoolProvider getInstance() {
        return INSTANCE;
    }

    private final ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    @Override
    public ExecutorService get() {
        return this.executorService;
    }
}
