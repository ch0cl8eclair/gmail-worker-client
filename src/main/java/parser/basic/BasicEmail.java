package parser.basic;

import parser.CSVRecord;

public record BasicEmail(long emailDate, String from, String to, String subject) implements CSVRecord {

    public String toCsv() {
        StringBuilder sb = new StringBuilder(formatDate());
        sb.append(CSVRecord.DELIMITER);
        sb.append(from);
        sb.append(CSVRecord.DELIMITER);
        sb.append(to);
        sb.append(CSVRecord.DELIMITER);
        sb.append(subject);
        return sb.toString();
    }

    @Override
    public long getLongDate() {
        return emailDate();
    }
}
