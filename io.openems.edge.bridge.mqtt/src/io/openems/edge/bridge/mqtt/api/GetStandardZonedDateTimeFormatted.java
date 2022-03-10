package io.openems.edge.bridge.mqtt.api;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public interface GetStandardZonedDateTimeFormatted {
    public static String getStandardZonedDateTimeString(){
        return ZonedDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSxxx"));
    }
}
