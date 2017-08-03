package converter;

import beans.*;
import beans.events.Message;
import beans.events.ReactionAdded;
import com.google.gson.stream.JsonReader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static api.Attributes.*;

public class Converter {
    private static final Logger Log = LogManager.getLogger(Converter.class);

    /**
     * Retourne le statut "ok" de la réponse
     * @param reader
     * @return le statut "ok" de la réponse
     * @throws Exception si la réponse n'existe pas, ou ne contient pas d'attribut "ok"
     */
    public static boolean readOk(JsonReader reader) throws Exception {
        String name = reader.nextName();
        if(name.equals(OK))
            return reader.nextBoolean();
        throw new Exception("Pas d'attribut \"ok\"");
    }

    /**
     * Retourne l'erreur de la réponse
     * @param reader
     * @return l'erreur de la réponse
     * @throws Exception si la réponse n'existe pas, ou ne ne contient pas d'attribut "erreur"
     */
    public static String readError(JsonReader reader) throws Exception {
        String name = reader.nextName();
        if(name.equals(ERROR))
            return reader.nextString();
        throw new Exception("Pas d'attribut \"error\"");
    }

    /**
     * Retourne les {@link Channel} dans la réponse
     * @param reader
     * @return les {@link Channel} dans la réponse
     * @throws IOException si la réponse n'existe pas
     */
    public static List<Channel> readChannels(JsonReader reader) throws IOException {
        List<Channel> channels = new ArrayList<>();
        reader.beginArray();
        while(reader.hasNext())
            channels.add(readChannel(reader));
        reader.endArray();
        return channels;
    }

    /**
     * Retourne le {@link Channel} dans la réponse
     * @param reader
     * @return le {@link Channel} dans la réponse
     * @throws IOException si la réponse n'existe pas
     */
    public static Channel readChannel(JsonReader reader) throws IOException {
        String aux;
        String id = null, name = null;
        List<String> members = new ArrayList<>();
        int numMembers = 0;
        reader.beginObject();
        while(reader.hasNext()) {
            aux = reader.nextName();
            switch(aux) {
                case ID:
                    id = reader.nextString();
                    break;
                case NAME:
                    name = reader.nextString();
                    break;
                case MEMBERS:
                    reader.beginArray();
                    while(reader.hasNext())
                        members.add(reader.nextString());
                    reader.endArray();
                    break;
                case NUM_MEMBERS:
                    numMembers = reader.nextInt();
                    break;
                default:
                    reader.skipValue();
                    break;
            }
        }
        reader.endObject();
        return new Channel(id, name, numMembers, members);
    }

    public static Message readMessageSent(JsonReader reader) throws IOException {
        String name;
        String user = null, channel = null, text = null, subtype = null, timestamp = null;
        while(reader.hasNext()) {
            name = reader.nextName();
            switch(name) {
                case CHANNEL :
                    channel = reader.nextString();
                    break;
                case TEXT :
                    text = reader.nextString();
                    break;
                case TS :
                    timestamp = reader.nextString();
                    break;
                case SUBTYPE:
                    subtype = reader.nextString();
                    break;
                case USER :
                    user = reader.nextString();
                    break;
                default :
                    reader.skipValue();
            }
        }
        return new Message(user, channel, text, subtype, timestamp, null);
    }

    public static ReactionAdded readReactionAdded(JsonReader reader) throws IOException {
        String name;
        String user = null, itemTs = null, reaction = null, timestamp = null;
        while(reader.hasNext()) {
            name = reader.nextName();
            switch(name) {
                case ITEM :
                    reader.beginObject();
                    while(reader.hasNext()){
                        name = reader.nextName();
                        switch(name) {
                            case TS:
                                itemTs = reader.nextString();
                                break;
                            default:
                                reader.skipValue();
                                break;
                        }
                    }
                    reader.endObject();
                    break;
                case REACTION :
                    reaction = reader.nextString();
                    break;
                case TS :
                    timestamp = reader.nextString();
                    break;
                case USER :
                    user = reader.nextString();
                    break;
                default :
                    reader.skipValue();
                    break;
            }
        }
        return new ReactionAdded(user, itemTs, reaction, timestamp);
    }

    /**
     * Retourne les {@link File} dans la réponse
     * @param reader
     * @return les {@link File} dans la réponse
     * @throws IOException si la réponse n'existe pas
     */
    public static List<File> readFiles(JsonReader reader) throws IOException {
        List<File> files = new ArrayList<>();
        reader.beginArray();
        while(reader.hasNext())
            files.add(readFile(reader));
        reader.endArray();
        return files;
    }

    /**
     * Retourne le {@link File} dans la réponse
     * @param reader
     * @return le {@link File} dans la réponse
     * @throws IOException si la réponse n'existe pas
     */
    public static File readFile(JsonReader reader) throws IOException {
        String aux;
        long created = 0;
        String id = null, name = null, title = null, user = null;
        reader.beginObject();
        while(reader.hasNext()) {
            aux = reader.nextName();
            switch(aux) {
                case ID:
                    id = reader.nextString();
                    break;
                case CREATED:
                    created = reader.nextLong();
                    break;
                case NAME:
                    name = reader.nextString();
                    break;
                case TITLE:
                    title = reader.nextString();
                    break;
                case USER:
                    user = reader.nextString();
                    break;
                default:
                    reader.skipValue();
                    break;
            }
        }
        reader.endObject();
        return new File(id, created, name, title, user);
    }

    public static Paging readPaging(JsonReader reader) throws IOException {
        String name;
        Integer count = null, total = null, page = null, pages = null;
        reader.beginObject();
        while(reader.hasNext()) {
            name = reader.nextName();
            switch(name) {
                case COUNT:
                    count = reader.nextInt();
                    break;
                case TOTAL:
                    total = reader.nextInt();
                    break;
                case PAGE:
                    page = reader.nextInt();
                    break;
                case PAGES:
                    pages = reader.nextInt();
                    break;
                default:
                    reader.skipValue();
                    break;
            }
        }
        reader.endObject();
        return new Paging(count, total, page, pages);
    }

    /**
     * Retourne les {@link Member} dans la réponse
     * @param reader
     * @return les {@link Member} dans la réponse
     * @throws IOException si la réponse n'existe pas
     */
    public static List<Member> readMembers(JsonReader reader) throws IOException {
        List<Member> users = new ArrayList<>();
        reader.beginArray();
        while(reader.hasNext())
            users.add(readMember(reader));
        reader.endArray();
        return users;
    }

    /**
     * Retourne le {@link Member} dans la réponse
     * @param reader
     * @return le {@link Member} dans la réponse
     * @throws IOException si la réponse n'existe pas
     */
    public static Member readMember(JsonReader reader) throws IOException {
        String aux;
        String id = null, name = null, realName = null;
        reader.beginObject();
        while(reader.hasNext()) {
            aux = reader.nextName();
            switch(aux) {
                case ID:
                    id = reader.nextString();
                    break;
                case NAME:
                    name = reader.nextString();
                    break;
                case REAL_NAME:
                    realName = reader.nextString();
                    break;
                default:
                    reader.skipValue();
                    break;
            }
        }
        reader.endObject();
        return new Member(id, name, realName);
    }

    public static List<Message> readMessages(JsonReader reader) throws IOException {
        List<Message> messages = new ArrayList<>();
        reader.beginArray();
        while(reader.hasNext()) {
            messages.add(readMessage(reader));
        }
        reader.endArray();
        return messages;
    }

    /**
     * Retourne l'identifiant de SELF
     * @param reader
     * @return l'identifiant de SELF
     * @throws IOException si la réponse n'existe pas
     */
    public static String readSelf(JsonReader reader) throws IOException {
        String name, self = null;
        reader.beginObject();
        while(reader.hasNext()) {
            name = reader.nextName();
            switch(name) {
                case ID :
                    self = reader.nextString();
                    break;
                default:
                    reader.skipValue();
                    break;
            }
        }
        reader.endObject();
        return self;
    }

    public static Message readMessage(JsonReader reader) throws IOException {
        String name;
        String user = null, channel = null, text = null, subtype = null, timestamp = null;
        List<Reaction> reactions = null;
        reader.beginObject();
        while(reader.hasNext()) {
            name = reader.nextName();
            try {
                switch (name) {
                    case CHANNEL:
                        channel = reader.nextString();
                        break;
                    case REACTIONS:
                        reactions = readReactions(reader);
                        break;
                    case TEXT:
                        text = reader.nextString();
                        break;
                    case TS:
                        timestamp = reader.nextString();
                        break;
                    case SUBTYPE:
                        subtype = reader.nextString();
                        break;
                    case USER:
                        user = reader.nextString();
                        break;
                    default:
                        reader.skipValue();
                }
            }catch (IllegalStateException e) {
                e.printStackTrace();
                reader.skipValue();
            }
        }
        reader.endObject();
        return new Message(user, channel, text, subtype, timestamp, reactions);
    }

    public static List<Reaction> readReactions(JsonReader reader) throws IOException {
        List<Reaction> reactions = new ArrayList<>();
        reader.beginArray();
        while(reader.hasNext())
            reactions.add(readReaction(reader));
        reader.endArray();
        return reactions;
    }

    public static Reaction readReaction(JsonReader reader) throws IOException {
        String aux, name = null;
        Set<String> users = null;
        Integer count = null;
        reader.beginObject();
        while(reader.hasNext()) {
            aux = reader.nextName();
            switch(aux) {
                case NAME:
                    name = reader.nextString();
                    break;
                case USERS:
                    users = readReactionUsers(reader);
                    break;
                case COUNT:
                    count = reader.nextInt();
                    break;
                default:
                    reader.skipValue();
                    break;
            }
        }
        reader.endObject();
        return new Reaction(name, count, users);
    }

    public static Set<String> readReactionUsers(JsonReader reader) throws IOException {
        Set<String> users = new HashSet<>();
        reader.beginArray();
        while(reader.hasNext())
            users.add(reader.nextString());
        reader.endArray();
        return users;
    }
}
