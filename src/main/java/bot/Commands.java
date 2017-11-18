package bot;

import java.util.*;

public class Commands {
    public static final Map<String, String> cmdsMap;

    static {
        Map<String, String> aMap = new HashMap();
        aMap.put("!awake", "si vous vous demandez si je suis réveillé");
        aMap.put("!files", "pour savoir le nombre de fichiers que vous avez posté");
        aMap.put("!help", "pour apprendre tout ce que j'ai à vous offrir :heart:");
        aMap.put("!plop", "plop");
        aMap.put("!remaining", "le temps qu'il me reste...");
        aMap.put("!stats", " statistiques des réactions");
        aMap.put("!top3", "pour avoir le top 3 des messages les plus populaires");
        cmdsMap = Collections.unmodifiableMap(aMap);
    }

    public static final String CMD_AWAKE = "!awake";
    public static final String CMD_FILES = "!files";
    public static final String CMD_HELP = "!help";
    public static final String CMD_PLOP = "!plop";
    public static final String CMD_REMAINING = "!remaining";
    public static final String CMD_STATS = "!stats";
    public static final String CMD_TOP_3 = "!top3";

    public static List<String> cmds() {
        return Arrays.asList(
                CMD_AWAKE,
                CMD_FILES,
                CMD_HELP,
                CMD_PLOP,
                CMD_REMAINING,
                CMD_STATS,
                CMD_TOP_3);
    }
}
