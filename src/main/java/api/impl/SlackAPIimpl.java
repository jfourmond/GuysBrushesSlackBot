package api.impl;

import api.Attributes;
import api.Methods;
import api.SlackAPI;
import beans.Attachment;
import beans.File;
import beans.Member;
import beans.Paging;
import beans.channels.Channel;
import beans.events.Message;
import com.google.gson.stream.JsonReader;
import converter.Converter;

import javax.annotation.Nullable;
import java.io.*;
import java.net.URL;
import java.net.URLEncoder;
import java.util.*;

import static api.Attributes.*;
import static api.Methods.*;
import static api.URLParameters.*;
import static api.URLParameters.CHANNEL;
import static api.URLParameters.COUNT;
import static api.URLParameters.NAME;
import static api.URLParameters.PAGE;
import static api.URLParameters.TEXT;
import static api.URLParameters.TS;
import static api.URLParameters.USER;
import static converter.Converter.*;

public class SlackAPIimpl implements SlackAPI {

	private boolean bot;


	private JsonReader reader = null;


	private String stringUrl = "";
	private String token = "";
	private String botToken = "";

	/**
	 * @param bot spécifie certains appels à l'API doivent s'effectuer en tant que bot
	 */
	public SlackAPIimpl(boolean bot) {
		this.bot = bot;
		build();
	}

	//  GETTERS
	@Override
	public boolean isBot() {
		return bot;
	}

	//  SETTERS
	@Override
	public void setBot(boolean bot) {
		this.bot = bot;
	}

	//********************//
	//	   METHODES       //
	//********************//
	@Override
	public void build() {
		Log.info("Lecture du fichier de configuration...");
		Properties properties = new Properties();

		ClassLoader classLoader = getClass().getClassLoader();
		InputStream propertyFile = null;

		try {
			propertyFile = new FileInputStream(classLoader.getResource(PROPERTIES_FILE).getFile());
		} catch (FileNotFoundException e) {
			Log.error("Fichier de configuration \"" + PROPERTIES_FILE + "\" introuvable.");
			e.printStackTrace();
		}

		try {
			properties.load(propertyFile);
			token = properties.getProperty(PROPERTY_TOKEN);
			botToken = properties.getProperty(PROPERTY_BOT_TOKEN);
		} catch (IOException e) {
			Log.error("Impossible de charger les propriétés de configuration.");
			e.printStackTrace();
		}

		try {
			propertyFile.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		Log.info("Lecture terminée.");
	}

	@Override
	public void buildUrl(String method, Map<String, String> optionalParameters, boolean bot) {
		StringBuilder sb = new StringBuilder();
		sb.append(API_URL);
		sb.append(method);
		sb.append("?token=");
		if (bot)
			sb.append(botToken);
		else
			sb.append(token);
		if (optionalParameters != null) {
			optionalParameters.forEach((k, v) -> {
				sb.append("&");
				sb.append(k);
				sb.append("=");
				sb.append(v);
			});
		}
		sb.append("&pretty=1");
		stringUrl = sb.toString();
	}

	/**
	 * GET de l'URL
	 *
	 * @return le fichier de l'URL (Json dans le cas présent)
	 * @throws Exception si l'URL est malformée
	 */
	public String readUrl() throws Exception {
		Log.info("GET : - " + stringUrl);
		URL url = new URL(stringUrl);
		BufferedReader in = new BufferedReader(
				new InputStreamReader(url.openStream()));

		String inputLine;
		StringBuilder sb = new StringBuilder();
		while ((inputLine = in.readLine()) != null)
			sb.append(inputLine);
		in.close();
		return sb.toString();
	}

	//***********************************//
	//	   METHODES D'APPEL A L'API      //
	//***********************************//

	/**
	 * Appel à la méthode "api.test"
	 * Test de l'API
	 *
	 * @return la réponse à la requête
	 * @throws Exception si l'URL est malformée
	 * @see <a href="https://api.slack.com/methods/api.test">https://api.slack.com/methods/api.test</a>
	 */
	@Override
	public String test() throws Exception {
		Log.info("Test de l'API");
		buildUrl(Methods.METHOD_API_TEST, null, false);
		return readUrl();
	}

	/**
	 * Appel à la méthode "auth.test"
	 * Test de l'authentification
	 *
	 * @return la réponse à la requête
	 * @throws Exception si l'URL est malformée
	 * @see <a href="https://api.slack.com/methods/auth.test">https://api.slack.com/methods/auth.test</a>
	 */
	@Override
	public String authentificationTest() throws Exception {
		Log.info("Test de l'authentification");
		buildUrl(METHOD_AUTHENTIFICATION_TEST, null, false);
		return readUrl();
	}

	/**
	 * Appel à la méthode "rtm.connect"
	 * Demande une url de connection pour WebSocket
	 *
	 * @return une {@link Map} possédant deux clés, URL et ID
	 * @throws Exception si l'URL est malformée
	 * @see <a href="https://api.slack.com/methods/rtm.connect">https://api.slack.com/methods/rtm.connect</a>
	 */
	@Override
	public Map<String, String> connect() throws Exception {
		Log.info("Demande d'une URL de connection");
		Map<String, String> parameters = new HashMap<>();
		parameters.put("scope", URLEncoder.encode("rtm:stream", "UTF-8"));
		// Construction de l'URL
		buildUrl(METHOD_RTM_CONNECT, parameters, true);

		boolean ok;
		String name;
		Map<String, String> map = new HashMap<>();

		// Lecture de l'URL
		String json = readUrl();
		reader = new JsonReader(new StringReader(json));
		reader.beginObject();
		ok = readOk(reader);
		if (ok) {
			while (reader.hasNext()) {
				name = reader.nextName();
				switch (name) {
					case Attributes.URL:
						map.put(Attributes.URL, reader.nextString());
						break;
					case SELF:
						map.put(ID, readSelf(reader));
						break;
					default:
						reader.skipValue();
						break;
				}
			}
		} else {
			System.err.println(json);
			// Récupération du message d'erreur
			throw new Exception(readError(reader));
		}
		reader.endObject();
		reader.close();
		return map;
	}

	/**
	 * Appel à la méthode "channels.list"
	 * Liste les channels
	 *
	 * @return une liste des channels
	 * @throws Exception si l'URL est mal formée
	 * @see <a href="https://api.slack.com/methods/channels.list">https://api.slack.com/methods/channels.list</a>
	 */
	@Override
	public List<Channel> listChannels() throws Exception {
		Log.info("Enumération des channels");
		buildUrl(METHOD_LIST_CHANNELS, null, false);

		List<Channel> channels = null;
		boolean ok;
		String name;

		// Lecture de l'URL
		String json = readUrl();
		reader = new JsonReader(new StringReader(json));
		reader.beginObject();
		ok = readOk(reader);
		if (ok) {
			while (reader.hasNext()) {
				//  Lecture des channels
				name = reader.nextName();
				if (name.equals(CHANNELS))
					channels = readChannels(reader);
				else
					reader.skipValue();
			}
		} else {
			System.err.println(json);
			// Récupération du message d'erreur
			throw new Exception(readError(reader));
		}
		reader.endObject();
		reader.close();

		return channels;
	}

	/**
	 * Appel à la méthode "files.list"
	 * Liste les fichiers
	 *
	 * @param channelId identifiant du channel sur lequel filtrer
	 * @param count     nombre de fichiers dans la réponse
	 * @param page      page de la réponse
	 * @param userId    identifiant de l'utilisateur sur lequel filtrer
	 * @return une paire {@link List} de {@link beans.File}s et de {@link Paging}
	 * @throws Exception Si l'URL est mal formée
	 * @see <a href="https://api.slack.com/methods/files.list">https://api.slack.com/methods/files.list</a>
	 */
	@Override
	public Map.Entry<List<beans.File>, Paging> listFiles(@Nullable String channelId, @Nullable Integer count, @Nullable Integer page, @Nullable String userId) throws Exception {
		Log.info("Enumération des fichiers");
		//  Construction des paramètres optionnels
		Map<String, String> parameters = new HashMap<>();
		if (channelId != null) parameters.put(CHANNEL, channelId);
		if (count != null) parameters.put(COUNT, count.toString());
		if (page != null) parameters.put(PAGE, page.toString());
		if (userId != null) parameters.put(USER, userId);
		// Construction de l'URL
		buildUrl(METHOD_LIST_FILES, parameters, false);

		List<beans.File> files = null;
		Paging paging = null;
		boolean ok;
		String name;

		// Lecture de l'URL
		String json = readUrl();
		reader = new JsonReader(new StringReader(json));
		reader.beginObject();
		ok = readOk(reader);
		if (ok) {
			while (reader.hasNext()) {
				name = reader.nextName();
				switch (name) {
					case FILES:
						//  Lecture des fichiers
						files = readFiles(reader);
						break;
					case PAGING:
						paging = readPaging(reader);
						break;
					default:
						reader.skipValue();
						break;
				}
			}
		} else {
			System.err.println(json);
			// Récupération du message d'erreur
			throw new Exception(readError(reader));
		}
		reader.endObject();
		reader.close();

		return new AbstractMap.SimpleEntry<>(files, paging);
	}

	/**
	 * Appel à la méthode "users.list"
	 * Liste les membres
	 *
	 * @return une liste de membre
	 * @throws Exception Si l'URL est mal formée
	 * @see <a href="https://api.slack.com/methods/users.list">https://api.slack.com/methods/users.list</a>
	 */
	@Override
	public List<Member> listMembers() throws Exception {
		Log.info("Listage des membres");
		buildUrl(METHOD_LIST_USERS, null, false);

		List<Member> users = null;
		boolean ok;
		String name;

		// Lecture de l'URL
		String json = readUrl();
		reader = new JsonReader(new StringReader(json));
		reader.beginObject();
		ok = readOk(reader);
		if (ok) {
			while (reader.hasNext()) {
				name = reader.nextName();
				switch (name) {
					case MEMBERS:
						//  Lecture des fichiers
						users = readMembers(reader);
						break;
					default:
						reader.skipValue();
						break;
				}
			}
		} else {
			System.err.println(json);
			// Récupération du message d'erreur
			throw new Exception(readError(reader));
		}
		reader.endObject();
		reader.close();

		return users;
	}

	/**
	 * Appel à la méthode "chat.postMessage"
	 * Poste un message dans un channel
	 *
	 * @param channelId      identifiant du channel
	 * @param text           texte à envoyer
	 * @param iconUrl        URL d'une image utilisée comme icône au message
	 * @param username       nom du bot
	 * @param attachments    Un tableau JSON de pièce jointes, {@link Attachment} see <a href="https://api.slack.com/docs/message-attachments">https://api.slack.com/docs/message-attachments</a>
	 * @param iconEmoji      emoji utilisé comme icône du message
	 * @param threadTS       identifiant timestamp du message auquel répondre
	 * @param replyBroadcast (Défaut : {@code false}) à utiliser en association avec threadTS, {@code true} si la réponse doit être visible dans le channel ou dans la conversation
	 * @param unfurlLinks    {@code true} pour activer le déroulement (l'intégration) du contenu texte
	 * @param unfurlMedia    {@code false} pour désactiver le déroulement de contenu média
	 * @return la réponse à la requête
	 * @throws Exception si l'URL est malformée
	 * @see <a href="https://api.slack.com/methods/chat.postMessage">https://api.slack.com/methods/chat.postMessage</a>
	 */
	@Override
	public String postMessage(String channelId, String text, @Nullable Boolean asUser, @Nullable Attachment[] attachments,
							  @Nullable String iconEmoji, @Nullable String iconUrl, @Nullable Boolean replyBroadcast,
							  @Nullable String threadTS, @Nullable Boolean unfurlLinks, @Nullable Boolean unfurlMedia,
							  @Nullable String username) throws Exception {
		Log.info("Envoi d'un message");
		//  Construction des paramètres optionnels
		Map<String, String> parameters = new HashMap<>();
		parameters.put(CHANNEL, channelId);
		parameters.put(TEXT, URLEncoder.encode(text, "UTF-8"));
		if (asUser != null) parameters.put(AS_USER, asUser.toString());
		if (attachments != null)
			parameters.put(ATTACHMENTS, URLEncoder.encode(AttachmentsToJson(attachments), "UTF-8"));
		if (iconEmoji != null)
			parameters.put(ICON_EMOJI, iconEmoji);
		if (iconUrl != null) parameters.put(ICON_URL, URLEncoder.encode(iconUrl, "UTF-8"));
		if (replyBroadcast != null && threadTS != null)
			parameters.put(REPLY_BROADCAST, replyBroadcast.toString());
		if (threadTS != null)
			parameters.put(THREAD_TS, threadTS);
		if (unfurlLinks != null)
			parameters.put(UNFURL_LINKS, unfurlLinks.toString());
		if (unfurlMedia != null)
			parameters.put(UNFURL_MEDIA, unfurlMedia.toString());
		if (username != null) parameters.put(USERNAME, username);
		// Construction de l'URL
		buildUrl(METHOD_CHAT_POST_MESSAGE, parameters, bot);
		// Lecture de l'URL
		return readUrl();
	}

	/**
	 * Appel à la méthode "chat.meMessage"
	 * Poste un meMessage dans le channel
	 *
	 * @param channelId identifiant du channel
	 * @param text      text à envoyer comme un meMessage
	 * @return la réponse à la requête
	 * @throws Exception si l'URL est malformée
	 */
	@Override
	public String meMessage(String channelId, String text) throws Exception {
		Log.info("Envoi d'un MeMessage");
		//  Construction des paramètres optionnels
		Map<String, String> parameters = new HashMap<>();
		parameters.put(CHANNEL, channelId);
		parameters.put(TEXT, URLEncoder.encode(text, "UTF-8"));
		// Construction de l'URL
		buildUrl(METHOD_CHAT_ME_MESSAGE, parameters, bot);
		// Lecture de l'URL
		return readUrl();
	}

	/**
	 * Appel à la méthode "chat.update"
	 * Met à jour un message dans un channel
	 *
	 * @param channelId   identifiant du channel
	 * @param text        texte à envoyer
	 * @param ts          timestamp du message à éditer
	 * @param asUser      {@code true} pour éditer le message comme un utilisateur authentifié. (Les bots sont considérés comme des utilisateurs authentifiés)
	 * @param attachments Un tableau JSON de pièce jointes, {@link Attachment} see <a href="https://api.slack.com/docs/message-attachments">https://api.slack.com/docs/message-attachments</a>
	 * @param linkNames   Cherche et lie les noms des channels et des utilisateurs
	 * @param parse       Modifie la manière dont sont traités les messages
	 * @return {@code true} si le message a pu être édité
	 * @throws Exception si l'URL est mal formée
	 * @see <a href="https://api.slack.com/methods/chat.update">https://api.slack.com/methods/chat.update</a>
	 */
	@Override
	public boolean updateMEssage(String channelId, String text, String ts, @Nullable Boolean asUser, @Nullable Attachment[] attachments, @Nullable Boolean linkNames, @Nullable String parse) throws Exception {
		Log.info("Edition d'un message");
		// Construction des paramètres
		Map<String, String> parameters = new HashMap<>();
		parameters.put(CHANNEL, channelId);
		parameters.put(TEXT, URLEncoder.encode(text, "UTF-8"));
		parameters.put(TS, ts);
		if (asUser != null) parameters.put(AS_USER, asUser.toString());
		if (attachments != null)
			parameters.put(ATTACHMENTS, URLEncoder.encode(AttachmentsToJson(attachments), "UTF-8"));
		if (linkNames != null) parameters.put(LINK_NAMES, linkNames.toString());
		if (parse != null) parameters.put(PARSE, parse);
		// Construction de l'URL
		buildUrl(METHOD_CHAT_UPDATE, parameters, false);
		// Lecture de l'URL
		String json = readUrl();
		reader = new JsonReader(new StringReader(json));
		reader.beginObject();
		boolean ok = readOk(reader);
		if (!ok) {
			System.err.println(json);
			// Récupération du message d'erreur
			throw new Exception(readError(reader));
		} else
			while (reader.hasNext()) reader.skipValue();
		reader.endObject();

		return ok;
	}

	/**
	 * Appel à la méthode "chat.postEphemeral"
	 * Poste un message éphémère, visible seulement à l'utilisateur assigné, à un channel publique, channel privé, ou message directe
	 *
	 * @param channelId   identifiant du channel
	 * @param text        texte à envoyer
	 * @param userId      identifiant de l'utilisateur
	 * @param asUser      {@code true} pour envoyer le message comme un utilisateur authentifié. (Les bots sont considérés comme des utilisateurs authentifiés)
	 * @param attachments Un tableau JSON de pièce jointes, {@link Attachment} see <a href="https://api.slack.com/docs/message-attachments">https://api.slack.com/docs/message-attachments</a>
	 * @param linkNames   Cherche et lie les noms des channels et des utilisateurs
	 * @param parse       Modifie la manière dont sont traités les messages
	 * @return la réponse à la requête
	 * @throws Exception si l'URL est mal formée
	 * @see <a href="https://api.slack.com/methods/chat.postEphemeral">https://api.slack.com/methods/chat.postEphemeral</a>
	 */
	@Override
	public String postEphemeral(String channelId, String text, String userId, @Nullable Boolean asUser, @Nullable Attachment[] attachments, @Nullable Boolean linkNames, @Nullable String parse) throws Exception {
		Log.info("Envoi d'un message ephémère");
		//  Construction des paramètres optionnels
		Map<String, String> parameters = new HashMap<>();
		parameters.put(CHANNEL, channelId);
		parameters.put(TEXT, URLEncoder.encode(text, "UTF-8"));
		parameters.put(USER, userId);
		if (asUser != null) parameters.put(AS_USER, asUser.toString());
		if (attachments != null)
			parameters.put(ATTACHMENTS, URLEncoder.encode(Converter.AttachmentsToJson(attachments), "UTF-8"));
		if (linkNames != null) parameters.put(LINK_NAMES, linkNames.toString());
		if (parse != null) parameters.put(PARSE, parse);
		// Construction de l'URL
		buildUrl(METHOD_CHAT_POST_EPHEMERAL, parameters, bot);
		// Lecture de l'URL
		return readUrl();
	}

	/**
	 * Appel à la méthode "reactions.add"
	 * Ajoute une réaction
	 *
	 * @param reactionName réaction à ajouter (code emoji)
	 * @param channelId    identifiant du channel
	 * @param file         fichier de la réaction
	 * @param fileComment  commentaire du fichier de la réaction
	 * @param timestamp    identifiant du message de la réaction
	 * @see <a href="https://api.slack.com/methods/reactions.add">https://api.slack.com/methods/reactions.add</a>
	 */
	@Override
	public void addReaction(String reactionName, @Nullable String channelId, @Nullable String file, @Nullable String fileComment, @Nullable String timestamp) throws Exception {
		Log.info("Ajout d'une réaction");
		boolean ok;
		// Construction des paramètres optionnels
		Map<String, String> parameters = new HashMap<>();
		parameters.put(NAME, reactionName);
		if (channelId != null) parameters.put(CHANNEL, channelId);
		if (file != null) parameters.put(FILE, file);
		if (fileComment != null) parameters.put(FILE_COMMENT, fileComment);
		if (timestamp != null) parameters.put(TIMESTAMP, timestamp);
		// Construction de l'URL
		buildUrl(METHOD_REACTION_ADD, parameters, bot);
		//  Lecture de l'URL
		String json = readUrl();
		reader = new JsonReader(new StringReader(json));
		reader.beginObject();
		ok = readOk(reader);
		if (!ok) {
			System.err.println(json);
			// Récupération du message d'erreur
			throw new Exception(readError(reader));
		}
		reader.endObject();
	}

	/**
	 * Appel à la méthode "channels.history"
	 * Récupére les {@link Message} d'un channel (du plus récent au plus vieux)
	 *
	 * @param channelId identifiant du channel
	 * @param count     nombre de messages dans la réponse
	 * @param inclusive inclusion des messages avec "latest" ou "oldest" timestamp dans la réponse
	 * @param latest    fin de la fourchette de temps des messages à inclure dans la réponse
	 * @param oldest    début de la fourchette de temps des messages à inclure dans la réponse
	 * @param unread    inclusion de la variable "unread_count_display" dans la réponse (?)
	 * @return une paire {@link List} de {@link Message}s et spécifie si d'autres messages peuvent être récupérés par un booléen
	 * @see <a href="https://api.slack.com/methods/channels.history">https://api.slack.com/methods/channels.history</a>
	 */
	@Override
	public Map.Entry<List<Message>, Boolean> fetchMessages(String channelId, @Nullable Integer count, @Nullable Boolean inclusive, @Nullable String latest, @Nullable String oldest, @Nullable Boolean unread) throws Exception {
		Log.info("Récupération des messages du channel \"" + channelId + "\"");
		//  Construction des paramètres optionnels
		Map<String, String> parameters = new HashMap<>();
		parameters.put(CHANNEL, channelId);
		if (count != null) parameters.put(COUNT, count.toString());
		if (inclusive != null) parameters.put(INCLUSIVE, inclusive.toString());
		if (latest != null) parameters.put(LATEST, latest);
		if (oldest != null) parameters.put(OLDEST, oldest);
		if (unread != null) parameters.put(UNREAD, unread.toString());
		// Construction de l'URL
		buildUrl(METHOD_CHANNELS_HISTORY, parameters, false);

		List<Message> messages = null;
		Boolean hasMore = null;
		boolean ok;
		String name;
		String json;
		// Lecture de l'URL
		json = readUrl();
		reader = new JsonReader(new StringReader(json));
		reader.beginObject();
		ok = readOk(reader);
		if (ok) {
			while (reader.hasNext()) {
				name = reader.nextName();
				switch (name) {
					case MESSAGES:
						messages = readMessages(reader);
						break;
					case HAS_MORE:
						hasMore = reader.nextBoolean();
						break;
					default:
						reader.skipValue();
						break;
				}
			}
		} else {
			System.err.println(json);
			// Récupération du message d'erreur
			throw new Exception(readError(reader));
		}
		reader.endObject();
		reader.close();
		return new AbstractMap.SimpleEntry<>(messages, hasMore);
	}

	/**
	 * Appel à la méthode "users.setPresence"
	 * Edite manuellement la présence de l'utilisateur (ici, le bot)
	 *
	 * @param presence {@code true} pour présent (auto), {@code false} pour absent
	 * @return la réponse à la requête
	 * @see <a href="https://api.slack.com/methods/users.setPresence/">https://api.slack.com/methods/users.setPresence/</a>
	 */
	@Override
	public String setPresence(boolean presence) throws Exception {
		Log.info("Edition de la présence : " + presence);
		// Construction des paramètres
		Map<String, String> parameters = new HashMap<>();
		if (presence)
			parameters.put(PRESENCE, AUTO);
		else
			parameters.put(PRESENCE, AWAY);
		// Construction de l'URL
		buildUrl(METHOD_SET_PRESENCE, parameters, true);
		// Lecture de l'URL
		return readUrl();
	}

	//************************************************************//
	//	   METHODES NE FAISANT PAS APPEL DIRECTEMENT A L'API      //
	//************************************************************//

	/**
	 * Récupération de tous les fichiers
	 *
	 * @param channelId identifiant du channel
	 * @param count     fichiers à récupérer par appel à la méthode "files.list"
	 * @param userId    identifiant de l'utilisateur sur lequel filtrer
	 * @return une liste de fichiers
	 * @throws Exception si une erreur est levée lors de la récupération des fichiers
	 */
	@Override
	public List<beans.File> listAllFiles(@Nullable String channelId, @Nullable Integer count, @Nullable String userId) throws Exception {
		Log.info("Enumération de tous les fichiers");
		List<beans.File> files = new ArrayList<>();
		Integer page = 1;
		Boolean hasNextPage = true;
		Paging paging;

		while (hasNextPage) {
			Map.Entry<List<File>, Paging> fetchedFiles = listFiles(channelId, count, page, userId);
			paging = fetchedFiles.getValue();
			files.addAll(fetchedFiles.getKey());
			hasNextPage = !Objects.equals(paging.getPage(), paging.getPages());
			page++;
		}
		return files;
	}

	/**
	 * Récupération de tous les messages du channel
	 *
	 * @param channelId identifiant du channel
	 * @param count     messages à récupérer par appel à la méthode "channels.history"
	 * @param oldest    timestamp du plus vieux à message à récupérer
	 * @return une liste de messages
	 * @throws Exception si une erreur est levée lors de la récupération des fichiers
	 */
	@Override
	public List<Message> fetchAllMessages(String channelId, @Nullable Integer count, @Nullable Long oldest) throws Exception {
		Log.info("Récupération de tous les messages du channel \"" + channelId + "\"");
		List<Message> messages = new ArrayList<>();
		String oldestTimestamp = null;
		if (oldest != null)
			oldestTimestamp = oldest.toString();
		String lastTs = null;
		boolean hasMore = true;
		while (hasMore) {
			Map.Entry<List<Message>, Boolean> messagesFetched = fetchMessages(channelId, count, null, lastTs, oldestTimestamp, null);
			hasMore = messagesFetched.getValue();
			if (hasMore)
				lastTs = messagesFetched.getKey().get(messagesFetched.getKey().size() - 1).getTimestamp();
			messages.addAll(messagesFetched.getKey());
		}
		return messages;
	}

	/**
	 * Retourne le channel correspondant à l'identifiant passé en paramètre
	 *
	 * @param id identifiant du channel à rechercher
	 * @return le channel correspondant (si trouvé), {@code null} sinon
	 * @throws Exception si une erreur est levée lors de la récupération des channels
	 */
	@Override
	public Channel getChannelById(String id) throws Exception {
		List<Channel> channels = this.listChannels();
		Optional<Channel> channel = channels.stream().filter(ch -> ch.getId().equals(id)).findFirst();
		return channel.orElse(null);
	}

	/**
	 * Retourne le channel correspondant au nom passé en paramètre
	 *
	 * @param name nom du channel à rechercher
	 * @return le channel correspondant (si trouvé), {@code null} sinon
	 * @throws Exception si une erreur est levée lors de la récupération des channels
	 */
	@Override
	public Channel getChannelByName(String name) throws Exception {
		List<Channel> channels = this.listChannels();
		Optional<Channel> channel = channels.stream().filter(ch -> ch.getName().equals(name)).findFirst();
		return channel.orElse(null);
	}

	/**
	 * Retourne l'utilisateur correspondant à l'identifiant passé en paramètre
	 *
	 * @param id identifiant de l'utilisateur à rechercher
	 * @return l'utilisateur correspondant (si trouvé) {@code null} sinon
	 * @throws Exception si une erreur est levée lors de la récupération des utilisateurs
	 */
	@Override
	public Member getMemberById(String id) throws Exception {
		List<Member> members = this.listMembers();
		Optional<Member> member = members.stream().filter(m -> m.getId().equals(id)).findFirst();
		return member.orElse(null);
	}

	/**
	 * Retourne l'utilisateur correspondant au nom (username) passé en paramètre
	 *
	 * @param name nom (username) de l'utilisateur à rechercher
	 * @return l'utilisateur correspondant (si trouvé) {@code null} sinon
	 * @throws Exception si une erreur est levée lors de la récupération des utilisateurs
	 */
	@Override
	public Member getMemberByName(String name) throws Exception {
		List<Member> members = this.listMembers();
		Optional<Member> member = members.stream().filter(m -> m.getName().equals(name)).findFirst();
		return member.orElse(null);
	}
}
