package parser.linkedin;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

class LinkedInJobAlertEmailParserTest {

    private static LinkedInJobAlertEmailParser sut;
    private static final long SAMPLE_EPOCH_MS_1 = 1720812522000L;

    @BeforeAll
    public static void createParser() {
        // Remember that configuration.properties::writeMessagesToFile should be set to false
        sut = new LinkedInJobAlertEmailParser();
    }

    @Test
    void parse5LineSample() {
        String msgTxt = """
                Your job alert for senior software engineer in Slough
       
                9 new jobs match your preferences.
         
                Senior Java Engineer (Hybrid - Flexible Options)
                Broadridge
                London
                This company is actively hiring
                View job: https://www.linkedin.com/comm/jobs/view/3922718778/?trackingId=d%2BPEc1q2BTPoD1mpPBBsRQ%3D%3D&refId=ByteString%28length%3D16%2Cbytes%3D00c2eb73...f4ee0a73%29&lipi=urn%3Ali%3Apage%3Aemail_email_job_alert_digest_01%3Bse6BonSPQ4uCg49rk2U86A%3D%3D&midToken=AQFeW4mjPLPhqw&midSig=2UEt_EO_zcKXk1&trk=eml-email_job_alert_digest_01-job_card-0-view_job&trkEmail=eml-email_job_alert_digest_01-job_card-0-view_job-null-3u5492~lyq4x0lq~sj-null-null&eid=3u5492-lyq4x0lq-sj&otpToken=MTAwNjFjZTAxMDJiY2VjZGJjMjQwNGVkNDQxZmU1Yjc4N2NiZDA0MjkwYWQ4NjYxNzdjMTAzNmM0NjU5NWZmNGYyZGZhYjhiNDhiNWVkODA3MzhjMTZmYmY5ZDlhN2ViZDI2MGQ3MzBhMGNkMmEyZTgzZmUsMSwx
                                
                ---------------------------------------------------------
                                
                """;
        List<LinkedInAlert> parsedAlertList = sut.parse(SAMPLE_EPOCH_MS_1, msgTxt);
        assertEquals(1, parsedAlertList.size());
        final LinkedInAlert linkedInAlert = parsedAlertList.getFirst();
        assertAll("Alert",
                () -> assertEquals(SAMPLE_EPOCH_MS_1, linkedInAlert.emailDate()),
                () -> assertEquals("12/Jul/2024", linkedInAlert.formatDate()),
                () -> assertEquals("Senior Java Engineer (Hybrid - Flexible Options)", linkedInAlert.title()),
                () -> assertEquals("Broadridge", linkedInAlert.company()),
                () -> assertEquals("London", linkedInAlert.location()),
                () -> assertEquals("This company is actively hiring", linkedInAlert.additional()),
                () -> assertEquals("https://www.linkedin.com/comm/jobs/view/3922718778", linkedInAlert.link())
        );
    }

    @Test
    void parse7LineSample() {
        String msgTxt = """
Your job alert for senior software engineer in Slough
       
9 new jobs match your preferences.
         
Java Software Engineer - Hybrid Working - Up to £130
000 + Bonus
Hunter Bond
Greater London
Fast growing
Apply with resume & profile
View job: https://www.linkedin.com/comm/jobs/view/3976166589/?trackingId=pSIMQ22AK6vekPP9iYwZKg%3D%3D&refId=ByteString%28length%3D16%2Cbytes%3D6b5fff93...ef9d72ac%29&lipi=urn%3Ali%3Apage%3Aemail_email_job_alert_digest_01%3B2kTvTX%2FTThaIgBym1Xo1zg%3D%3D&midToken=AQFeW4mjPLPhqw&midSig=1jYuwkygkZJHk1&trk=eml-email_job_alert_digest_01-job_card-0-view_job&trkEmail=eml-email_job_alert_digest_01-job_card-0-view_job-null-3u5492~lyoq31cu~60-null-null&eid=3u5492-lyoq31cu-60&otpToken=MTAwNjFjZTAxMDJiY2VjZGJjMjQwNGVkNDQxZmU2YjE4Y2NmZDE0NTk4YWY4NzYxNzdjMTAzNmM0NjU5NWZmNGYyZGY5Zjg0MTRiMmNhYzg1OGYzMDAxZTFkMzI4NjY4MTBlYTU0ZDc3MjhmMThiODRkOGIsMSwx

---------------------------------------------------------
                """;
        List<LinkedInAlert> parsedAlertList = sut.parse(SAMPLE_EPOCH_MS_1, msgTxt);
        assertEquals(1, parsedAlertList.size());
        final LinkedInAlert linkedInAlert = parsedAlertList.getFirst();
        assertAll("Alert",
                () -> assertEquals(SAMPLE_EPOCH_MS_1, linkedInAlert.emailDate()),
                () -> assertEquals("12/Jul/2024", linkedInAlert.formatDate()),
                () -> assertEquals("Java Software Engineer - Hybrid Working - Up to £130 - 000 + Bonus", linkedInAlert.title()),
                () -> assertEquals("Hunter Bond", linkedInAlert.company()),
                () -> assertEquals("Greater London", linkedInAlert.location()),
                () -> assertEquals("Fast growing. Apply with resume & profile", linkedInAlert.additional()),
                () -> assertEquals("https://www.linkedin.com/comm/jobs/view/3976166589", linkedInAlert.link())
        );
    }

    @Test
    void isEmptyLine() {
        assertTrue(sut.isEmptyLine(""));
        // There is no null check atm
//        assertTrue(sut.isEmptyLine(null));
        assertTrue(sut.isEmptyLine("   "));
        assertTrue(sut.isEmptyLine("      "));
        assertFalse(sut.isEmptyLine(" -"));
    }

    @Test
    void isHyphenSeparatorLine() {
        assertTrue(sut.isHyphenSeparatorLine("----"));
        assertTrue(sut.isHyphenSeparatorLine("-------------------"));
        assertFalse(sut.isHyphenSeparatorLine("---- 123"));
        assertFalse(sut.isHyphenSeparatorLine(""));
        assertFalse(sut.isHyphenSeparatorLine("    "));
    }

    @Test
    void chompLink() {
        assertNull( sut.chompLink(null));
        assertEquals("", sut.chompLink(""));
        assertEquals("   ", sut.chompLink("   "));
        assertEquals("https://www.linkedin.com/comm/jobs/view/3980342987", sut.chompLink("https://www.linkedin.com/comm/jobs/view/3980342987/?trackingId=rDK7yNnYfQGa6%2FjS1%2FuFxA%3D%3D&refId=ByteString%28length%3D16%2Cbytes%3Deeeb2cb4...d51b6cfd%29&lipi=urn%3Ali%3Apage%3Aemail_email_job_alert_digest_01%3BddCXLj%2FtQPScONCjKXfx3A%3D%3D&midToken=AQFeW4mjPLPhqw&midSig=1IX3ke7txRSbk1&trk=eml-email_job_alert_digest_01-job_card-0-view_job&trkEmail=eml-email_job_alert_digest_01-job_card-0-view_job-null-3u5492~lyxf0ggb~tp-null-null&eid=3u5492-lyxf0ggb-tp&otpToken=MTAwNjFjZTAxMDJiY2VjZGJjMjQwNGVkNDQxZmUxYjM4N2M5ZDQ0NTk5YTk4ZjYxNzdjMTAzNmM0NjU5NWZmNGYyZGY4MThiNGNmOWYxZTkwM2Y5ODMzNGI2OWVhNDFmZWM4ZWYxYzhhOWQ5ZTllYWQxYWYsMSwx"));
        assertEquals("https://www.linkedin.com/comm/jobs/view/3980342987/?hello=world", sut.chompLink("https://www.linkedin.com/comm/jobs/view/3980342987/?hello=world"));
    }
}