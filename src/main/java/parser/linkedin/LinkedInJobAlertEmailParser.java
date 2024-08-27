package parser.linkedin;

import com.google.api.services.gmail.model.Message;
import utils.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import parser.MessageParser;
import utils.Util;

import java.io.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LinkedInJobAlertEmailParser implements MessageParser {
    private static final Logger logger = LoggerFactory.getLogger(LinkedInJobAlertEmailParser.class.getName());

    enum messageParsingStates {SUMMARY, RECORDS, COMPLETED}

    private static final String INITIAL_ALERT_LINE = "^Your job alert for .*";
    private static final String INITIAL_SUMMARY_LINE = "^[0-9]+[+]* new jobs match your preferences.*";
    private static final String FINAL_SUMMARY_LINE = "^See all jobs on LinkedIn: .*";
    private static final String RECORD_LINK_LINE = "^View job:\\s+(.*)";
    private static final Pattern RECORD_LINK_LINE_PATTERN = Pattern.compile(RECORD_LINK_LINE);

    private String messageOutputFileName;
    private PrintWriter messageOutputHandle;

    public LinkedInJobAlertEmailParser() {
        Configuration parserConfiguration = Configuration.getInstance();
        if (parserConfiguration.outputMessagesToFile()) {
            openOpenFile();
        }
    }

    // --------------------------------------------------------------
    // Output file methods
    // --------------------------------------------------------------
    private void openOpenFile() {
        LocalDate localDate = LocalDate.now();
        String dateSuffix = localDate.format(DateTimeFormatter.ofPattern("ddMMyy"));
        this.messageOutputFileName = "messages-" + dateSuffix + ".txt";
        try {
            messageOutputHandle = new PrintWriter(messageOutputFileName);
        } catch (FileNotFoundException e) {
            messageOutputHandle = null;
        }
    }

    private void writeoutMessageToFile(String msgTxt) {
        if (this.messageOutputHandle != null) {
            this.messageOutputHandle.println(msgTxt);
            this.messageOutputHandle.println("----------------------------------------\n");
        }
    }

    // --------------------------------------------------------------
    // Parsing methods
    // --------------------------------------------------------------
    boolean isEmptyLine(String line) {
        return line.matches("^\\s*$");
    }

    boolean isHyphenSeparatorLine(String line) {
        return line.matches("^\\s*\\-+");
    }

    private void parseRecord(Long internalDate, String title, BufferedReader br, List<LinkedInAlert> parsedAlerts) throws IOException {
        logger.debug("--- Parsing email alert record start");
        List<String> jobLines = new ArrayList<>();
        title = title.replace(',', '-');
        jobLines.add(title);
        boolean linkParsed = false;
        String link = null;
        do {
            String additional = br.readLine();
            if (additional != null) {
                // Avoid any comma related issues for when we export to csv
                jobLines.add(additional.replace(',', '-'));
                Matcher matcher = RECORD_LINK_LINE_PATTERN.matcher(additional);
                if (matcher.find()) {
                    link = matcher.group(1);
                    linkParsed = true;
                }
            }
            else {
                break;
            }
        } while (!linkParsed);

        if (!linkParsed) {
            logger.error("Failed to parse link line for job: {}", title);
        }

        // Empty line
        try {
            String emptyLine = br.readLine();
            if (!isEmptyLine(emptyLine)) {
                logger.error("--- Failed to find empty line!");
            }
        } catch (IOException io) {
            io.printStackTrace();
        }

        // hyphen line
        try {
            String hyphenLine = br.readLine();
            if (!isHyphenSeparatorLine(hyphenLine)) {
                logger.error("--- Failed to find hyphen line!");
            }
        } catch (IOException io) {
            io.printStackTrace();
        }

        LinkedInAlert linkedInAlert = null;
        // get company, but may be job line continuation
        String company = jobLines.get(1);

        int locationOffset = 0;
        if (jobLines.size() == 7) {
            title = title + " - " + company;
            locationOffset += 1;
            company = jobLines.get(1 + locationOffset);
        }
        // get location
        if (company.startsWith("£")) {
            title = title + company;
            locationOffset += 1;
            company = jobLines.get(1 + locationOffset);
        }
        String location = jobLines.get(2 + locationOffset);

        // Parse comments
        StringBuilder additionalComments = new StringBuilder();
        for (int i = 3 + locationOffset; i < jobLines.size() -1; i++) {
            if (!additionalComments.isEmpty()) {
                additionalComments.append(". ");
            }
            additionalComments.append(jobLines.get(i));
        }

        linkedInAlert = new LinkedInAlert(internalDate, title, company, location, additionalComments.toString(), chompLink(link));
        parsedAlerts.add(linkedInAlert);

        logger.debug("--- Parsing email record end");
    }

    String chompLink(String givenLink) {
        if (givenLink != null && givenLink.length() > 0) {
            int endPos = givenLink.indexOf("/?trackingId");
            if (endPos != -1)
                return givenLink.substring(0, endPos);
        }
        return givenLink;
    }

    public List<LinkedInAlert> parse(Message msg) {
        String msgTxt = Util.decodeMessageContent(msg, Util.EMAIL_MIME_TYPE.TEXT);
        writeoutMessageToFile(msgTxt);
        Long internalDate = msg.getInternalDate();
        return this.parse(internalDate, msgTxt);
    }

    @Override
    public void cleanup() {
        if (this.messageOutputHandle != null)
            this.messageOutputHandle.close();
    }

    @Override
    public String getCSVOutputFilename() {
        LocalDate localDate = LocalDate.now();
        String dateSuffix = localDate.format(DateTimeFormatter.ofPattern("ddMMyy"));
        return String.format("linkedInAlerts-%s.csv", dateSuffix);
    }

    public List<LinkedInAlert> parse(Long internalDate, String msgTxt) {
        Reader stringReader = new StringReader(msgTxt);
        return this.parse(internalDate, new BufferedReader(stringReader));
    }

    public List<LinkedInAlert> parse(Long internalDate, BufferedReader br) {
        logger.debug("Parsing email message start");
        messageParsingStates state = messageParsingStates.SUMMARY;
        List<LinkedInAlert> parsedAlerts = new ArrayList<>();
        String sCurrentLine;
        try {
            while ((sCurrentLine = br.readLine()) != null) {
            logger.debug("Parsing [{}]", sCurrentLine);
                if (isEmptyLine(sCurrentLine)) {
                    continue;
                }

                if (sCurrentLine.startsWith("See all jobs")) {
                    logger.debug("--- All email alerts in message parsed.");
                }
                if (sCurrentLine.matches(FINAL_SUMMARY_LINE)) {
                    state = messageParsingStates.COMPLETED;
                    break;
                }
                switch (state) {
                    case SUMMARY:
                        if (sCurrentLine.matches(INITIAL_ALERT_LINE))
                            continue;
                        else if (sCurrentLine.matches(INITIAL_SUMMARY_LINE))
                            state = messageParsingStates.RECORDS;
                        continue;

                    case RECORDS:
                        parseRecord(internalDate, sCurrentLine, br, parsedAlerts);
                        break;
                }
            }
        }
        catch(IOException e) {
            e.printStackTrace();
        }
        logger.debug("Parsing email message completed, parsed {} alerts\n", parsedAlerts.size());
        return parsedAlerts;
    }
}
