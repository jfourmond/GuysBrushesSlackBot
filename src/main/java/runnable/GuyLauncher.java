package runnable;

import bot.Guy;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;

import java.net.URI;
import java.util.concurrent.TimeUnit;

public class GuyLauncher {
    private static final Logger Log = LogManager.getLogger(GuyLauncher.class);

    private static final int DURATION = 120;
    private static final TimeUnit UNIT = TimeUnit.MINUTES;

    public static void main(String args[]) {

        WebSocketClient client = new WebSocketClient();
        Guy socket;
        try {
            socket = new Guy();
        } catch (Exception e) {
            //  Erreur lors de l'appel aux informations
            e.printStackTrace();
            return;
        }

        try {
            client.start();
            URI echoUri = new URI(socket.getUrl());

            ClientUpgradeRequest request = new ClientUpgradeRequest();
            client.connect(socket, echoUri, request);

            Log.info("Connecting to : " + echoUri);

            socket.awaitClose(DURATION, UNIT);
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
