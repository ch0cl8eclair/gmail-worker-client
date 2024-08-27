package parser.linkedin;

import parser.CSVRecord;

public record LinkedInAlert(long emailDate, String title, String company, String location, String additional, String link) implements CSVRecord {

    public String toCsv() {
        StringBuilder sb = new StringBuilder(formatDate());
        sb.append(CSVRecord.DELIMITER);
        sb.append(title);
        sb.append(CSVRecord.DELIMITER);
        sb.append(company);
        sb.append(CSVRecord.DELIMITER);
        sb.append(location);
        sb.append(CSVRecord.DELIMITER);
        sb.append(additional != null? additional: "");
        sb.append(CSVRecord.DELIMITER);
        sb.append(link);
        return sb.toString();
    }

    @Override
    public long getLongDate() {
        return emailDate();
    }
}
