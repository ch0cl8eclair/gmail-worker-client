package parser;

import com.google.api.services.gmail.model.Message;

import java.util.List;

public interface MessageParser {
    /**
     * Main method that parses the given Gmail Email message object.
     * @param msg
     * @return List of generated output records from the Message
     */
    List parse(Message msg);

    /**
     * Permits the parser to clean up any open resources
     */
    void cleanup();

    /**
     * The name of the csv file to be generated for the output
     * @return
     */
    String getCSVOutputFilename();
}
