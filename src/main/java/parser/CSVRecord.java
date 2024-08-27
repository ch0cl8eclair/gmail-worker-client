package parser;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public interface CSVRecord {
    static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd/MMM/yyyy");
    static final String DELIMITER = ", ";

    default String formatDate() {
        ZoneId zoneId = ZoneId.of("Europe/London");
        LocalDate localDate = Instant.ofEpochMilli(getLongDate()).atZone(zoneId).toLocalDate();
        return localDate.format(dateTimeFormatter);
    }

    String toCsv();

    long getLongDate();
}
