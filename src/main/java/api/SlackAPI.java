package api;

import beans.Channel;
import beans.File;
import beans.Member;
import beans.Paging;
import beans.events.Message;
import com.google.gson.stream.JsonReader;
import com.sun.istack.internal.Nullable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.*;

import static api.Methods.*;
import static api.Attributes.*;
import static converter.Converter.*;

public class SlackAPI {
    private static final Logger Log = LogManager.getLogger(SlackAPI.class);

    private static final String PROPERTIES_FILE = "slack.properties";
    private static final String PROPERTY_BOT_TOKEN = "BOT_TOKEN";
    private static final String PROPERTY_TOKEN = "TOKEN";

    private static final String API_URL = "https://slack.com/api/";

    private JsonReader reader;

    private String stringUrl;
    private String token;
    private String botToken;

    public SlackAPI() {
        build();
    }

    //	METHODES
    private void build() {
        Log.info("Lecture du fichier de configuration...");
        Properties properties = new Properties();

        ClassLoader classLoader = getClass().getClassLoader();
        InputStream propertyFile = null;
        URL url = classLoader.getResource(PROPERTIES_FILE);
        System.out.println(url);
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
        Log.info("Lecture terminée.");
    }

    /**
     * Construction de l'URL de l'appel à l'API
     *
     * @param method             méthode de l'API
     * @param optionalParameters paramètres optionnels de la méthode
     */
    private void buildUrl(String method, Map<String, String> optionalParameters, boolean bot) {
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
    private String readUrl() throws Exception {
        Log.info("GET : - " + stringUrl);
        BufferedReader reader = null;
        try {
            URL url = new URL(stringUrl);
            URLConnection conn = url.openConnection();
            //  Récupération des en-têtes
//            Map<String, List<String>> map = conn.getHeaderFields();
//            for (Map.Entry<String, List<String>> entry : map.entrySet()) {
//                System.out.println(entry.getKey() + " : " + entry.getValue());
//            }
            // Récuparation du contenu
            reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder buffer = new StringBuilder();
            int read;
            char[] chars = new char[1024];
            while ((read = reader.read(chars)) != -1)
                buffer.append(chars, 0, read);
            return buffer.toString();
        } finally {
            if (reader != null)
                reader.close();
        }
    }

    //  METHODES D'APPEL A L'API

    /**
     * Appel à la méthode "api.test"
     * Test de l'API
     *
     * @return la réponse à la requête
     * @throws Exception si l'URL est malformée
     */
    public String test() throws Exception {
        Log.info("Test de l'API");
        buildUrl(METHOD_API_TEST, null, false);
        return readUrl();
    }

    /**
     * Appel à la méthode "auth.test"
     * Test de l'authentification
     *
     * @return la réponse à la requête
     * @throws Exception si l'URL est malformée
     */
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
     */
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
                    case URL:
                        map.put(URL, reader.nextString());
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
     */
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
     * @return une paire {@link List} de {@link File}s et de {@link Paging}
     * @throws Exception Si l'URL est mal formée
     */
    public Map.Entry<List<File>, Paging> listFiles(@Nullable String channelId, @Nullable Integer count, @Nullable Integer page, @Nullable String userId) throws Exception {
        Log.info("Enumération des fichiers");
        //  Construction des paramètres optionnels
        Map<String, String> parameters = new HashMap<>();
        if (channelId != null) parameters.put("channel", channelId);
        if (count != null) parameters.put("count", count.toString());
        if (page != null) parameters.put("page", page.toString());
        if (userId != null) parameters.put("user", userId);
        // Construction de l'URL
        buildUrl(METHOD_LIST_FILES, parameters, false);

        List<File> files = null;
        Paging paging = null;
        boolean ok;
        String name;

        // Lecture de l'URL
        String json = readUrl();
        reader = new JsonReader(new StringReader(json));
        reader.beginObject();
        ok = readOk(reader);
        System.out.println(ok);
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
     * Récupération de tous les fichiers
     *
     * @param channelId identifiant du channel
     * @param count     fichiers à récupérer par appel à la méthode "files.list"
     * @param userId    identifiant de l'utilisateur sur lequel filtrer
     * @return une liste de fichiers
     * @throws Exception Si l'URL est malformée
     */
    public List<File> listAllFiles(@Nullable String channelId, @Nullable Integer count, @Nullable String userId) throws Exception {
        Log.info("Enumération de tous les fichiers");
        List<File> files = new ArrayList<>();
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
     * Appel à la méthode "users.list"
     * Liste les membres
     *
     * @return une liste de membre
     * @throws Exception Si l'URL est mal formée
     */
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
     * @param channelId identifiant du channel
     * @param text      texte à envoyer
     * @param iconUrl   URL d'une image utilisée comme icône au message
     * @param username  nom du bot
     * @return la réponse à la réquête
     * @throws Exception si l'URL est malformée
     */
    public String postMessage(String channelId, String text, @Nullable String iconUrl, @Nullable String username) throws Exception {
        Log.info("Envoi d'un message");
        //  Construction des paramètres optionnels
        Map<String, String> parameters = new HashMap<>();
        parameters.put("channel", channelId);
        parameters.put("text", URLEncoder.encode(text, "UTF-8"));
        if (iconUrl != null) parameters.put("icon_url", URLEncoder.encode(iconUrl, "UTF-8"));
        if (username != null) parameters.put("username", username);
        // Construction de l'URL
        buildUrl(METHOD_CHAT_POST_MESSAGE, parameters, false);
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
     */
    public void addReaction(String reactionName, @Nullable String channelId, @Nullable String file, @Nullable String fileComment, @Nullable String timestamp) throws Exception {
        Log.info("Ajout d'une réaction");
        boolean ok;
        // Construction des paramètres optionnels
        Map<String, String> parameters = new HashMap<>();
        parameters.put("name", reactionName);
        if (channelId != null) parameters.put("channel", channelId);
        if (file != null) parameters.put("file", file);
        if (fileComment != null) parameters.put("file_comment", fileComment);
        if (timestamp != null) parameters.put("timestamp", timestamp);
        // Construction de l'URL
        buildUrl(METHOD_REACTION_ADD, parameters, true);
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
     */
    public Map.Entry<List<Message>, Boolean> fetchMessages(String channelId, @Nullable Integer count, @Nullable Boolean inclusive, @Nullable String latest, @Nullable String oldest, @Nullable Boolean unread) throws Exception {
        Log.info("Récupération des messages du channel \"" + channelId + "\"");
        //  Construction des paramètres optionnels
        Map<String, String> parameters = new HashMap<>();
        parameters.put("channel", channelId);
        if (count != null) parameters.put("count", count.toString());
        if (inclusive != null) parameters.put("inclusive", inclusive.toString());
        if (latest != null) parameters.put("latest", latest);
        if (oldest != null) parameters.put("oldest", oldest);
        if (unread != null) parameters.put("unread", unread.toString());
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
     * Récupération de tous les messages du channel
     *
     * @param channelId identifiant du channel
     * @param count     messages à récupérer par appel à la méthode "channels.history"
     * @return une liste de messages
     */
    public List<Message> fetchAllMessages(String channelId, @Nullable Integer count) throws Exception {
        Log.info("Récupération de tous les messages du channel \"" + channelId + "\"");
        List<Message> messages = new ArrayList<>();
        String lastTs = null;
        boolean hasMore = true;
        while (hasMore) {
            Map.Entry<List<Message>, Boolean> messagesFetched = fetchMessages(channelId, count, null, lastTs, null, null);
            hasMore = messagesFetched.getValue();
            lastTs = messagesFetched.getKey().get(messagesFetched.getKey().size() - 1).getTimestamp();
            messages.addAll(messagesFetched.getKey());
        }
        return messages;
    }
}