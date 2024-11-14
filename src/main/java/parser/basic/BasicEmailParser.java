package parser.basic;

import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.MessagePartHeader;
import parser.MessageParser;

import java.util.List;

public class BasicEmailParser implements MessageParser {
    @Override
    public List<BasicEmail> parse(Message msg) {
        Long internalDate = msg.getInternalDate();
        List<MessagePartHeader> headers = msg.getPayload().getHeaders();
        String messageSubject = headers.stream().filter(s -> s.getName().equalsIgnoreCase("SUBJECT")).map(MessagePartHeader::getValue).findFirst().orElse("Not Found");
        String messageSender  = headers.stream().filter(s -> s.getName().equalsIgnoreCase("FROM")).map(MessagePartHeader::getValue).findFirst().orElse("Not Found");
        String messageFrom    = headers.stream().filter(s -> s.getName().equalsIgnoreCase("TO")).map(MessagePartHeader::getValue).findFirst().orElse("Not Found");
        String messageSentDate = headers.stream().filter(s -> s.getName().equalsIgnoreCase("DATE")).map(MessagePartHeader::getValue).findFirst().orElse("Not Found");
        System.out.println("Message sent date is: " + messageSentDate);
        return List.of(new BasicEmail(internalDate, messageFrom, messageSender, messageSubject));
    }

    @Override
    public void cleanup() {
    }

    @Override
    public String getCSVOutputFilename() {
        return "email-listing.csv";
    }
}
