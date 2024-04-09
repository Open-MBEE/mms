package org.openmbee.mms.core.config;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class Formats {

    public static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";
    public static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern(DATE_FORMAT).withZone(
        ZoneId.systemDefault());

    private Formats() {
        throw new IllegalStateException("Formats");
    }
}
