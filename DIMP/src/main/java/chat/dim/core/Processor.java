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
import java.util.ArrayList;
import java.util.List;

import chat.dim.Transceiver;
import chat.dim.User;
import chat.dim.protocol.Content;
import chat.dim.protocol.Envelope;
import chat.dim.protocol.ID;
import chat.dim.protocol.InstantMessage;
import chat.dim.protocol.ReliableMessage;
import chat.dim.protocol.SecureMessage;

/**
 *  Core Processor
 *  ~~~~~~~~~~~~~~
 */
public abstract class Processor implements chat.dim.Processor {

    private final WeakReference<Transceiver> transceiverRef;

    protected Processor(Transceiver transceiver) {
        super();
        transceiverRef = new WeakReference<>(transceiver);
    }

    protected Transceiver getTransceiver() {
        return transceiverRef.get();
    }

    @Override
    public List<byte[]> process(final byte[] data) {
        final Transceiver transceiver = getTransceiver();
        // 1. deserialize message
        final ReliableMessage rMsg = transceiver.deserializeMessage(data);
        if (rMsg == null) {
            // no valid message received
            return null;
        }
        // 2. process message
        final List<ReliableMessage> responses = transceiver.process(rMsg);
        if (responses == null || responses.size() == 0) {
            // nothing to respond
            return null;
        }
        // 3. serialize message
        final List<byte[]> packages = new ArrayList<>();
        byte[] pack;
        for (ReliableMessage res: responses) {
            pack = transceiver.serializeMessage(res);
            if (pack == null) {
                // should not happen
                continue;
            }
            packages.add(pack);
        }
        return packages;
    }

    @Override
    public List<ReliableMessage> process(final ReliableMessage rMsg) {
        // TODO: override to check broadcast message before calling it
        final Transceiver transceiver = getTransceiver();
        // 1. verify message
        final SecureMessage sMsg = transceiver.verifyMessage(rMsg);
        if (sMsg == null) {
            // waiting for sender's meta if not exists
            return null;
        }
        // 2. process message
        final List<SecureMessage> responses = transceiver.process(sMsg, rMsg);
        if (responses == null || responses.size() == 0) {
            // nothing to respond
            return null;
        }
        // 3. sign messages
        final List<ReliableMessage> messages = new ArrayList<>();
        ReliableMessage msg;
        for (SecureMessage res : responses) {
            msg = transceiver.signMessage(res);
            if (msg == null) {
                // should not happen
                continue;
            }
            messages.add(msg);
        }
        return messages;
        // TODO: override to deliver to the receiver when catch exception "receiver error ..."
    }

    @Override
    public List<SecureMessage> process(final SecureMessage sMsg, final ReliableMessage rMsg) {
        final Transceiver transceiver = getTransceiver();
        // 1. decrypt message
        final InstantMessage iMsg = transceiver.decryptMessage(sMsg);
        if (iMsg == null) {
            // cannot decrypt this message, not for you?
            // delivering message to other receiver?
            return null;
        }
        // 2. process message
        final List<InstantMessage> responses = transceiver.process(iMsg, rMsg);
        if (responses == null || responses.size() == 0) {
            // nothing to respond
            return null;
        }
        // 3. encrypt messages
        final List<SecureMessage> messages = new ArrayList<>();
        SecureMessage msg;
        for (InstantMessage res : responses) {
            msg = transceiver.encryptMessage(res);
            if (msg == null) {
                // should not happen
                continue;
            }
            messages.add(msg);
        }
        return messages;
    }

    @Override
    public List<InstantMessage> process(final InstantMessage iMsg, final ReliableMessage rMsg) {
        final Transceiver transceiver = getTransceiver();
        // 1. process content
        final List<Content> responses = transceiver.process(iMsg.getContent(), rMsg);
        if (responses == null || responses.size() == 0) {
            // nothing to respond
            return null;
        }
        // 2. select a local user to build message
        final ID sender = iMsg.getSender();
        final ID receiver = iMsg.getReceiver();
        final User user = transceiver.selectLocalUser(receiver);
        assert user != null : "receiver error: " + receiver;
        // 3. pack messages
        final List<InstantMessage> messages = new ArrayList<>();
        Envelope env;
        for (Content res : responses) {
            if (res == null) {
                // should not happen
                continue;
            }
            env = Envelope.create(user.identifier, sender, null);
            messages.add(InstantMessage.create(env, res));
        }
        return messages;
    }
}
