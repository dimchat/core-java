/* license: https://mit-license.org
 *
 *  DIMP : Decentralized Instant Messaging Protocol
 *
 *                                Written in 2020 by Moky <albert.moky@gmail.com>
 *
 * ==============================================================================
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 Albert Moky
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
package chat.dim.core;

import java.lang.ref.WeakReference;

import chat.dim.User;
import chat.dim.protocol.Content;
import chat.dim.protocol.Envelope;
import chat.dim.protocol.ID;
import chat.dim.protocol.InstantMessage;
import chat.dim.protocol.ReliableMessage;
import chat.dim.protocol.SecureMessage;

/**
 *  Message Processor
 *  ~~~~~~~~~~~~~~~~~
 */
public abstract class Processor implements Transceiver.Processor {

    private final WeakReference<Transceiver> transceiverRef;

    protected Processor(Transceiver transceiver) {
        super();
        transceiverRef = new WeakReference<>(transceiver);
    }

    protected Transceiver getTransceiver() {
        return transceiverRef.get();
    }

    @Override
    public byte[] process(byte[] data) {
        // 1. deserialize message
        ReliableMessage rMsg = getTransceiver().deserializeMessage(data);
        if (rMsg == null) {
            // no valid message received
            return null;
        }
        // 2. process message
        rMsg = getTransceiver().process(rMsg);
        if (rMsg == null) {
            // nothing to respond
            return null;
        }
        // 3. serialize message
        return getTransceiver().serializeMessage(rMsg);
    }

    @Override
    public ReliableMessage process(ReliableMessage rMsg) {
        // TODO: override to check broadcast message before calling it
        // 1. verify message
        SecureMessage sMsg = getTransceiver().verifyMessage(rMsg);
        if (sMsg == null) {
            // waiting for sender's meta if not exists
            return null;
        }
        // 2. process message
        sMsg = getTransceiver().process(sMsg, rMsg);
        if (sMsg == null) {
            // nothing to respond
            return null;
        }
        // 3. sign message
        return getTransceiver().signMessage(sMsg);
        // TODO: override to deliver to the receiver when catch exception "receiver error ..."
    }

    @Override
    public SecureMessage process(SecureMessage sMsg, ReliableMessage rMsg) {
        // 1. decrypt message
        InstantMessage iMsg = getTransceiver().decryptMessage(sMsg);
        if (iMsg == null) {
            // cannot decrypt this message, not for you?
            // delivering message to other receiver?
            return null;
        }
        // 2. process message
        iMsg = getTransceiver().process(iMsg, rMsg);
        if (iMsg == null) {
            // nothing to respond
            return null;
        }
        // 3. encrypt message
        return getTransceiver().encryptMessage(iMsg);
    }

    @Override
    public InstantMessage process(InstantMessage iMsg, ReliableMessage rMsg) {
        // 1. process content
        Content response = getTransceiver().process(iMsg.getContent(), rMsg);
        if (response == null) {
            // nothing to respond
            return null;
        }

        // 2. select a local user to build message
        ID sender = iMsg.getSender();
        ID receiver = iMsg.getReceiver();
        User user = getTransceiver().selectLocalUser(receiver);
        assert user != null : "receiver error: " + receiver;

        // 3. pack message
        Envelope env = Envelope.create(user.identifier, sender, null);
        return InstantMessage.create(env, response);
    }
}
