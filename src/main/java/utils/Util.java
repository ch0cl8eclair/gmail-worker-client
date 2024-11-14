package utils;

import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.MessagePart;
import com.google.api.services.gmail.model.MessagePartHeader;
import parser.CSVRecord;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Base64;
import java.util.List;
import java.util.Optional;


public class Util {
    public enum EMAIL_MIME_TYPE {
        HTML("html-body"), TEXT("text-body");

        String contentValue;
        EMAIL_MIME_TYPE(String contentString) {
            this.contentValue = contentString;
        }
    }

    public static String base64UrlDecode(String msgString) {
        return new String(Base64.getUrlDecoder().decode(msgString));
    }

    public static String decodeMessageContent(Message message, EMAIL_MIME_TYPE messagePartType) {
        // 1 try the main body
        if (message.getPayload().getBody().getSize() != 0) {
            return base64UrlDecode(message.getPayload().getBody().getData());
        }
        // 2 try the message parts list
        else {
            for (MessagePart msgPart : message.getPayload().getParts()) {
                // filter by Content Type
                if (isRequestedMessagePart(msgPart.getHeaders(), messagePartType))
                    return base64UrlDecode(msgPart.getBody().getData());
            }
        }
        return null;
    }

    public static boolean isRequestedMessagePart(List<MessagePartHeader> headers,
                                                  EMAIL_MIME_TYPE messagePartType) {

        Optional<String> messageContentType = headers.stream().filter(s -> s.getName().toUpperCase().equals("CONTENT-ID")).map(it -> it.getValue()).findFirst();
        String messagePartConentValue = messageContentType.orElseGet(() -> "None");
        boolean isRequestedMessagePart = messagePartType.contentValue.equals(messagePartConentValue);
        //boolean isRequestedMessagePart = messageContentType.ifPresent(it -> it.equals(messagePartType.contentValue));
        return isRequestedMessagePart;
    }

    public static String addFileSuffix(String csvOutputFilename, String suffix, String extension) {
        if (csvOutputFilename == null || csvOutputFilename.length() < 1) {
            return String.format("%s-%s.txt", "myfile", suffix);
        }
        String[] filenameSplit = csvOutputFilename.split("\\.");
        return String.format("%s-%s.%s", filenameSplit[0], suffix, extension);
    }

    public static void outputListToFile(List<String> data, String filename) {
        try (PrintWriter out = new PrintWriter(filename)) {
            data.forEach(it -> out.println(it));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }
}
