/* license: https://mit-license.org
 * ==============================================================================
 * The MIT License (MIT)
 *
 * Copyright (c) 2019 Albert Moky
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 * ==============================================================================
 */
package chat.dim.protocol.command;

import chat.dim.protocol.CommandContent;

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

    public HandshakeCommand(Map<String, Object> dictionary) {
        super(dictionary);
        message    = (String) dictionary.get("message");
        sessionKey = (String) dictionary.get("session");
        state      = getState(message, sessionKey);
    }

    public HandshakeCommand(String text, String session) {
        super(HANDSHAKE);
        // message
        message = text;
        dictionary.put("message", text);
        // session key
        sessionKey = session;
        dictionary.put("session", sessionKey);
        // state
        state = getState(message, sessionKey);
    }

    public HandshakeCommand(String session) {
        this("Hello world!", session);
    }

    public static int getState(String text, String session) {
        // check message
        if (text == null) {
            return INIT;
        } else if (text.equals("DIM!") || text.equals("OK!")) {
            return SUCCESS;
        } else if (text.equals("DIM?")) {
            return AGAIN;
        }
        // check session key
        if (session == null) {
            return START;
        } else {
            return RESTART;
        }
    }
}
