package runnable;

import api.SlackAPI;
import beans.File;

import java.util.List;

public class ListFiles {
    public static void main(String args[]) throws Exception {
        SlackAPI api = new SlackAPI();

        List<File> files = api.listAllFiles(null, null, null);

        System.out.println(files.size() + " FICHIERS");
    }
}
