package chat.dim.protocols;

import chat.dim.dkd.Content;
import chat.dim.dkd.ReliableMessage;

import java.util.Map;

/**
 *  Top-Secret message: {
 *      type : 0xFF,
 *      sn   : 456,
 *
 *      forward : {...}  // reliable (secure + certified) message
 *  }
 */
public class ForwardContent extends Content {

    public final ReliableMessage forwardMessage;

    public ForwardContent(Map<String, Object> dictionary) throws NoSuchFieldException {
        super(dictionary);
        forwardMessage = ReliableMessage.getInstance(dictionary.get("forward"));
    }

    public ForwardContent(ReliableMessage message) {
        super(ContentType.FORWARD.value);
        forwardMessage = message;
        dictionary.put("forward", message);
    }
}
