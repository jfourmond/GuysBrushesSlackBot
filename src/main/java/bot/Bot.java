package bot;

import api.SlackAPI;
import beans.channels.ChannelType;
import beans.events.Message;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

@WebSocket
public abstract class Bot {
	private static final Logger Log = LogManager.getLogger(Bot.class);

	// API SLACK
	protected SlackAPI api;

	//  INFORMATION SUR LE BOT
	String id;
	String name;

	final CountDownLatch closeLatch;
	Session session;

	//  INFORMATION DE DUREE
	private int duration;
	private TimeUnit unit;

	protected Bot(String id, String botName) throws Exception {
		Log.info("Création du bot : " + id + "(" + botName + ")");
		this.id = id;
		this.name = botName;

		initialisation();

		this.closeLatch = new CountDownLatch(1);
	}

	/**
	 * Initialisation du {@link Bot}
	 *
	 * @throws Exception
	 */
	protected void initialisation() throws Exception {
		//  API
		api = new SlackAPI(true);
	}

	/**
	 * Connection du {@link WebSocket}
	 *
	 * @param session
	 */
	@OnWebSocketConnect
	public void onConnect(Session session) {
		Log.info("Connexion");
		this.session = session;
	}

	/**
	 * Réception d'un message
	 *
	 * @param message : message reçu
	 */
	@OnWebSocketMessage
	public void onMessage(String message) {
		Log.info("Message reçu : " + message);
	}

	@OnWebSocketError
	public void onError(Throwable t) {
		Log.error("Error: " + t.getMessage());
	}

	/**
	 * Fermeture du {@link WebSocket}
	 *
	 * @param statusCode
	 * @param reason
	 */
	@OnWebSocketClose
	public void onClose(int statusCode, String reason) {
		Log.info("Fermeture de la connexion : " + statusCode + " - " + reason);

		this.session = null;
		this.closeLatch.countDown();
	}

	public boolean awaitClose(int duration, TimeUnit unit) throws InterruptedException {
		this.duration = duration;
		this.unit = unit;
		return this.closeLatch.await(duration, unit);
	}

	/**
	 * Envoi un message via le {@link WebSocket}
	 *
	 * @param text      : texte à envoyer
	 * @param channelId : identifiant du {@link beans.channels.Channel} où envoyer le message
	 * @return
	 */
	Future<Void> sendMessage(String text, String channelId) {
		Log.info("Envoi d'un message sur le channel : " + channelId);
		return session.getRemote().sendStringByFuture(
				"{ " +
						"\"type\" : \"message\", " +
						"\"text\" : \"" + text + "\"," +
						"\"channel\" : \"" + channelId + "\"" +
						"}");
	}

	/**
	 * Teste si le {@link Bot} a été cité dans le message passé en paramètre
	 *
	 * @param M : le message
	 * @return {@code true} si le bot a été cité dans le message, {@code false} sinon
	 */
	boolean hasBeenCited(Message M) {
		return (M.getText().contains("<@" + id + ">") || M.getText().toLowerCase().contains(name.toLowerCase()));
	}

	boolean isPublicChannel(String channelId) {
		return channelId.charAt(0) == 'C';
	}

	ChannelType getChannelTypeFromMessage(Message M) {
		switch (M.getChannel().charAt(0)) {
			case 'C':
				return ChannelType.PUBLIC;
			case 'D':
				return ChannelType.DIRECT_MESSAGE;
			default:
				return ChannelType.PRIVATE;
		}
	}
}
