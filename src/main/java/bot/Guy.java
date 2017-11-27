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
	private Map<String, LocalDateTime> anonymousMessage;
	
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
		anonymousMessage = new HashMap<>();
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
				try {
					api.postEphemeral(M.getChannel(), "Votre demande n'a pas pu être traitée, veuillez essayer ultérieurement",
							M.getUser(), true, null, null, null);
				} catch (Exception EE) {
					EE.printStackTrace();
				}
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
		for (String cmd : cmds) {
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
	private List<String> getCmdArg(String cmd, String text) {
		// Récupération du texte après l'argument (s'il y en a)
		int ind = text.indexOf(cmd);
		if (ind != -1)
			text = text.substring(ind + cmd.length()).trim();
		// Récupération des arguments
		int argNb = getCmdArgumentNb(cmd);
		if (argNb == -1)
			return Collections.singletonList(text);
		if (argNb == 1)
			return Collections.singletonList(text.split(" ")[0]);
		if (argNb > 1) {
			List<String> list = new ArrayList<>();
			for (String txt : text.split(" "))
				if (!txt.isEmpty())
					list.add(txt);
			return list;
		}
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
			List<String> args;
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
					args = getCmdArg(CMD_FILES, M.getText());
					sendFilesInfo(files, M.getChannel(), prefix, args);
					break;
				case CMD_HELP:
					// COMMANDE : !help
					sendHelpCmd(M.getChannel());
					break;
				case CMD_PLOP:
					// COMMANDE : !plop [Number]
					args = getCmdArg(CMD_PLOP, M.getText());
					sendPlop(args, M.getChannel());
					break;
				case CMD_REMAINING:
					// COMMANDE : !remaining
					// Envoie le temps restant du bot
					Duration d = Duration.between(startDate, LocalDateTime.now());
					long remaining = duration - d.toMinutes();
					sendMessage("Il me reste " + remaining + " minutes...", M.getChannel());
					break;
				case CMD_SAY:
					// COMMANDE : !say
					String message = getCmdArg(CMD_SAY, M.getText()).get(0);
					say(message, M.getChannel(), M.getUser(), M.getTimestamp());
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
	 * Réponse à la commande !plop [Number]
	 *
	 * @param args      argument(s) passé(s) à la commande
	 * @param channelId identifiant du channel sur lequel envoyé le message
	 */
	private void sendPlop(@Nullable List<String> args, String channelId) {
		try {
			if (args == null)
				sendMessage("plop", channelId);
			else {
				Integer nbPlop = Integer.parseInt(args.get(0));
				StringBuilder sb = new StringBuilder();
				for (int i = 0; i < nbPlop; i++)
					sb.append("plop ");
				sendMessage(sb.toString(), channelId);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Réponse à la commande !say [message]
	 *
	 * @param message          message à envoyer
	 * @param currentChannelId le channel de la commande
	 * @param userId           l'utilisateur faisant appel à la commande
	 * @param ts               timestamp du message faisant appel à la commande
	 */
	private void say(String message, String currentChannelId, String userId, String ts) {
		boolean ok = true;
		LocalDateTime now = LocalDateTime.now();
		if (anonymousMessage.containsKey(userId)) {
			LocalDateTime d = anonymousMessage.get(userId);
			Duration between = Duration.between(now, d);
			if (between.toHours() < 24)
				ok = false;
		}
		
		try {
			if (ok) {
				sendMessage(message, currentChannelId);
				anonymousMessage.put(userId, now);
			} else
				api.postEphemeral(currentChannelId, "Veuillez attendre 24h pour envoyer un message anonyme", userId,
						true, null, null, null);
			// Suppression du message
			api.deleteMessage(currentChannelId, ts, true);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Réponse à la commande !stats
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
	 * @param args    arguments optionnels à la commande
	 */
	private void sendFilesInfo(List<File> files, String channel, @Nullable String prefix, List<String> args) {
		StringBuilder sb = new StringBuilder();
		if (prefix != null) sb.append(prefix);
		if (args.isEmpty()) {
			if (files.isEmpty())
				sb.append("Vous n'avez aucun fichier sur le Slack.");
			else
				sb.append("Vous possédez ").append(files.size()).append(" fichiers sur le Slack.");
		} else {
			// VIDE : DONNER LE COMPTE
			// LIST : LISTER LES FICHIERS
			// DELETE : SUPPRIMER LES FICHIERS
			// 	avec arguments : - ALL : supprimer tout
			//					 - OLDER [N] les N plus vieux
			//					 - YOUNGER [N] les N plus récents
			String arg = args.get(0).toLowerCase();
			String y = null;
			int n = 0;
			if (args.size() >= 2)
				y = args.get(1).toLowerCase();
			if (args.size() >= 3)
				n = Integer.parseInt(args.get(2));
			
			System.out.println(arg);
			System.out.println(y);
			System.out.println(n);
			
			if (y != null) {
				switch (y.toLowerCase()) {
					case "all":
						if (arg.equals("list"))
							for (File file : files)
								sb.append("<").append(file.getPermalink()).append("|").append(file.getTitle()).append(">").append("\n");
						if (arg.equals("delete")) {
							for (File file : files)
								try {
									api.deleteFile(file.getId());
								} catch (Exception e) {
									e.printStackTrace();
								}
							sb.append("Tous vos fichiers ont été supprimés");
						}
						break;
					case "oldest":
						files.sort(Comparator.comparingLong(File::getCreated));
						
						if (arg.equals("list") && n != 0)
							for (int i = 0; i < n; i++)
								sb.append("<").append(files.get(i).getPermalink()).append("|").append(files.get(i).getTitle()).append(">").append("\n");
						if (arg.equals("delete") && n != 0) {
							for (int i = 0; i < n; i++)
								try {
									api.deleteFile(files.get(i).getId());
								} catch (Exception e) {
									e.printStackTrace();
								}
							sb.append("Vos ").append(n).append(" plus vieux fichiers ont été supprimés");
						}
						break;
					case "latest":
						files.sort((File f1, File f2) -> Long.compare(f2.getCreated(), f1.getCreated()));
						if (arg.equals("list") && n != 0)
							for (int i = 0; i < n; i++)
								sb.append("<").append(files.get(i).getPermalink()).append("|").append(files.get(i).getTitle()).append(">").append("\n");
						if (arg.equals("delete") && n != 0) {
							for (int i = 0; i < n; i++)
								try {
									api.deleteFile(files.get(i).getId());
								} catch (Exception e) {
									e.printStackTrace();
								}
							sb.append("Vos ").append(n).append(" plus récents fichiers ont été supprimés");
						}
						break;
				}
			}
		}
		
		// Envoi du message
		try {
			if (!sb.toString().isEmpty())
				api.postMessage(channel, sb.toString(), true, null, null, null, null, null, null, false, null);
			// sendMessage(sb.toString(), channel);
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
		List<Message> messages = api.fetchAllMessages(channel.getId(), 1000, timestamp7DaysAgo);
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
			
			sleep(5000);
		}
	}
}