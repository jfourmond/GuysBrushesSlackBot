package runnable;

import api.SlackAPI;
import bot.GuyReactionAnalyser;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class GuyReactionAnalyzeLauncher {
	private static SlackAPI api = new SlackAPI(true);

	private static final String URL = "url";
	private static final String ID = "id";

	private static final int DURATION = 120;
	private static final TimeUnit UNIT = TimeUnit.MINUTES;

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
		GuyReactionAnalyser socket;
		try {
			socket = new GuyReactionAnalyser(id);
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