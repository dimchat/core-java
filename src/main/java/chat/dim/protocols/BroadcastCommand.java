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

    public BroadcastCommand(Map<String, Object> dictionary) {
        super(dictionary);
        title = (String) dictionary.get("title");
    }

    public BroadcastCommand(String message) {
        super(BROADCAST);
        title = message;
        dictionary.put("title", message);
    }
}
