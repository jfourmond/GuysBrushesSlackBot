package bot;

import beans.File;
import beans.Reaction;
import beans.channels.Channel;
import beans.events.Message;
import beans.events.ReactionAdded;
import com.google.gson.stream.JsonReader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

import static bot.Commands.*;
import static converter.Converter.readMessageSent;
import static converter.Converter.readReactionAdded;
import static java.lang.Thread.sleep;

@WebSocket
public class Guy extends Bot {
	private static final Logger Log = LogManager.getLogger(Guy.class);

	private static final String EMOJIS_FILE = "emojis_list.txt";

	private static final String MESSAGE = "message";
	private static final String REACTION_ADDED = "reaction_added";
	private static final String TYPE = "type";

	private static final String BOT_NAME = "Guy";

	//  INFORMATION DE SESSION
	private List<Reaction> reactions;
	private List<String> channels;
	// DATES
	private LocalDateTime startDate;
	// RANDOM
	private Random rand;

	public Guy() throws Exception {
		super(BOT_NAME);

		rand = new Random(System.currentTimeMillis());
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
		// TODO Décommenter en déploiement publique
//		channels.forEach(channel -> {
//			try {
//				api.meMessage(channel, "s'est réveillé");
//			} catch (Exception e) {
//				e.printStackTrace();
//			}
//		});
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
			try {
				switch (getChannelTypeFromMessage(M)) {
					case PUBLIC:
						onPublicMessage(M);
						break;
					case DIRECT_MESSAGE:
						onDirectMessage(M);
						break;
				}
			} catch (Exception E) {
				E.printStackTrace();
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

	private void onPublicMessage(Message M) throws Exception {
		Log.info("Réception d'un message publique");

		if (hasBeenCited(M)) {
			messageTreatment(M);
		}
	}

	private void onDirectMessage(Message M) throws Exception {
		// Dans le cas d'un message direct, pas besoin de citer le bot
		// Récupération de l'utilisateur concerné
		Log.info("Réception d'un message direct : \n" + M);
		messageTreatment(M);
	}

	/**
	 * Récupération de la première commande à exécuter dans le texte passé en paramètre
	 *
	 * @param text texte à analyser
	 * @return la commande à exécuter
	 */
	private String getCmd(String text) {
		String command = null;
		int index = Integer.MAX_VALUE;
		for (String cmd : cmds()) {
			int ind = text.indexOf(cmd);
			if (ind != -1 && ind < index) {
				command = cmd;
				index = ind;
			}
		}
		Log.info("Commande détectée : " + command);
		return command;
	}

	/**
	 * Récupération des arguments utilisés avec la commande dans le texte passé en paramètre
	 *
	 * @param cmd  commande utilisé
	 * @param text texte à analyser
	 * @return les arguments à utiliser avec la commande
	 */
	private String getCmdArg(String cmd, String text) {
		// TODO
		return null;
	}

	private void messageTreatment(Message M) throws Exception {
		String cmd = getCmd(M.getText());
		if (cmd == null) {
			// Interaction avec l'utilisateur lorsqu'il n'exécute pas de commande
			try {
				Path path = Paths.get(getClass().getClassLoader().getResource(EMOJIS_FILE).toURI());
				long lineCount = Files.lines(path).count();
				int lineN = rand.nextInt((int) lineCount);
				api.addReaction(Files.readAllLines(path).get(lineN), M.getChannel(), null, null, M.getTimestamp());
			} catch (Exception E) {
				E.printStackTrace();
			}
		} else {
			String prefix = null;
			if (isPublicChannel(M.getChannel()))
				prefix = "<@" + M.getUser() + "> : ";
			switch (cmd) {
				case CMD_AWAKE:
					// COMMANDE : !awake
					sendMessage(":wave:", M.getChannel());
					break;
				case CMD_FILES:
					// COMMANDE : !files
					List<File> files = api.listAllFiles(null, null, M.getUser());
					sendFilesInfo(files, M.getChannel(), prefix);
					break;
				case CMD_HELP:
					// COMMANDE : !help
					sendHelpCmd(M.getChannel());
					break;
				case CMD_PLOP:
					// COMMANDE : !plop
					sendMessage("plop", M.getChannel());
					break;
				case CMD_REMAINING:
					// COMMANDE : !remaining
					// Envoie le temps restant du bot
					Duration d = Duration.between(startDate, LocalDateTime.now());
					long remaining = duration - d.toMinutes();
					sendMessage("Il me reste " + remaining + " minutes...", M.getChannel());
					break;
				case CMD_STATS:
					// COMMANDE : !stats
					// Récupération des stats de l'utilisateur
					Map<String, Long> reactionsUser = reactionsUser(M.getUser());
					// Préparation & Envoi du message
					sendReactionsMessage(reactionsUser, M.getChannel(), prefix);
					break;
				case CMD_TOP_3:
					// COMMANDE : !top3
					sendTop3(M.getChannel());
					break;
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

	/**
	 * Réponse à la commande !help
	 * Envoie les commandes de Guy sur le channel passé en paramètre
	 *
	 * @param channel identifiant du channel
	 */
	private void sendHelpCmd(String channel) {
		Log.info("Envoi des commandes de Guy sur le channel : " + channel);
		List<String> cmds = new ArrayList<>();
		cmds.add("Commandes : ");
		Commands.cmdsMap.forEach((nom, desc) ->
				cmds.add("\\t*" + nom + "* _" + desc + "_")
		);
		// Envoi du message
		try {
			sendMessage(cmds, channel);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Réponse à la commande !stats
	 * <p>
	 * Envoie les statistiques de l'utilisateur en terme d'utilisation d'emoji
	 *
	 * @param reactionsUser Liste d'association < emoji, nb d'utilisations > de l'utilisateur concerné
	 * @param channel       identifiant du channel sur lequel envoyer le message
	 * @param prefix        préfixe à utiliser dans le message (communément l'identifiant de l'utilisateur concerné dans les channels publiques)
	 */
	private void sendReactionsMessage(Map<String, Long> reactionsUser, String channel, @Nullable String prefix) {
		StringBuilder sb = new StringBuilder();
		if (prefix != null) sb.append(prefix);
		if (reactionsUser.isEmpty())
			sb.append("Vous n'avez envoyé aucune réaction depuis mon réveil.");
		else {
			sb.append("Vos réactions depuis mon réveil :\\n");
			reactionsUser.forEach((r, l) -> sb.append("\\t:").append(r).append(": : *")
					.append(l).append("*\\n"));
		}
		// Envoi du message
		try {
			sendMessage(sb.toString(), channel);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Réponse à la commande !files
	 *
	 * @param files   Liste de fichiers de l'utilisateur concerné
	 * @param channel identifiant du channel sur lequel envoyer le message
	 * @param prefix  préfixe à utiliser dans le message (communément l'identifiant de l'utilisateur concerné dans les channels publiques)
	 */
	private void sendFilesInfo(List<File> files, String channel, @Nullable String prefix) {
		StringBuilder sb = new StringBuilder();
		if (prefix != null) sb.append(prefix);
		if (files.isEmpty())
			sb.append("Vous n'avez aucun fichier sur le Slack.");
		else
			sb.append("Vos possédez ").append(files.size()).append(" fichiers sur le Slack.");
		// Envoi du message
		try {
			sendMessage(sb.toString(), channel);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Réponse à la commande !top3
	 * <p>
	 * Récupération des 3 meilleurs messages des 7 derniers jours du channel passé en paramètre
	 *
	 * @param channelId identifiant du channel
	 * @throws Exception si une erreur est levée dans le récupération des fichiers ou dans l'envoi du message
	 */
	private void sendTop3(String channelId) throws Exception {
		Instant sevenDaysAgo = Instant.now().minus(7, ChronoUnit.DAYS);
		Long timestamp7DaysAgo = sevenDaysAgo.getEpochSecond();

		Channel channel = api.getChannelById(channelId);
		List<Message> messages = api.fetchAllMessages(channel.getId(), null, timestamp7DaysAgo);
		System.out.println(messages.size() + " MESSAGES");
		// FILTRE
		messages = messages.stream().filter(message -> message.getReactions() != null).collect(Collectors.toList());
		messages.sort((o1, o2) -> o2.countReactions() - o1.countReactions());

		System.out.println(messages.size() + " MESSAGES AVEC REACTIONS");
		List<Message> bestMessages = messages.subList(0, 3);

		sendMessage("Voici le top 3 des messages du channel _*" + channel.getName() + "*_ de ces 7 derniers jours :fireworks: ! :", channel.getId());

		sleep(5000);

		for (int number = 2; number >= 0; number--) {
			Message message = bestMessages.get(number);
			String timestampStr = message.getTimestamp();

			String urlTimestamp = timestampStr.replace(".", "");

			StringBuilder sb = new StringBuilder();
			sb.append("<https://guysbrushes.slack.com/archives/").append(channel.getId()).append("/p").append(urlTimestamp)
					.append("|N°").append((number + 1)).append("> avec ").append(message.countReactions()).append(" réactions ");
			for (Reaction reaction : message.getReactions())
				sb.append(":").append(reaction.getName()).append(": ");
			sb.append(": ");


			api.postMessage(channel.getId(), sb.toString(), true,
					null, null, null, null, null,
					true, null, null);

			sleep(15000);
		}
	}
}