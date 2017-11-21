package bot;

import java.util.*;

public class Commands {
	public static final String CMD_AWAKE = "!awake";
	public static final String CMD_FILES = "!files";
	public static final String CMD_HELP = "!help";
	public static final String CMD_PLOP = "!plop";
		public static final int CMD_PLOP_ARGUMENT = 1;
	public static final String CMD_REMAINING = "!remaining";
	public static final String CMD_SAY = "!say";
		public static final int CMD_SAY_ARGUMENT = -1;
	public static final String CMD_STATS = "!stats";
	public static final String CMD_TOP_3 = "!top3";
	
	public static final Map<String, String> cmdsMap;
	static {
		Map<String, String> aMap = new LinkedHashMap<>();
		aMap.put(CMD_AWAKE, "si vous vous demandez si je suis réveillé");
		aMap.put(CMD_FILES, "pour savoir le nombre de fichiers que vous avez posté");
		aMap.put(CMD_HELP, "pour apprendre tout ce que j'ai à vous offrir :heart:");
		aMap.put(CMD_PLOP, "plop");
		aMap.put(CMD_REMAINING, "le temps qu'il me reste...");
		aMap.put(CMD_SAY, "un message anonyme a passé ? :wink:");
		aMap.put(CMD_STATS, " statistiques des réactions");
		aMap.put(CMD_TOP_3, "pour avoir le top 3 des messages les plus populaires");
		cmdsMap = Collections.unmodifiableMap(aMap);
	}
	
	public static final List<String> cmds;
	static {
		cmds = Arrays.asList(
				CMD_AWAKE,
				CMD_FILES,
				CMD_HELP,
				CMD_PLOP,
				CMD_REMAINING,
				CMD_SAY,
				CMD_STATS,
				CMD_TOP_3);
	}
	
	public static int getCmdArgumentNb(String cmd) {
		switch(cmd) {
			case CMD_PLOP:
				return CMD_PLOP_ARGUMENT;
			case CMD_SAY:
				return CMD_SAY_ARGUMENT;
			default:
				return 0;
		}
	}
}
