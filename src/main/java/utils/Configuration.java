package utils;

import java.io.IOException;
import java.util.Properties;

/**
 * This is a singleton to hold the parse configuration centrally for all objects to access.
 * It reads the file: configuration.properties from the classpath
 */
public class Configuration {
    private static final String CONFIGURATION_FILE = "configuration.properties";

    private record MailParserConfiguration(String emailSenderFilter, String emailSubjectFilter, String emailSearchQuery, long maxSearchResultsLimit, boolean outputMessagesToFile) {}

    private static Configuration instance;

    private static MailParserConfiguration parserConfiguration;


    private Configuration() {
    }

    private static void createInstance() {
        Properties props = new Properties();
        try {
            props.load(Configuration.class.getClassLoader().getResourceAsStream(CONFIGURATION_FILE));
            boolean outputMessagesFlag = Boolean.parseBoolean(props.getProperty("writeMessagesToFile", "false"));

            long maxSearchResults;
            try {
                maxSearchResults = Long.parseLong(props.getProperty("messageSearchQueryLimit", "50"));
            } catch (NumberFormatException nfe) {
                System.err.println("Failed to parse max search query limit value from config file, using default value");
                maxSearchResults = 50l;
            }

            Configuration.parserConfiguration = new MailParserConfiguration(props.getProperty("fromEmailFilter"),
                    props.getProperty("subjectEmailFilter"),
                    props.getProperty("messageSearchQuery"),
                    maxSearchResults,
                    outputMessagesFlag);
            props = null;
        } catch (IOException e) {
            System.err.println("Failed to load configuration file: " + CONFIGURATION_FILE);
            throw new RuntimeException(e);
        }
        instance = new Configuration();
    }

    public static synchronized Configuration getInstance() {
        if (instance == null) {
            createInstance();
        }
        return instance;
    }

    public boolean outputMessagesToFile() {
        return parserConfiguration.outputMessagesToFile();
    }

    public boolean hasSubjectMailFilter() {
        return parserConfiguration.emailSubjectFilter() != null &&
                !parserConfiguration.emailSubjectFilter().isEmpty();
    }

    public String subjectMailFilter() {
        return parserConfiguration.emailSubjectFilter();
    }

    public boolean hasSenderMailFilter() {
        return parserConfiguration.emailSenderFilter() != null &&
                !parserConfiguration.emailSenderFilter().isEmpty();
    }

    public String senderMailFilter() {
        return parserConfiguration.emailSenderFilter();
    }

    public boolean hasMailSearchQuery() {
        return parserConfiguration.emailSearchQuery() != null &&
                !parserConfiguration.emailSearchQuery().isEmpty();
    }

    public String searchQuery() {
        return parserConfiguration.emailSearchQuery();
    }

    public long getMailSearchQueryResultsLength() {
        return parserConfiguration.maxSearchResultsLimit;
    }
}
