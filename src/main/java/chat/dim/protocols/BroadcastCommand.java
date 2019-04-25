package chat.dim.protocols;

import chat.dim.dkd.content.CommandContent;

import java.util.Map;

public class BroadcastCommand extends CommandContent {

    public static String REPORT = "report";

    public final String title;

    public BroadcastCommand(BroadcastCommand content) {
        super(content);
        this.title = content.title;
    }

    public BroadcastCommand(Map<String, Object> dictionary) {
        super(dictionary);
        this.title = (String) dictionary.get("title");
    }

    public BroadcastCommand(String title) {
        super(BROADCAST);
        this.title = title;
        this.dictionary.put("title", title);
    }
}
