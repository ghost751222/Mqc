package com.macaron.mqcsdk;

import java.sql.Timestamp;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Date;

public class LocalDateTimeUtils {
    private static String defaultPattern = "yyyy-MM-dd HH:mm:ss";

    public LocalDateTimeUtils() {
    }

    public static String string(LocalDateTime dateTime) {
        return dateTime.format(DateTimeFormatter.ofPattern(defaultPattern));
    }

    public static String string(LocalDateTime dateTime, String pattern) {
        return dateTime.format(DateTimeFormatter.ofPattern(pattern));
    }

    public static Timestamp timestamp(LocalDateTime dateTime) {
        return Timestamp.valueOf(dateTime);
    }

    public static LocalDateTime localDateTime(Timestamp timestamp) {
        return timestamp.toLocalDateTime();
    }

    public static LocalDate localDate(Timestamp timestamp) {
        return localDateTime(timestamp).toLocalDate();
    }

    public static LocalTime localTime(Timestamp timestamp) {
        return localDateTime(timestamp).toLocalTime();
    }

    public static long toLong(LocalDateTime dateTime) {
        return timestamp(dateTime).getTime();
    }

    public static Instant instant(LocalDateTime dateTime) {
        return dateTime.atZone(ZoneId.systemDefault()).toInstant();
    }

    public static Date date(LocalDateTime dateTime) {
        return Date.from(instant(dateTime));
    }

    public static LocalDateTime fromString(String text, String pattern) {
        return LocalDateTime.parse(text, DateTimeFormatter.ofPattern(pattern));
    }

    public static LocalDateTime fromLong(long timestamp) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault());
    }

    public static LocalDateTime fromDate(Date date) {
        return LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
    }

    public static String timeLength(long value) {
        int h = (int)(value / 3600L);
        int h_mod = (int)(value % 3600L);
        int m = h_mod / 60;
        int s = h_mod % 60;
        return String.format("%02d:%02d:%02d", h, m, s);
    }

    public static String getDefaultPattern() {
        return defaultPattern;
    }

    public static void setDefaultPattern(final String defaultPattern) {
        LocalDateTimeUtils.defaultPattern = defaultPattern;
    }
}
