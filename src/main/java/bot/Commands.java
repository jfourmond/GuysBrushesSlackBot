package bot;

import java.util.Arrays;
import java.util.List;

class Commands {
	static final String CMD_AWAKE = "!awake";
	static final String CMD_FILES = "!files";
	static final String CMD_HELP = "!help";
	static final String CMD_PLOP = "!plop";
	static final String CMD_REMAINING = "!remaining";
	static final String CMD_STATS = "!stats";
	static final String CMD_TOP_3 = "!top3";

	static List<String> cmds() {
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
