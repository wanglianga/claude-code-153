package com.community.waste.common;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicInteger;

/** 业务单号生成 */
public final class NoGenerator {
    private static final AtomicInteger SEQ = new AtomicInteger(0);
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private NoGenerator() {}

    public static String next(String prefix) {
        int s = SEQ.incrementAndGet() % 1000;
        return prefix + LocalDateTime.now().format(FMT) + String.format("%03d", s);
    }
}
