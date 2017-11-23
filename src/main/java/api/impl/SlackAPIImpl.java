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

public class SlackAPIImpl implements SlackAPI {
	private boolean bot;
	
	private JsonReader reader = null;
	
	private String stringUrl = "";
	private String token = "";
	private String botToken = "";
	
	/**
	 * @param bot spécifie certains appels à l'API doivent s'effectuer en tant que bot
	 */
	public SlackAPIImpl(boolean bot) {
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
	
	@Override
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
	@Override
	public String test() throws Exception {
		Log.info("Test de l'API");
		buildUrl(Methods.METHOD_API_TEST, null, false);
		return readUrl();
	}
	
	@Override
	public String authentificationTest() throws Exception {
		Log.info("Test de l'authentification");
		buildUrl(METHOD_AUTHENTIFICATION_TEST, null, false);
		return readUrl();
	}
	
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
	
	@Override
	public void deleteFile(String fileId) throws Exception {
		Log.info("Suppression d'un fichier");
		// Construction des paramètres
		Map<String, String> parameters = new HashMap<>();
		parameters.put(FILE, fileId);
		// Construction de l'URL
		buildUrl(METHOD_FILES_DELETE, parameters, false);
		// Lecture de l'URL
		String json = readUrl();
		reader = new JsonReader(new StringReader(json));
		reader.beginObject();
		boolean ok = readOk(reader);
		if (!ok) {
			System.err.println(json);
			// Récupération du message d'erreur
			throw new Exception(readError(reader));
		}
		reader.endObject();
	}
	
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
	
	@Override
	public void deleteMessage(String channelId, String ts, @Nullable Boolean asUser) throws Exception {
		Log.info("Suppression d'un message");
		// Construction des paramètres
		Map<String, String> parameters = new HashMap<>();
		parameters.put(CHANNEL, channelId);
		parameters.put(TS, ts);
		if (asUser != null) parameters.put(AS_USER, asUser.toString());
		// Construction de l'URL
		buildUrl(METHOD_CHAT_DELETE, parameters, false);
		// Lecture de l'URL
		String json = readUrl();
		reader = new JsonReader(new StringReader(json));
		reader.beginObject();
		boolean ok = readOk(reader);
		if (!ok) {
			System.err.println(json);
			// Récupération du message d'erreur
			throw new Exception(readError(reader));
		}
		reader.endObject();
	}
	
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
	
	@Override
	public void updateMEssage(String channelId, String text, String ts, @Nullable Boolean asUser, @Nullable Attachment[] attachments, @Nullable Boolean linkNames, @Nullable String parse) throws Exception {
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
	}
	
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
	
	
	@Override
	public void removeReaction(String name, @Nullable String channelId, @Nullable String file, @Nullable String fileComment, @Nullable String timestamp) throws Exception {
		Log.info("Suppression d'une réaction");
		boolean ok;
		// Construction des paramètres optionnels
		Map<String, String> parameters = new HashMap<>();
		parameters.put(NAME, name);
		if (channelId != null) parameters.put(CHANNEL, channelId);
		if (file != null) parameters.put(FILE, file);
		if (fileComment != null) parameters.put(FILE_COMMENT, fileComment);
		if (timestamp != null) parameters.put(TIMESTAMP, timestamp);
		// Construction de l'URL
		buildUrl(METHOD_REACTION_REMOVE, parameters, bot);
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
	
	@Override
	public Channel getChannelById(String id) throws Exception {
		List<Channel> channels = this.listChannels();
		Optional<Channel> channel = channels.stream().filter(ch -> ch.getId().equals(id)).findFirst();
		return channel.orElse(null);
	}
	
	@Override
	public Channel getChannelByName(String name) throws Exception {
		List<Channel> channels = this.listChannels();
		Optional<Channel> channel = channels.stream().filter(ch -> ch.getName().equals(name)).findFirst();
		return channel.orElse(null);
	}
	
	@Override
	public Member getMemberById(String id) throws Exception {
		List<Member> members = this.listMembers();
		Optional<Member> member = members.stream().filter(m -> m.getId().equals(id)).findFirst();
		return member.orElse(null);
	}
	
	@Override
	public Member getMemberByName(String name) throws Exception {
		List<Member> members = this.listMembers();
		Optional<Member> member = members.stream().filter(m -> m.getName().equals(name)).findFirst();
		return member.orElse(null);
	}
}
