package bot;

import api.SlackAPI;
import beans.channels.ChannelType;
import beans.events.Message;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

@WebSocket
public abstract class Bot {
	private static final Logger Log = LogManager.getLogger(Bot.class);
	
	// API SLACK
	protected SlackAPI api = new SlackAPI(true);
	
	private static final String URL = "url";
	private static final String ID = "id";
	
	//  INFORMATION SUR LE BOT
	String id;
	private String url;
	private String name;
	
	protected final CountDownLatch closeLatch = new CountDownLatch(1);
	Session session;
	
	//  INFORMATION DE DUREE
	int duration;
	private TimeUnit unit;
	
	protected Bot(String botName) throws Exception {
		Log.info("Création du bot : " + botName);
		this.name = botName;
		
		initialisation();
	}
	
	//  GETTERS
	public String getName() {
		return name;
	}
	
	public String getUrl() {
		return url;
	}
	
	public int getDuration() {
		return duration;
	}
	
	public TimeUnit getDurationUnit() {
		return unit;
	}
	
	//  SETTERS
	public void setName(String name) {
		this.name = name;
	}
	
	public void setUrl(String url) {
		this.url = url;
	}
	
	
	/**
	 * Initialisation du {@link Bot}
	 *
	 * @throws Exception
	 */
	protected void initialisation() throws Exception {
		Map<String, String> map;
		try {
			map = api.connect();
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}
		
		url = map.get(URL);
		id = map.get(ID);
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
		
		this.session.close();
		this.closeLatch.countDown();
	}
	
	public boolean awaitClose(int duration, TimeUnit unit) throws InterruptedException {
		this.duration = duration;
		this.unit = unit;
		return this.closeLatch.await(duration, unit);
	}
	
	/**
	 * Construit un message à envoyer
	 *
	 * @param text      : texte à envoyer
	 * @param channelId identifiant du {@link beans.channels.Channel} où envoyer le message
	 * @return
	 */
	private Future<Void> buildMessage(List<String> text, String channelId) {
		String message = "{\"type\" : \"message\", \"text\" : \"";
		for (String t : text) {
			message = message.concat(t).concat("\\n");
		}
		message = message.concat("\",\"channel\" : \"" + channelId + "\"}");
		return session.getRemote().sendStringByFuture(message);
	}
	
	/**
	 * Méthode raccourci pour {@link Bot#sendMessage(List, String)} dans le cas d'un message d'une ligne.
	 */
	void sendMessage(String text, String channelId) throws InterruptedException, ExecutionException, TimeoutException {
		sendMessage(Arrays.asList(text), channelId);
	}
	
	/**
	 * Envoie un message via le {@link WebSocket}
	 *
	 * @param text      : texte à envoyer
	 * @param channelId : idenfiant du {@link beans.channels.Channel} où envoyer le message
	 * @throws ExecutionException
	 * @throws InterruptedException
	 * @throws TimeoutException
	 */
	void sendMessage(List<String> text, String channelId) throws ExecutionException, InterruptedException, TimeoutException {
		Future<Void> fut = buildMessage(text, channelId);
		
		// Envoi du message
		try {
			fut.get(2, TimeUnit.SECONDS);
		} catch (ExecutionException | InterruptedException e) {
			// L'envoi a échoué
			e.printStackTrace();
			throw (e);
		} catch (TimeoutException e) {
			// Timeout
			e.printStackTrace();
			fut.cancel(true);
			throw (e);
		} catch (Exception e) {
			e.printStackTrace();
			throw (e);
		}
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
