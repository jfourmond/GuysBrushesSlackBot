package bot;

import api.SlackAPI;
import beans.Channel;
import beans.Member;
import beans.Reaction;
import beans.events.Message;
import beans.events.ReactionAdded;
import com.google.gson.stream.JsonReader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.*;

import java.io.IOException;
import java.io.StringReader;
import java.util.*;
import java.util.concurrent.*;

import static converter.Converter.*;

/**
 * WebSocket pour le bot "Guy"
 */
@WebSocket
public class Guy {
    private static final Logger Log = LogManager.getLogger(Guy.class);

    private static final String MESSAGE = "message";
    private static final String REACTION_ADDED = "reaction_added";
    private static final String TYPE = "type";

    private Map<String, Boolean> saidHi;

    // API SLACK
    private SlackAPI api;

    //  INFORMATION DE SESSION
    private String botId;
    private List<Member> members;       // Membres du Slack
    private List<Channel> channels;     // Channels où le bot est présent
    private Map<String, List<Reaction>> reactions;

    //  INFORMATION DE DUREE
    private int duration;
    private TimeUnit unit;

    private final CountDownLatch closeLatch;
    private Session session;

    public Guy(String botId) throws Exception {
        Log.info("Création de Guy : " + botId);
        this.botId = botId;

        initialisation();

        this.closeLatch = new CountDownLatch(1);
    }

    private void initialisation() throws Exception {
        //  API
        api = new SlackAPI(true);
        //  RECHERCHE DES MEMBRES
        saidHi = new HashMap<>();
        members = api.listMembers();
        members.forEach(m -> saidHi.put(m.getId(), false));
        //  RECHERCHE DES CHANNELS
        channels = new ArrayList<>();
        api.listChannels().forEach(channel -> {
            if (channel.getMembers().contains(botId))
                channels.add(channel);
        });
        // RECHERCHE DES REACTIONS
        reactions = new HashMap<>();
        //  Récupération des messages
        channels.forEach(channel -> {
            try {
                List<Message> messages = api.fetchAllMessages(channel.getId(), null);
                List<Reaction> reactionsProv = new ArrayList<>();
                // Traitement des réactions
                messages.forEach(message -> {
                    List<Reaction> reactionsMessages = message.getReactions();
                    if(reactionsMessages != null) {
                        reactionsMessages.forEach(reaction -> {
                            Optional<Reaction> reac = reactionsProv.stream().filter(r -> reaction.getName().equals(r.getName())).findFirst();
                            if (reac.isPresent()) {
                                reac.get().setCount(reac.get().getCount() + reaction.getCount());
                                reac.get().getUsers().addAll(reaction.getUsers());
                            } else
                                reactionsProv.add(reaction);
                        });
                    }
                });
                reactionsProv.sort(Comparator.comparingInt(Reaction::getCount));
                reactions.put(channel.getId(), reactionsProv);
            } catch(Exception e) {
                e.printStackTrace();
            }
        });
    }

    public boolean awaitClose(int duration, TimeUnit unit) throws InterruptedException {
        this.duration = duration;
        this.unit = unit;
        return this.closeLatch.await(duration, unit);
    }

    @OnWebSocketError
    public void onError(Throwable t) {
        System.out.println("Error: " + t.getMessage());
    }

    @OnWebSocketConnect
    public void onConnect(Session session) {
        Log.info("Connexion");
        this.session = session;

        channels.forEach(channel -> {
            int reactionCount = 0;
            for(Reaction reaction : reactions.get(channel.getId()))
                reactionCount += reaction.getCount();


            Future<Void> fut0 = null;
            try {
                fut0 = sendMessage("Bonjour tout le monde ! :heart: Je ne serais présent que pendant " + duration + " " + unit.name().toLowerCase() + " !", channel.getId());
                fut0.get(2, TimeUnit.SECONDS);
            } catch (ExecutionException | InterruptedException e) {
                // L'envoi a échoué
                e.printStackTrace();
            } catch (TimeoutException e) {
                // Timeout
                e.printStackTrace();
                fut0.cancel(true);
            }

            try {
                fut0 = sendMessage(reactionCount + " réactions dans le channel !", channel.getId());
                fut0.get(2, TimeUnit.SECONDS);
            } catch (ExecutionException | InterruptedException e) {
                // L'envoi a échoué
                e.printStackTrace();
            } catch (TimeoutException e) {
                // Timeout
                e.printStackTrace();
                fut0.cancel(true);
            }
        });
    }

    @OnWebSocketMessage
    public void onMessage(String message) {
        Log.info("Event reçu");
        System.out.println(message);
        Message M = null;
        ReactionAdded RA = null;
        JsonReader reader = new JsonReader(new StringReader(message));
        String name;
        try {
            reader.beginObject();
            while (reader.hasNext()) {
                name = reader.nextName();
                switch(name) {
                    case TYPE:
                        switch(reader.nextString()) {
                            case MESSAGE:
                                Log.info("Event est : Message");
                                M = readMessageSent(reader);
                                break;
                            case REACTION_ADDED:
                                Log.info("Event est : Ajout d'une réaction");
                                RA = readReactionAdded(reader);
                                System.out.println(RA);
                                break;
                        }
                        break;
                    default:
                        reader.skipValue();
                        break;
                }
            }
            reader.endObject();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (M != null && M.getSubtype() == null && !M.getUser().equals(botId)) {
            // TRAITEMENT DU MESSAGE
            Message finalM = M;
            Optional<Member> member = members.stream().filter(m -> m.getId().equals(finalM.getUser())).findFirst();
            if (member.isPresent() && !saidHi.get(member.get().getId())) {
                Future<Void> fut = null;
                try {
                    fut = session.getRemote().sendStringByFuture(
                            "{ " +
                                    "\"type\" : \"message\", " +
                                    "\"text\" : \"Salut " + member.get().getName() + " ! :wave: \"," +
                                    "\"channel\" : \"" + M.getChannel() + "\"" +
                                    "}");
                    fut.get(2, TimeUnit.SECONDS);
                    if (fut.isDone())
                        saidHi.put(member.get().getId(), true);
                } catch (ExecutionException | InterruptedException e) {
                    // L'envoi a échoué
                    e.printStackTrace();
                } catch (TimeoutException e) {
                    // Timeout
                    e.printStackTrace();
                    if (fut != null)
                        fut.cancel(true);
                }
            } else {
                try {
                    if (hasBeenCited(M)) {
                        api.addReaction("wave", M.getChannel(), null, null, M.getTimestamp());
                    } else {
                        api.addReaction("neutral_face", M.getChannel(), null, null, M.getTimestamp());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        if(RA != null) {
            // TRAITEMENT DE LA REACTION AJOUTE
            List<Reaction> channelReactions = reactions.get(RA.getChannel());
            ReactionAdded finalRA = RA;
            Optional<Reaction> reactionFound = channelReactions.stream().filter(reaction -> reaction.getName().equals(finalRA.getReaction())).findFirst();
            if(reactionFound.isPresent()) {
                reactionFound.get().addUser(RA.getUserId());
                reactionFound.get().incrementCount();
            } else {
                Set<String> users = new HashSet<>();
                users.add(RA.getUserId());
                channelReactions.add(new Reaction(RA.getReaction(), 1, users));
            }
            Log.info("Ajout d'une réaction");
        }
    }

    @OnWebSocketClose
    public void onClose(int statusCode, String reason) {
        Log.info("Fermeture de la connexion : " + statusCode + " - " + reason);

        //  TODO Rapport d'exécution

        this.session = null;
        this.closeLatch.countDown();
    }

    private Future<Void> sendMessage(String text, String channelId) {
        return session.getRemote().sendStringByFuture(
                "{ " +
                        "\"type\" : \"message\", " +
                        "\"text\" : \"" + text + "\"," +
                        "\"channel\" : \"" + channelId + "\"" +
                        "}");
    }

    private boolean hasBeenCited(Message M) {
        return M.getText().contains("<@U6E9ZNAJC>");
    }
}
