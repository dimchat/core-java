/* license: https://mit-license.org
 *
 *  DIMP : Decentralized Instant Messaging Protocol
 *
 *                                Written in 2019 by Moky <albert.moky@gmail.com>
 *
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
package chat.dim.protocol;

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
public class HandshakeCommand extends Command {

    public final String message;
    public final String sessionKey;
    public final HandshakeState state;

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

    private static HandshakeState getState(String text, String session) {
        // check message
        if (text == null) {
            return HandshakeState.INIT;
        }
        if (text.equals("DIM!") || text.equals("OK!")) {
            return HandshakeState.SUCCESS;
        }
        if (text.equals("DIM?")) {
            return HandshakeState.AGAIN;
        }
        // check session key
        if (session == null) {
            return HandshakeState.START;
        } else {
            return HandshakeState.RESTART;
        }
    }

    /**
     *  Handshake state
     */
    public enum HandshakeState {
        INIT,
        START,    // C -> S, without session key(or session expired)
        AGAIN,    // S -> C, with new session key
        RESTART,  // C -> S, with new session key
        SUCCESS,  // S -> C, handshake accepted
    }

    //
    //  Factories
    //

    public static HandshakeCommand start() {
        return restart(null);
    }

    public static HandshakeCommand restart(String sessionKey) {
        return new HandshakeCommand(sessionKey);
    }

    public static HandshakeCommand again(String sessionKey) {
        return new HandshakeCommand("DIM?", sessionKey);
    }

    public static HandshakeCommand success(String sessionKey) {
        return new HandshakeCommand("DIM!", sessionKey);
    }
    public static HandshakeCommand success() {
        return success(null);
    }
}
