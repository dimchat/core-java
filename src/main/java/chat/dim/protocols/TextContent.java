package chat.dim.protocols;

import chat.dim.dkd.Content;

import java.util.Map;

/**
 *  Text message: {
 *      type : 0x01,
 *      sn   : 123,
 *
 *      text : "..."
 *  }
 */
public class TextContent extends Content {

    private String text;

    public TextContent(Map<String, Object> dictionary) {
        super(dictionary);
    }

    public TextContent(String message) {
        super(ContentType.TEXT.value);
        setText(message);
    }

    //-------- setter/getter --------

    public void setText(String message) {
        text = message;
        dictionary.put("text", message);
    }

    public String getText() {
        return text;
    }
}
