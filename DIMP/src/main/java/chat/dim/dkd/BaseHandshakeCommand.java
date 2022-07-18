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
package chat.dim.dkd;

import java.util.Map;

import chat.dim.protocol.HandshakeCommand;
import chat.dim.protocol.HandshakeState;

/**
 *  Command message: {
 *      type : 0x88,
 *      sn   : 123,
 *
 *      cmd     : "handshake",    // command name
 *      message : "Hello world!",
 *      session : "{SESSION_KEY}" // session key
 *  }
 */
public class BaseHandshakeCommand extends BaseCommand implements HandshakeCommand {

    private final String message;
    private final String sessionKey;
    private final HandshakeState state;

    public BaseHandshakeCommand(Map<String, Object> command) {
        super(command);
        message    = (String) command.get("message");
        sessionKey = (String) command.get("session");
        state      = getState(message, sessionKey);
    }

    public BaseHandshakeCommand(String text, String session) {
        super(HANDSHAKE);
        // message
        message = text;
        put("message", text);
        // session key
        sessionKey = session;
        if (session != null) {
            put("session", session);
        }
        // state
        state = getState(text, session);
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public String getSessionKey() {
        return sessionKey;
    }

    @Override
    public HandshakeState getState() {
        return state;
    }

    private static HandshakeState getState(String text, String session) {
        assert text != null : "handshake message should not be empty";
        if (text.equals("DIM!") || text.equals("OK!")) {
            return HandshakeState.SUCCESS;
        } else if (text.equals("DIM?")) {
            return HandshakeState.AGAIN;
        } else if (session == null) {
            return HandshakeState.START;
        } else {
            return HandshakeState.RESTART;
        }
    }
}
