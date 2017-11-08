package bot;

import api.SlackAPI;
import beans.Reaction;
import beans.events.Message;
import beans.events.ReactionAdded;
import com.google.gson.stream.JsonReader;
import com.sun.istack.internal.Nullable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;

import java.io.IOException;
import java.io.StringReader;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static converter.Converter.readMessageSent;
import static converter.Converter.readReactionAdded;

@WebSocket
public class GuyReactionAnalyser extends Bot {
	private static final Logger Log = LogManager.getLogger(GuyReactionAnalyser.class);

	private static final String MESSAGE = "message";
	private static final String REACTION_ADDED = "reaction_added";
	private static final String TYPE = "type";

	private static final String CMD_HELP = "!help";
	private static final String CMD_STATS = "!stats";
	private static final String CMD_AWAKE = "!awake";

	private static final String BOT_NAME = "Guy";

	//  INFORMATION DE SESSION
	private List<Reaction> reactions;
	private List<String> channels;
	// DATES
	private LocalDateTime startDate;

	public GuyReactionAnalyser(SlackAPI api, String botId) throws Exception {
		super(api, botId, BOT_NAME);
	}

	@Override
	protected void initialisation() throws Exception {
		super.initialisation();
		reactions = new ArrayList<>();
		//  RECHERCHE DES CHANNELS
		channels = new ArrayList<>();
		api.listChannels().forEach(channel -> {
			if (channel.getMembers().contains(id))
				channels.add(channel.getId());
		});
		startDate = LocalDateTime.now();
	}

	public void onConnect(Session session) {
		super.onConnect(session);
		channels.forEach(channel -> {
			try {
				api.meMessage(channel, "s'est réveillé");
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
	}

	@Override
	public void onMessage(String message) {
		Log.info("Event reçu");
		Message M = null;
		ReactionAdded RA = null;

		System.out.println(message);
		JsonReader reader = new JsonReader(new StringReader(message));

		String name;
		try {
			reader.beginObject();
			while (reader.hasNext()) {
				name = reader.nextName();
				switch (name) {
					case TYPE:
						switch (reader.nextString()) {
							case MESSAGE:
								Log.info("Event est : Message");
								M = readMessageSent(reader);
								break;
							case REACTION_ADDED:
								Log.info("Event est : Ajout d'une réaction");
								RA = readReactionAdded(reader);
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

		if (M != null && M.getSubtype() == null && !M.getUser().equals(id)) {
			// Traitement channel publique ou message direct
			switch (getChannelTypeFromMessage(M)) {
				case PUBLIC:
					onPublicMessage(M);
					break;
				case DIRECT_MESSAGE:
					onDirectMessage(M);
					break;
			}
		}

		if (RA != null && isPublicChannel(RA.getChannel())) {
			// TRAITEMENT DE LA REACTION AJOUTEE
			ReactionAdded finalRA = RA;
			Optional<Reaction> reactionFound = reactions.stream().filter(reaction -> reaction.getName().equals(finalRA.getReaction())).findFirst();
			if (reactionFound.isPresent()) {
				reactionFound.get().addUser(RA.getUserId());
				reactionFound.get().incrementCount();
			} else {
				List<String> users = new ArrayList<>();
				users.add(RA.getUserId());
				reactions.add(new Reaction(RA.getReaction(), 1, users));
			}
			Log.info("Réaction traitée et ajoutée");
		}
	}

	@Override
	public void onClose(int statusCode, String reason) {
		Log.info("Fermeture de la connexion : " + statusCode + " - " + reason);

		LocalDateTime endDate = LocalDateTime.now();
		System.out.println(Duration.between(startDate, endDate));
		System.out.println(reactions);

		channels.forEach(channel -> {
			try {
				api.meMessage(channel, "nous a quitté.");
			} catch (Exception e) {
				e.printStackTrace();
			}
		});

		this.session = null;
		this.closeLatch.countDown();
	}

	private void onPublicMessage(Message M) {
		Log.info("Réception d'un message publique");

		if (hasBeenCited(M)) {
			List<String> cmds = getCmd(M.getText());
			if (cmds.isEmpty()) {

			} else if (cmds.contains(CMD_HELP)) {
				sendHelpCmd(M.getChannel());
			} else if (cmds.contains(CMD_STATS)) {
				// Récupération des stats de l'utilisateur
				Map<String, Long> reactionsUser = reactionsUser(M.getUser());
				// Préparation & Envoi du message
				sendReactionsMessage(reactionsUser, M.getChannel(), "<@" + M.getUser() + "> : ");
			} else if (cmds.contains(CMD_AWAKE)) {
				// Ajout d'une réaction pour marquer la présence du bot
				try {
					api.addReaction("thumbsup", M.getChannel(), null, null, M.getTimestamp());
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

	private void onDirectMessage(Message M) {
		// Dans le cas d'un message direct, pas besoin de citer le bot
		// Récupération de l'utilisateur concerné
		Log.info("Réception d'un message direct : \n" + M);

		List<String> cmds = getCmd(M.getText());
		if (cmds.isEmpty()) {

		} else if (cmds.contains(CMD_HELP)) {
			sendHelpCmd(M.getChannel());
		} else if (cmds.contains(CMD_STATS)) {
			// Récupération des stats de l'utilisateur
			Map<String, Long> reactionsUser = reactionsUser(M.getUser());
			// Préparation & Envoi du message
			sendReactionsMessage(reactionsUser, M.getChannel(), null);
		} else if (cmds.contains(CMD_AWAKE)) {
			// Ajout d'une réaction pour marquer la présence du bot
			try {
				api.addReaction("thumbsup", M.getChannel(), null, null, M.getTimestamp());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Récupération des réactions de l'utilisateur passé en paramètre
	 *
	 * @param user : utilisateur pour lequel rechercher ses réactions
	 * @return les réactions, et leurs comptes respectifs, de l'utilisateur passé en paramètre
	 */
	private Map<String, Long> reactionsUser(String user) {
		Map<String, Long> reactionsUser = new HashMap<>();
		reactions.forEach(reaction -> {
			List<String> users = reaction.getUsers();
			if (users.contains(user))
				reactionsUser.put(reaction.getName(), users.stream().filter(u -> u.equals(user)).count());
		});
		// Tri de la map
		return reactionsUser.entrySet().stream()
				.sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
						(oldValue, newValue) -> oldValue, HashMap::new));
	}

	private void sendReactionsMessage(Map<String, Long> reactionsUser, String channel, @Nullable String prefix) {
		StringBuilder sb = new StringBuilder();
		if (prefix != null) sb.append(prefix);
		if (reactionsUser.isEmpty())
			sb.append("Vous n'avez envoyé aucune réaction depuis mon réveil.");
		else {
			sb.append("Vos réactions depuis mon réveil :\\n");
			reactionsUser.forEach((r, l) -> {
				sb.append("\\t:").append(r).append(": : *")
						.append(l).append("*\\n");
			});
		}
		// Envoi du message
		try {
			sendMessage(sb.toString(), channel);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void sendHelpCmd(String channel) {
		Log.info("Envoi des commandes de Guy sur le channel : " + channel);
		String cmds = "Commandes : \\n" +
				"\\t_*!awake*_ : si vous vous demandez si je suis réveillé\\n" +
				"\\t_*!stats*_ : statistiques des réactions\\n" +
				"\\t_*!help*_ : pour apprendre tout ce que je peux vous offrir :heart:";
		// Envoi du message
		try {
			sendMessage(cmds, channel);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Récupération des commandes présentes dans le texte passé en paramètre
	 *
	 * @param text : texte à analyser
	 * @return une liste des commandes présentes dans le texte
	 */
	private List<String> getCmd(String text) {
		List<String> cmds = new ArrayList<>();
		if (text.contains(CMD_STATS)) cmds.add(CMD_STATS);
		if (text.contains(CMD_AWAKE)) cmds.add(CMD_AWAKE);
		if (text.contains(CMD_HELP)) cmds.add(CMD_HELP);
		return cmds;
	}
}
