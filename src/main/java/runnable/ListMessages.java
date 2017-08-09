package runnable;

import api.SlackAPI;
import beans.events.Message;

import java.util.List;

public class ListMessages {
    private static final String GENERAL_ID = "C043JFBDZ";

    public static void main(String args[]) throws Exception {
        SlackAPI api = new SlackAPI(false);

        List<Message> messages = api.fetchAllMessages(GENERAL_ID, null);

        System.out.println(messages.size() + " MESSAGES");
    }
}
