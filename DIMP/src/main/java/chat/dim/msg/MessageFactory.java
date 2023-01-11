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

    //
    //  InstantMessage.Factory
    //
    @Override
    public long generateSerialNumber(int msgType, Date time) {
        // because we must make sure all messages in a same chat box won't have
        // same serial numbers, so we can't use time-related numbers, therefore
        // the best choice is a totally random number, maybe.
        Random random = new Random();
        int sn = random.nextInt();
        if (sn > 0) {
            return sn;
        } else if (sn < 0) {
            return -sn;
        }
        // ZERO? do it again!
        return 9527 + 9394; // generateSerialNumber(msgType, time);
    }

    @Override
    public InstantMessage createInstantMessage(Envelope head, Content body) {
        return new PlainMessage(head, body);
    }

    @Override
    public InstantMessage parseInstantMessage(Map<String, Object> msg) {
        // check 'sender', 'content'
        Object sender = msg.get("sender");
        Object content = msg.get("content");
        if (sender == null || content == null) {
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
        Object sender = msg.get("sender");
        Object data = msg.get("data");
        if (sender == null || data == null) {
            // msg.sender should not be empty
            // msg.data should not be empty
            return null;
        }
        // check 'signature'
        Object signature = msg.get("signature");
        if (signature != null) {
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
        Object sender = msg.get("sender");
        Object data = msg.get("data");
        Object signature = msg.get("signature");
        if (sender == null || data == null || signature == null) {
            // msg.sender should not be empty
            // msg.data should not be empty
            // msg.signature should not be empty
            return null;
        }
        return new NetworkMessage(msg);
    }
}
