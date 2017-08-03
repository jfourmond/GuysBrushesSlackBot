package runnable;

import api.SlackAPI;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import bot.Guy;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class GuyLauncher {
    private static final Logger Log = LogManager.getLogger(GuyLauncher.class);

    private static SlackAPI api = new SlackAPI();

    private static final String URL = "url";
    private static final String ID = "id";

    public static void main(String args[]) {
        Map<String, String> map;
        String url;
        String id;
        try {
            map = api.connect();
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        url = map.get(URL);
        id = map.get(ID);

        WebSocketClient client = new WebSocketClient();
        Guy socket;
        try {
            socket = new Guy(id);
        } catch (Exception e) {
            //  Erreur lors de l'appel aux informations
            e.printStackTrace();
            return;
        }

        try {
            client.start();
            URI echoUri = new URI(url);

            ClientUpgradeRequest request = new ClientUpgradeRequest();
            client.connect(socket, echoUri, request);

            System.out.println("Connecting to : " + echoUri);

            socket.awaitClose(1, TimeUnit.MINUTES);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                client.stop();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
