package converter;

import beans.*;
import beans.channels.Channel;
import beans.channels.ChannelType;
import beans.events.Message;
import beans.events.ReactionAdded;
import com.google.gson.stream.JsonReader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static api.Attributes.*;

public class Converter {
	/**
	 * Retourne le statut "ok" du JSON
	 *
	 * @param reader le JsonReader précédemment utilisé (pour pouvoir poursuivre le traitement)
	 * @return le statut "ok" du JSON
	 * @throws Exception si une erreur est détectée lors de la lecture, ou s'il n'existe pas d'attribut "ok"
	 */
	public static boolean readOk(JsonReader reader) throws Exception {
		String name = reader.nextName();
		if (name.equals(OK))
			return reader.nextBoolean();
		throw new Exception("Pas d'attribut \"ok\"");
	}
	
	/**
	 * Retourne l'erreur du JSON
	 *
	 * @param reader le JsonReader précédemment utilisé (pour pouvoir poursuivre le traitement)
	 * @return l'erreur du JSON
	 * @throws Exception si une erreur est détectée lors de la lecture, ou s'il n'existe pas d'attribut "error"
	 */
	public static String readError(JsonReader reader) throws Exception {
		String name = reader.nextName();
		if (name.equals(ERROR))
			return reader.nextString();
		throw new Exception("Pas d'attribut \"error\"");
	}
	
	/**
	 * Lecture des channels dans le JSON
	 *
	 * @param reader le JsonReader précédemment utilisé (pour pouvoir poursuivre le traitement)
	 * @return une liste de {@link Channel}
	 * @throws IOException si une erreur est détectée lors de la lecture
	 */
	public static List<Channel> readChannels(JsonReader reader) throws IOException {
		List<Channel> channels = new ArrayList<>();
		reader.beginArray();
		while (reader.hasNext())
			channels.add(readChannel(reader));
		reader.endArray();
		return channels;
	}
	
	/**
	 * Lecture du channel dans le JSON
	 *
	 * @param reader le JsonReader précédemment utilisé (pour pouvoir poursuivre le traitement)
	 * @return un {@link Channel}
	 * @throws IOException si une erreur est détectée lors de la lecture
	 */
	public static Channel readChannel(JsonReader reader) throws IOException {
		String aux;
		String id = null, name = null;
		List<String> members = new ArrayList<>();
		int numMembers = 0;
		ChannelType type;
		reader.beginObject();
		while (reader.hasNext()) {
			aux = reader.nextName();
			switch (aux) {
				case ID:
					id = reader.nextString();
					break;
				case NAME:
					name = reader.nextString();
					break;
				case MEMBERS:
					reader.beginArray();
					while (reader.hasNext())
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
	
	/**
	 * Retourne l'Event "message" du JSON
	 *
	 * @param reader le JsonReader précédemment utilisé (pour pouvoir poursuivre le traitement)
	 * @return l'Event "message" du JSON
	 * @throws IOException si une erreur est détectée lors de la lecture
	 */
	public static Message readMessageSent(JsonReader reader) throws IOException {
		String name;
		String user = null, channel = null, text = null, subtype = null, timestamp = null;
		while (reader.hasNext()) {
			name = reader.nextName();
			switch (name) {
				case CHANNEL:
					channel = reader.nextString();
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
		}
		return new Message(user, channel, text, subtype, timestamp, null);
	}
	
	/**
	 * Retourne l'Event "réaction_added" du JSON
	 *
	 * @param reader le JsonReader précédemment utilisé (pour pouvoir poursuivre le traitement)
	 * @return une {@link ReactionAdded}
	 * @throws IOException si une erreur est détectée lors de la lecture
	 */
	public static ReactionAdded readReactionAdded(JsonReader reader) throws IOException {
		String name;
		String user = null, itemTs = null, itemType = null, itemChannel = null, reaction = null, timestamp = null, itemUser = null;
		while (reader.hasNext()) {
			name = reader.nextName();
			switch (name) {
				case ITEM:
					reader.beginObject();
					while (reader.hasNext()) {
						name = reader.nextName();
						switch (name) {
							case CHANNEL:
								itemChannel = reader.nextString();
								break;
							case TS:
								itemTs = reader.nextString();
								break;
							case TYPE:
								itemType = reader.nextString();
								break;
							default:
								reader.skipValue();
								break;
						}
					}
					reader.endObject();
					break;
				case ITEM_USER:
					itemUser = reader.nextString();
					break;
				case REACTION:
					reaction = reader.nextString();
					break;
				case TS:
					timestamp = reader.nextString();
					break;
				case USER:
					user = reader.nextString();
					break;
				default:
					reader.skipValue();
					break;
			}
		}
		return new ReactionAdded(user, itemType, itemChannel, itemTs, reaction, timestamp, itemUser);
	}
	
	/**
	 * Lecture des fichiers dans le JSON
	 *
	 * @param reader le JsonReader précédemment utilisé (pour pouvoir poursuivre le traitement)
	 * @return une liste de {@link File}
	 * @throws IOException si une erreur est détectée lors de la lecture
	 */
	public static List<File> readFiles(JsonReader reader) throws IOException {
		List<File> files = new ArrayList<>();
		reader.beginArray();
		while (reader.hasNext())
			files.add(readFile(reader));
		reader.endArray();
		return files;
	}
	
	/**
	 * Lecture du fichier dans le JSON
	 *
	 * @param reader le JsonReader précédemment utilisé (pour pouvoir poursuivre le traitement)
	 * @return un {@link File}
	 * @throws IOException si une erreur est détectée lors de la lecture
	 */
	public static File readFile(JsonReader reader) throws IOException {
		String aux;
		long created = 0;
		String id = null, permalink = null, name = null, title = null, user = null;
		reader.beginObject();
		while (reader.hasNext()) {
			aux = reader.nextName();
			switch (aux) {
				case ID:
					id = reader.nextString();
					break;
				case CREATED:
					created = reader.nextLong();
					break;
				case NAME:
					name = reader.nextString();
					break;
				case PERMALINK:
					permalink = reader.nextString();
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
		return new File(id, created, name, title, user, permalink);
	}
	
	/**
	 * Lecture du Paging dans le JSON
	 *
	 * @param reader le JsonReader précédemment utilisé (pour pouvoir poursuivre le traitement)
	 * @return un {@link Paging}
	 * @throws IOException si une erreur est détectée lors de la lecture
	 */
	public static Paging readPaging(JsonReader reader) throws IOException {
		String name;
		Integer count = null, total = null, page = null, pages = null;
		reader.beginObject();
		while (reader.hasNext()) {
			name = reader.nextName();
			switch (name) {
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
	 * Lecture des membres dans le JSON
	 *
	 * @param reader le JsonReader précédemment utilisé (pour pouvoir poursuivre le traitement)
	 * @return une liste de {@link Member}
	 * @throws IOException si une erreur est détectée lors de la lecture
	 */
	public static List<Member> readMembers(JsonReader reader) throws IOException {
		List<Member> users = new ArrayList<>();
		reader.beginArray();
		while (reader.hasNext())
			users.add(readMember(reader));
		reader.endArray();
		return users;
	}
	
	/**
	 * Lecture du membre dans le JSON
	 *
	 * @param reader le JsonReader précédemment utilisé (pour pouvoir poursuivre le traitement)
	 * @return un {@link Member}
	 * @throws IOException si une erreur est détectée lors de la lecture
	 */
	public static Member readMember(JsonReader reader) throws IOException {
		String aux;
		String id = null, name = null, realName = null;
		reader.beginObject();
		while (reader.hasNext()) {
			aux = reader.nextName();
			switch (aux) {
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
		while (reader.hasNext()) {
			messages.add(readMessage(reader));
		}
		reader.endArray();
		return messages;
	}
	
	/**
	 * Lecture de l'attribut "self" du JSON
	 *
	 * @param reader le JsonReader précédemment utilisé (pour pouvoir poursuivre le traitement)
	 * @return le contenu de l'attribut "self" du JSON
	 * @throws IOException si une erreur est détectée lors de la lecture
	 */
	public static String readSelf(JsonReader reader) throws IOException {
		String name, self = null;
		reader.beginObject();
		while (reader.hasNext()) {
			name = reader.nextName();
			switch (name) {
				case ID:
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
	
	/**
	 * Lecture du message du JSON
	 *
	 * @param reader le JsonReader précédemment utilisé (pour pouvoir poursuivre le traitement)
	 * @return un {@link Message}
	 * @throws IOException si une erreur est détectée lors de la lecture
	 */
	public static Message readMessage(JsonReader reader) throws IOException {
		String name;
		String user = null, channel = null, text = null, subtype = null, timestamp = null;
		List<Reaction> reactions = null;
		reader.beginObject();
		while (reader.hasNext()) {
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
			} catch (IllegalStateException e) {
				e.printStackTrace();
				reader.skipValue();
			}
		}
		reader.endObject();
		return new Message(user, channel, text, subtype, timestamp, reactions);
	}
	
	/**
	 * Lecture des réactions dans le JSON
	 *
	 * @param reader le JsonReader précédemment utilisé (pour pouvoir poursuivre le traitement)
	 * @return une liste de {@link Reaction}
	 * @throws IOException si une erreur est détectée lors de la lecture
	 */
	public static List<Reaction> readReactions(JsonReader reader) throws IOException {
		List<Reaction> reactions = new ArrayList<>();
		reader.beginArray();
		while (reader.hasNext())
			reactions.add(readReaction(reader));
		reader.endArray();
		return reactions;
	}
	
	/**
	 * Lecture d'une réaction dans le JSON
	 *
	 * @param reader le JSONReader précédemment utilisé (pour pouvoir poursuivre le traitement)
	 * @return une {@link Reaction}
	 * @throws IOException si une erreur est détectée lors de la lecture
	 */
	public static Reaction readReaction(JsonReader reader) throws IOException {
		String aux, name = null;
		ArrayList<String> users = null;
		Integer count = null;
		reader.beginObject();
		while (reader.hasNext()) {
			aux = reader.nextName();
			switch (aux) {
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
	
	/**
	 * Lecture des utilisateurs d'une réaction dans le JSON
	 *
	 * @param reader le JSONReader précédemment utilisé (pour pouvoir poursuivre le traitement)
	 * @return une liste d'utilisateurs
	 * @throws IOException si une erreur est détectée lors de la lecture
	 */
	public static ArrayList<String> readReactionUsers(JsonReader reader) throws IOException {
		ArrayList<String> users = new ArrayList<>();
		reader.beginArray();
		while (reader.hasNext())
			users.add(reader.nextString());
		reader.endArray();
		return users;
	}
	
	/**
	 * Conversion du tableau de {@link Attachment} en une chaîne de caractère JSON
	 *
	 * @param attachments un tableau de pièce jointe, {@link Attachment}
	 * @return un chaîne de caractère JSON
	 */
	public static String AttachmentsToJson(Attachment[] attachments) {
		StringBuilder sb = new StringBuilder();
		sb.append("[");
		for (int i = 0; i < attachments.length; i++) {
			sb.append(attachments[i].json());
			if (i != attachments.length - 1)
				sb.append(",");
		}
		sb.append("]");
		return sb.toString();
	}
}
