package chat.dim.protocols;

import chat.dim.dkd.Content;
import chat.dim.protocols.ContentType;

import java.util.Map;

/**
 *  Command message: {
 *      type : 0x88,
 *      sn   : 123,
 *
 *      command : "...", // command name
 *      extra   : info   // command parameters
 *  }
 */
public class CommandContent extends Content {

    //-------- command names begin --------
    public static final String HANDSHAKE = "handshake";
    public static final String BROADCAST = "broadcast";
    public static final String RECEIPT   = "receipt";
    public static final String META      = "meta";
    public static final String PROFILE   = "profile";
    //-------- command names end --------

    public final String command;

    public CommandContent(Map<String, Object> dictionary) {
        super(dictionary);
        command = (String) dictionary.get("command");
    }

    protected CommandContent(int type, String cmd) {
        super(type);
        command = cmd;
        dictionary.put("command", cmd);
    }

    public CommandContent(String command) {
        this(ContentType.COMMAND.value, command);
    }
}
