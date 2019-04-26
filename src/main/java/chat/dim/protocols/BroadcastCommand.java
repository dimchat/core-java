package chat.dim.protocols;

import chat.dim.dkd.content.CommandContent;

import java.util.Map;

/**
 *  Command message: {
 *      type : 0x88,
 *      sn   : 123,
 *
 *      command : "broadcast",
 *      title   : "...", // broadcast title
 *      extra   : info   // broadcast info
 *  }
 */
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
