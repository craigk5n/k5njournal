package us.k5n.journal;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

public class UIDGenerator {

    /**
     * Generates a UID suitable for use in a VJOURNAL entry for iCalendar.
     * The UID contains the product name "k5njournal", the owner "k5n", and a unique
     * identifier generated using the current date, time, and a random UUID.
     * 
     * @return a unique UID string
     */
    public static String generateVJournalUID() {
        // Get current date and time in a simple format (e.g., YYYYMMDD-HHmmss)
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd-HHmmss");
        String dateTime = dateFormat.format(new Date());

        // Generate a random UUID
        String randomUUID = UUID.randomUUID().toString();

        // Combine components to create the UID
        String uid = "k5njournal-" + dateTime + "-" + randomUUID + "@k5n.us";

        return uid;
    }
}