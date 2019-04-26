package chat.dim.protocols;

import chat.dim.dkd.content.CommandContent;

import java.util.Map;

/**
 *  Command message: {
 *      type : 0x88,
 *      sn   : 123,
 *
 *      command : "handshake",    // command name
 *      message : "Hello world!",
 *      session : "{SESSION_KEY}" // session key
 *  }
 */
public class HandshakeCommand extends CommandContent {

    //-------- states begin --------
    public static final int INIT    = 0;
    public static final int START   = 1;  // C -> S, without session key(or session expired)
    public static final int AGAIN   = 2;  // S -> C, with new session key
    public static final int RESTART = 3;  // C -> S, with new session key
    public static final int SUCCESS = 4;  // S -> C, handshake accepted
    //-------- states end --------

    public final String message;
    public final String sessionKey;
    public final int state;

    public HandshakeCommand(HandshakeCommand content) {
        super(content);
        this.message    = content.message;
        this.sessionKey = content.sessionKey;
        this.state      = content.state;
    }

    public HandshakeCommand(Map<String, Object> dictionary) {
        super(dictionary);
        this.message    = (String) dictionary.get("message");
        this.sessionKey = (String) dictionary.get("session");
        this.state      = getState(this.message, this.sessionKey);
    }

    public HandshakeCommand(String message, String sessionKey) {
        super(HANDSHAKE);
        // message
        this.message = message;
        this.dictionary.put("message", message);
        // session key
        this.sessionKey = sessionKey;
        this.dictionary.put("session", sessionKey);
        // state
        this.state = getState(message, sessionKey);
    }

    public HandshakeCommand(String sessionKey) {
        this("Hello world!", sessionKey);
    }

    public static int getState(String message, String sessionKey) {
        // check message
        if (message == null) {
            return INIT;
        } else if (message.equals("DIM!") || message.equals("OK!")) {
            return SUCCESS;
        } else if (message.equals("DIM?")) {
            return AGAIN;
        }
        // check session key
        if (sessionKey == null) {
            return START;
        } else {
            return RESTART;
        }
    }
}
