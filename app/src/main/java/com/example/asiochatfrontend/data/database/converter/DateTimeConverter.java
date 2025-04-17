package com.example.asiochatfrontend.data.database.converter;

import androidx.room.TypeConverter;

import java.util.Date;
import java.util.TimeZone;

/**
 * Room converter for Date objects that preserves the timezone information
 */
public class DateTimeConverter {
    // Store timezone ID with each timestamp
    private static final String TIMEZONE_SEPARATOR = ":TZ:";
    private static final TimeZone DEFAULT_TIMEZONE = TimeZone.getTimeZone("Asia/Jerusalem"); // UTC+3

    @TypeConverter
    public static String fromDateToString(Date date) {
        if (date == null) return null;

        // Get the current timezone ID
        String timezoneId = TimeZone.getDefault().getID();

        // Store both the timestamp and timezone ID
        return date.getTime() + TIMEZONE_SEPARATOR + timezoneId;
    }

    @TypeConverter
    public static Date fromStringToDate(String value) {
        if (value == null) return null;

        try {
            // Check if the stored value has timezone information
            if (value.contains(TIMEZONE_SEPARATOR)) {
                // Split the timestamp and timezone ID
                String[] parts = value.split(TIMEZONE_SEPARATOR);
                long timestamp = Long.parseLong(parts[0]);
                String timezoneId = parts[1];

                // Create a date using the stored timestamp
                Date date = new Date(timestamp);

                // No need to adjust since timestamp is absolute
                return date;
            } else {
                // Legacy data without timezone info - assume it's a raw timestamp
                // and was created in Israel timezone (UTC+3)
                long timestamp = Long.parseLong(value);
                return new Date(timestamp);
            }
        } catch (Exception e) {
            // Fallback for any parsing errors
            try {
                return new Date(Long.parseLong(value));
            } catch (NumberFormatException ex) {
                return null;
            }
        }
    }

    // For compatibility with existing code that expects Long converters
    @TypeConverter
    public static Date fromTimestamp(Long value) {
        return value == null ? null : new Date(value);
    }

    @TypeConverter
    public static Long dateToTimestamp(Date date) {
        return date == null ? null : date.getTime();
    }
}