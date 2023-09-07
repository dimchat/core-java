/* license: https://mit-license.org
 *
 *  Dao-Ke-Dao: Universal Message Module
 *
 *                                Written in 2022 by Moky <albert.moky@gmail.com>
 *
 * ==============================================================================
 * The MIT License (MIT)
 *
 * Copyright (c) 2022 Albert Moky
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
package chat.dim.msg;

import java.util.Date;
import java.util.Map;
import java.util.Random;

import chat.dim.protocol.Content;
import chat.dim.protocol.Envelope;
import chat.dim.protocol.InstantMessage;
import chat.dim.protocol.ReliableMessage;
import chat.dim.protocol.SecureMessage;

public class MessageFactory implements InstantMessage.Factory, SecureMessage.Factory, ReliableMessage.Factory {

    private int sn;

    public MessageFactory() {
        super();
        Random random = new Random();
        sn = random.nextInt();
        if (sn < 0) {
            sn = -sn;
        } else if (sn == 0) {
            // ZERO? do it again!
            sn = 9527 + 9394;
        }
    }

    /**
     *  next sn
     *
     * @return 1 ~ 2^31-1
     */
    private synchronized int next() {
        if (sn < 0x7fffffff) {
            sn += 1;
        } else {
            sn = 1;
        }
        return sn;
    }

    //
    //  InstantMessage.Factory
    //
    @Override
    public long generateSerialNumber(int msgType, Date time) {
        // because we must make sure all messages in a same chat box won't have
        // same serial numbers, so we can't use time-related numbers, therefore
        // the best choice is a totally random number, maybe.
        return next();
    }

    @Override
    public InstantMessage createInstantMessage(Envelope head, Content body) {
        return new PlainMessage(head, body);
    }

    @Override
    public InstantMessage parseInstantMessage(Map<String, Object> msg) {
        // check 'sender', 'content'
        if (msg.get("sender") == null || msg.get("content") == null) {
            // msg.sender should not be empty
            // msg.content should not be empty
            return null;
        }
        return new PlainMessage(msg);
    }

    //
    //  SecureMessage.Factory
    //
    @Override
    public SecureMessage parseSecureMessage(Map<String, Object> msg) {
        // check 'sender', 'data'
        if (msg.get("sender") == null || msg.get("data") == null) {
            // msg.sender should not be empty
            // msg.data should not be empty
            return null;
        }
        // check 'signature'
        if (msg.get("signature") != null) {
            return new NetworkMessage(msg);
        }
        return new EncryptedMessage(msg);
    }

    //
    //  ReliableMessage.Factory
    //
    @Override
    public ReliableMessage parseReliableMessage(Map<String, Object> msg) {
        // check 'sender', 'data', 'signature'
        if (msg.get("sender") == null || msg.get("data") == null || msg.get("signature") == null) {
            // msg.sender should not be empty
            // msg.data should not be empty
            // msg.signature should not be empty
            return null;
        }
        return new NetworkMessage(msg);
    }
}
