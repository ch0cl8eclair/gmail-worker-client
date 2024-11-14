import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.batch.BatchRequest;
import com.google.api.client.googleapis.batch.json.JsonBatchCallback;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.googleapis.json.GoogleJsonError;
import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.GmailScopes;
import com.google.api.services.gmail.model.*;
import com.google.common.collect.Lists;
import parser.linkedin.LinkedInAlert;
import utils.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import parser.CSVRecord;
import parser.MessageParser;
import parser.basic.BasicEmailParser;
import parser.linkedin.LinkedInJobAlertEmailParser;

import java.io.*;
import java.security.GeneralSecurityException;
import java.util.*;
import java.util.stream.Collectors;

import static utils.Util.addFileSuffix;
import static utils.Util.outputListToFile;

/* class to demonstrate use of Gmail list labels API */

/**
 * Main class that connects to Gmail API, obtains messages, parses them and exports them to a csv file.
 *
 * The following details the search terms available for the search query for messages:
 * <a href="https://support.google.com/mail/answer/7190">Search Terms</a>
 */
public class GmailMessageExporter {
  private static final Logger logger = LoggerFactory.getLogger(GmailMessageExporter.class.getName());

  /**
   * Application name.
   */
  private static final String APPLICATION_NAME = "Gmail Parser Client";
  /**
   * Global instance of the JSON factory.
   */
  private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
  /**
   * Directory to store authorization tokens for this application.
   */
  private static final String TOKENS_DIRECTORY_PATH = "tokens";

  /**
   * Global instance of the scopes required by this quickstart.
   * If modifying these scopes, delete your previously saved tokens/ folder.
   */
  // private static final List<String> SCOPES = Arrays.asList(GmailScopes.GMAIL_LABELS, GmailScopes.GMAIL_READONLY, GmailScopes.GMAIL_METADATA);
  private static final List<String> SCOPES = Collections.singletonList(GmailScopes.GMAIL_MODIFY);
  private static final String CREDENTIALS_FILE_PATH = "/credentials.json";

  // TODO remove delete option later
  private enum COMMANDS {LABELS, SEARCH, LIST, DELETE}

  private static final String USAGE = """
          GmailMessageExporter command
          where command can be one of:
          - search [default]
            Issues the search query from the the configuration file to find messages
          - labels
            Lists the labels from the Gmail account
          - list
            Lists the emails from the Gmail account as found in the inbox
            
          Parser configuration parameters can be found in configuration.properties which should be on the classpath. This
          file has the following options:
          - writeMessagesToFile - boolean flag if the email message should be outputted to a text file
          - fromEmailFilter - filters the emails by sender this given string value
          - subjectEmailFilter - filters the emails by subject this given string value 
          - messageSearchQuery - gmail search query string
          - messageSearchQueryLimit - max search query results to return.
          """;

  /**
   * Creates an authorized Credential object.
   *
   * @param HTTP_TRANSPORT The network HTTP Transport.
   * @return An authorized Credential object.
   * @throws IOException If the credentials.json file cannot be found.
   */
  private static Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT)
      throws IOException {
    // Load client secrets.
    InputStream in = GmailMessageExporter.class.getResourceAsStream(CREDENTIALS_FILE_PATH);
    if (in == null) {
      throw new FileNotFoundException("Resource not found: " + CREDENTIALS_FILE_PATH);
    }
    GoogleClientSecrets clientSecrets =
        GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

    // Build flow and trigger user authorization request.
    GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
        HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
        .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
        .setAccessType("offline")
        .build();
    LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
    Credential credential = new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
    //returns an authorized Credential object.
    return credential;
  }

  private static void outputLabels(Gmail service)  throws IOException, GeneralSecurityException {
    String user = "me";
    ListLabelsResponse listResponse = service.users().labels().list(user).execute();
    List<Label> labels = listResponse.getLabels();
    if (labels.isEmpty()) {
      System.out.println("No labels found.");
    } else {
      System.out.println("Labels:");
      for (Label label : labels) {
        System.out.println( "- " + label.getName() );
      }
    }
  }

  /**
   * The main method that fetches a list of message from gmail. Only the msg id is returned. This method is wrapped by other methods.
   * @param service - Gmail service
   * @param query - gmail style query
   * @param requestedMax - a value of 100 should be considered the default
   * @return List of parsed messages
   * @throws IOException should any retrieval issues occur
   */
  private static List<Message> performFetchGmailMessages(Gmail service, String query, long requestedMax) throws IOException {
    String user = "me";
    List<Message> accumulatMessages = new ArrayList<>();

    String nextPageToken = null;
    boolean done = false;
    int itertionCount = 0;
    int previousMsgCount = 0;
    while(!done) {
      Gmail.Users.Messages.List listRequest = service.users().messages().list(user);
      if (nextPageToken != null) {
        listRequest.setPageToken(nextPageToken);
      }
      if (query != null) {
        listRequest.setQ(query);
      }
      listRequest.setMaxResults(requestedMax);
      ListMessagesResponse listResponse = listRequest.execute();
      List<Message> messages = listResponse.getMessages();
      nextPageToken = listResponse.getNextPageToken();
      if (messages.isEmpty()) {
        done = true;
      }
      accumulatMessages.addAll(messages);
      int currentMessageCount = accumulatMessages.size();
      // End the loop once we have the max requested number of msgs or we are nolonger getting new msgs.
      if (currentMessageCount >= requestedMax || previousMsgCount == currentMessageCount) {
        done = true;
      }
      previousMsgCount = currentMessageCount;
      itertionCount++;
    }
    logger.info("Retrieved {} messages in {} requests", accumulatMessages.size(), itertionCount);
    return accumulatMessages;
  }

  private static List<Message> listMessages(Gmail service, long requestedMax) throws IOException {
    return performFetchGmailMessages(service, null, requestedMax);
  }

  // from:jobalerts-noreply@linkedin.com
  // newer_than:1d
  private static List<Message> searchMessages(Gmail service, String query, long requestedMax) throws IOException {
    return performFetchGmailMessages(service, query, requestedMax);
  }

  private static void outputObjectsAsCSVToFile(List<? extends CSVRecord> messages, String filename) {
    try (PrintWriter out = new PrintWriter(filename)) {
      messages.forEach(it -> out.println(it.toCsv()));
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }
  }

  /**
   * Given a list of bare messages (just the msg ids), this method fully fetches the message details from Gmail.
   * The operation is performed in batches of 50msg id requests.
   * Visit <a href="https://developers.google.com/gmail/api/reference/quota">gmail quotas</a> for daily limits
   * @param service Gmail service
   * @param partialMessages - list of message ids to resolve
   * @return List of parsed messages
   * @throws IOException should any retrieval issues occur
   */
  private static List<Message> batchFetch(Gmail service, List<Message> partialMessages) throws IOException {
    final int BATCH_CHUCK_SIZE = 50; // Api recommended
    final List<Message> fullyQualifiedMessageList = new ArrayList<>();

    final JsonBatchCallback<Message> callback = new JsonBatchCallback<Message>() {
      public void onSuccess(Message message, HttpHeaders responseHeaders) {
        fullyQualifiedMessageList.add(message);
      }

      public void onFailure(GoogleJsonError e, HttpHeaders responseHeaders) {
        logger.error("Failed to execute batch message fetch request, " + e.getMessage());
      }
    };

    BatchRequest batch = service.batch();
    List<List<Message>> partitionedLists = Lists.partition(partialMessages, BATCH_CHUCK_SIZE);
    for (List<Message> subList : partitionedLists) {
      for (Message message : subList) {
        service.users().messages().get("me", message.getId()).setFormat("full").queue(batch, callback);
      }
      batch.execute();
    }
    return fullyQualifiedMessageList;
  }

  private static void genericExportEmails(Gmail service, MessageParser parser, List<Message> partialMessages) throws IOException {
    logger.debug("genericExportEmails begin");
    logger.debug(">>> Number of emails listed is: " + partialMessages.size());

    Configuration parserConfiguration = Configuration.getInstance();
    String configuredEmailSenderFilter = parserConfiguration.subjectMailFilter();

    // batch fetch message
    List<Message> fullMessages = batchFetch(service, partialMessages);

    // TODO capture post filter msg ids for deletion purposes
    // Get the base list with filtered email messages
    List<Message> filteredMessagesList = fullMessages.stream().filter(it -> filterMessageBySender(it, configuredEmailSenderFilter)).toList();
    // Perform the first operation to get the email message ids from the base list
    Set<String> messageIds = filteredMessagesList.stream().map(it -> it.getId()).collect(Collectors.toSet());
    // Performs the second operation to parse the emails from the base list
    List<? extends CSVRecord> records = filteredMessagesList.stream()
            .map(parser::parse)
            .flatMap(Collection::stream)
            .toList();
    parser.cleanup();

    outputObjectsAsCSVToFile(records, parser.getCSVOutputFilename());
    logger.info("Number of output records found is: {}", records.size());
    if (parser instanceof LinkedInJobAlertEmailParser) {
      outputUniqueLinkedinUrlsToFile((List<LinkedInAlert>) records, addFileSuffix(parser.getCSVOutputFilename(), "urls", "txt"));
    }
    deleteProcessedEmailMessages(service, messageIds);
  }

  private static void outputUniqueLinkedinUrlsToFile(List<LinkedInAlert> records, String outputFilename) {
    List<String> urls = records.stream().map(LinkedInAlert::link).distinct().toList();
    outputListToFile(urls, outputFilename);
  }

  private static void deleteProcessedEmailMessages(Gmail service, Set<String> messageIds) {
    Configuration parserConfiguration = Configuration.getInstance();
    int msgLength = messageIds != null ? messageIds.size() : 0;
    if (!parserConfiguration.deleteEmailMessages() || msgLength < 1){
      return;
    }

    logger.info("Message ids to delete are: " + String.join(", ", messageIds));

    final JsonBatchCallback<Message> callback = new JsonBatchCallback<Message>() {
      public void onSuccess(Message message, HttpHeaders responseHeaders) {
        logger.info("Processed gmail messages have been deleted");
      }

      public void onFailure(GoogleJsonError e, HttpHeaders responseHeaders) {
        logger.error("Failed to delete processed gmail message: " + e.getMessage());
      }
    };

    try {
      BatchRequest batch = service.batch();
      for (String messageId : messageIds) {
        service.users().messages().trash("me", messageId).queue(batch, callback);
      }
      batch.execute();
    } catch (IOException io) {
      logger.error("Failed to successfully delete processed gmail msgs, " + io.getMessage(), io);
    }
  }

  private static boolean filterMessageBySubject(Message message, String subject) {
    if (subject == null || subject.isEmpty())
      return true;
    List<MessagePartHeader> headers = message.getPayload().getHeaders();
    String messageSubject = headers.stream().filter(s -> s.getName().equalsIgnoreCase("SUBJECT")).map(MessagePartHeader::getValue).findFirst().orElse("Not Found");
    return messageSubject.contains(subject);
  }

  private static boolean filterMessageBySender(Message message, String sender) {
    if (sender == null || sender.isEmpty())
      return true;
    List<MessagePartHeader> headers = message.getPayload().getHeaders();
    String messageSender = headers.stream().filter(s -> s.getName().equalsIgnoreCase("FROM")).map(MessagePartHeader::getValue).findFirst().orElse("Not Found");
    return messageSender.contains(sender);
  }

  /**
   * Run the appropriate action against Gmail, this can be one of:
   * <li>Search</li> Search for messages and run the configured msg parser
   * <li>List</li> Simple list of email messages
   * <li>Labels</li> List the gmail labels
   * @param args - command line arguments
   * @throws IOException
   * @throws GeneralSecurityException
   */
  public static void main(String... args) throws IOException, GeneralSecurityException {
    // Build a new authorized API client service.
    final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
    Gmail service = new Gmail.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
        .setApplicationName(APPLICATION_NAME)
        .build();

    // Obtain instance of utils properties
    Configuration parserConfiguration = Configuration.getInstance();

    // Determine the command to run based on passed arguments, search is the default.
    COMMANDS requestCommand = COMMANDS.SEARCH;
    if (args.length > 0) {
      requestCommand = COMMANDS.valueOf(args[0].toUpperCase());
    }
    logger.debug("Processing command: {}", requestCommand);
    switch (requestCommand) {
      case SEARCH:
        LinkedInJobAlertEmailParser parser = new LinkedInJobAlertEmailParser();
        genericExportEmails(service,
                parser,
                searchMessages(service, parserConfiguration.searchQuery(),
                        parserConfiguration.getMailSearchQueryResultsLength()));
        break;

      case LIST:
        BasicEmailParser basicParser = new BasicEmailParser();
        genericExportEmails(service,
                basicParser,
                listMessages(service,
                        parserConfiguration.getMailSearchQueryResultsLength()));
        break;

      case LABELS:
        outputLabels(service);
        break;

      case DELETE:
        String[] messages = new String[] {"1930180cb567165f", "19301be07bb71986", "19301edeeec03d46", "193030179957b913", "193036f3403162c9", "19303a637d968bdf"};
        Set<String> msgsIdsToDelete = new HashSet<>();
        Collections.addAll(msgsIdsToDelete, messages);
        deleteProcessedEmailMessages(service, msgsIdsToDelete);
        break;

      default:
        System.err.println("Unknown command given: " + args[0] + "\n" + USAGE);
        System.exit(-1);
        break;
    }
  }
}
