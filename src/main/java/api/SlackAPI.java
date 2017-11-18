package api;

import beans.Attachment;
import beans.File;
import beans.Member;
import beans.Paging;
import beans.channels.Channel;
import beans.events.Message;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

public interface SlackAPI {
    Logger Log = LogManager.getLogger(SlackAPI.class);

    String PROPERTIES_FILE = "slack.properties";
    String PROPERTY_BOT_TOKEN = "BOT_TOKEN";
    String PROPERTY_TOKEN = "TOKEN";

    String API_URL = "https://slack.com/api/";

    boolean bot = true;


    //  GETTERS
    boolean isBot();

    //  SETTERS
    void setBot(boolean bot);

    //********************//
    //	   METHODES       //
    //********************//

    void build();

    /**
     * Construction de l'URL de l'appel à l'API
     *
     * @param method             méthode de l'API
     * @param optionalParameters paramètres optionnels de la méthode
     * @param bot                spécifie si l'URL doit s'exécuter en tant que Bot ou Application (modifie le token)
     */
    void buildUrl(String method, Map<String, String> optionalParameters, boolean bot);

    /**
     * GET de l'URL
     *
     * @return le fichier de l'URL (Json dans le cas présent)
     * @throws Exception si l'URL est malformée
     */
    String readUrl() throws Exception;

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
    String test() throws Exception;

    /**
     * Appel à la méthode "auth.test"
     * Test de l'authentification
     *
     * @return la réponse à la requête
     * @throws Exception si l'URL est malformée
     * @see <a href="https://api.slack.com/methods/auth.test">https://api.slack.com/methods/auth.test</a>
     */
    String authentificationTest() throws Exception;

    /**
     * Appel à la méthode "rtm.connect"
     * Demande une url de connection pour WebSocket
     *
     * @return une {@link Map} possédant deux clés, URL et ID
     * @throws Exception si l'URL est malformée
     * @see <a href="https://api.slack.com/methods/rtm.connect">https://api.slack.com/methods/rtm.connect</a>
     */
    Map<String, String> connect() throws Exception;

    /**
     * Appel à la méthode "channels.list"
     * Liste les channels
     *
     * @return une liste des channels
     * @throws Exception si l'URL est mal formée
     * @see <a href="https://api.slack.com/methods/channels.list">https://api.slack.com/methods/channels.list</a>
     */
    List<Channel> listChannels() throws Exception;

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
     * @see <a href="https://api.slack.com/methods/files.list">https://api.slack.com/methods/files.list</a>
     */
    Map.Entry<List<File>, Paging> listFiles(@Nullable String channelId, @Nullable Integer count, @Nullable Integer page, @Nullable String userId) throws Exception;

    /**
     * Appel à la méthode "users.list"
     * Liste les membres
     *
     * @return une liste de membre
     * @throws Exception Si l'URL est mal formée
     * @see <a href="https://api.slack.com/methods/users.list">https://api.slack.com/methods/users.list</a>
     */
    List<Member> listMembers() throws Exception;

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
    String postMessage(String channelId, String text, @Nullable Boolean asUser, @Nullable Attachment[] attachments,
                       @Nullable String iconEmoji, @Nullable String iconUrl, @Nullable Boolean replyBroadcast,
                       @Nullable String threadTS, @Nullable Boolean unfurlLinks, @Nullable Boolean unfurlMedia,
                       @Nullable String username) throws Exception;

    /**
     * Appel à la méthode "chat.meMessage"
     * Poste un meMessage dans le channel
     *
     * @param channelId identifiant du channel
     * @param text      text à envoyer comme un meMessage
     * @return la réponse à la requête
     * @throws Exception si l'URL est malformée
     */
    String meMessage(String channelId, String text) throws Exception;

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
    boolean updateMEssage(String channelId, String text, String ts, @Nullable Boolean asUser, @Nullable Attachment[] attachments, @Nullable Boolean linkNames, @Nullable String parse) throws Exception;

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
    String postEphemeral(String channelId, String text, String userId, @Nullable Boolean asUser, @Nullable Attachment[] attachments, @Nullable Boolean linkNames, @Nullable String parse) throws Exception;

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
    void addReaction(String reactionName, @Nullable String channelId, @Nullable String file, @Nullable String fileComment, @Nullable String timestamp) throws Exception;

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
    Map.Entry<List<Message>, Boolean> fetchMessages(String channelId, @Nullable Integer count, @Nullable Boolean inclusive, @Nullable String latest, @Nullable String oldest, @Nullable Boolean unread) throws Exception;

    /**
     * Appel à la méthode "users.setPresence"
     * Edite manuellement la présence de l'utilisateur (ici, le bot)
     *
     * @param presence {@code true} pour présent (auto), {@code false} pour absent
     * @return la réponse à la requête
     * @see <a href="https://api.slack.com/methods/users.setPresence/">https://api.slack.com/methods/users.setPresence/</a>
     */
    String setPresence(boolean presence) throws Exception;

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
    List<File> listAllFiles(@Nullable String channelId, @Nullable Integer count, @Nullable String userId) throws Exception;

    /**
     * Récupération de tous les messages du channel
     *
     * @param channelId identifiant du channel
     * @param count     messages à récupérer par appel à la méthode "channels.history"
     * @param oldest    timestamp du plus vieux à message à récupérer
     * @return une liste de messages
     * @throws Exception si une erreur est levée lors de la récupération des fichiers
     */
    List<Message> fetchAllMessages(String channelId, @Nullable Integer count, @Nullable Long oldest) throws Exception;

    /**
     * Retourne le channel correspondant à l'identifiant passé en paramètre
     *
     * @param id identifiant du channel à rechercher
     * @return le channel correspondant (si trouvé), {@code null} sinon
     * @throws Exception si une erreur est levée lors de la récupération des channels
     */
    Channel getChannelById(String id) throws Exception;


    /**
     * Retourne le channel correspondant au nom passé en paramètre
     *
     * @param name nom du channel à rechercher
     * @return le channel correspondant (si trouvé), {@code null} sinon
     * @throws Exception si une erreur est levée lors de la récupération des channels
     */
    Channel getChannelByName(String name) throws Exception;

    /**
     * Retourne l'utilisateur correspondant à l'identifiant passé en paramètre
     *
     * @param id identifiant de l'utilisateur à rechercher
     * @return l'utilisateur correspondant (si trouvé) {@code null} sinon
     * @throws Exception si une erreur est levée lors de la récupération des utilisateurs
     */
    Member getMemberById(String id) throws Exception;

    /**
     * Retourne l'utilisateur correspondant au nom (username) passé en paramètre
     *
     * @param name nom (username) de l'utilisateur à rechercher
     * @return l'utilisateur correspondant (si trouvé) {@code null} sinon
     * @throws Exception si une erreur est levée lors de la récupération des utilisateurs
     */
    Member getMemberByName(String name) throws Exception;
}
