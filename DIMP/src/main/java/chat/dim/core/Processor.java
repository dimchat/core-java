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

import chat.dim.EntityDelegate;
import chat.dim.User;
import chat.dim.protocol.Content;
import chat.dim.protocol.Envelope;
import chat.dim.protocol.ID;
import chat.dim.protocol.InstantMessage;
import chat.dim.protocol.Message;
import chat.dim.protocol.ReliableMessage;
import chat.dim.protocol.SecureMessage;

/**
 *  Message Processor
 *  ~~~~~~~~~~~~~~~~~
 */
public abstract class Processor {

    private final WeakReference<EntityDelegate> entityDelegateRef;
    private final WeakReference<Message.Delegate> messageDelegateRef;
    private final WeakReference<Packer> packerRef;

    protected Processor(EntityDelegate barrack, Message.Delegate transceiver, Packer packer) {
        super();
        entityDelegateRef = new WeakReference<>(barrack);
        messageDelegateRef = new WeakReference<>(transceiver);
        packerRef = new WeakReference<>(packer);
    }

    protected EntityDelegate getEntityDelegate() {
        return entityDelegateRef.get();
    }
    protected Message.Delegate getMessageDelegate() {
        return messageDelegateRef.get();
    }
    protected Packer getPacker() {
        return packerRef.get();
    }

    //
    //  Processing Message
    //

    public byte[] process(byte[] data) {
        // 1. deserialize message
        ReliableMessage rMsg = getPacker().deserializeMessage(data);
        if (rMsg == null) {
            // no message received
            return null;
        }
        // 2. process message
        rMsg = process(rMsg);
        if (rMsg == null) {
            // nothing to respond
            return null;
        }
        // 3. serialize message
        return getPacker().serializeMessage(rMsg);
    }

    // TODO: override to check broadcast message before calling it
    // TODO: override to deliver to the receiver when catch exception "receiver error ..."
    public ReliableMessage process(ReliableMessage rMsg) {
        // 1. verify message
        SecureMessage sMsg = getPacker().verifyMessage(rMsg);
        if (sMsg == null) {
            // waiting for sender's meta if not exists
            return null;
        }
        // 2. process message
        sMsg = process(sMsg, rMsg);
        if (sMsg == null) {
            // nothing to respond
            return null;
        }
        // 3. sign message
        return getPacker().signMessage(sMsg);
    }

    protected SecureMessage process(SecureMessage sMsg, ReliableMessage rMsg) {
        // 1. decrypt message
        InstantMessage iMsg = getPacker().decryptMessage(sMsg);
        if (iMsg == null) {
            // cannot decrypt this message, not for you?
            // delivering message to other receiver?
            return null;
        }
        // 2. process message
        iMsg = process(iMsg, rMsg);
        if (iMsg == null) {
            // nothing to respond
            return null;
        }
        // 3. encrypt message
        return getPacker().encryptMessage(iMsg);
    }

    protected InstantMessage process(InstantMessage iMsg, ReliableMessage rMsg) {
        // check message delegate
        if (iMsg.getDelegate() == null) {
            iMsg.setDelegate(getMessageDelegate());
        }

        // process content from sender
        Content content = iMsg.getContent();
        Content response = process(content, rMsg);
        if (response == null) {
            // nothing to respond
            return null;
        }

        ID sender = iMsg.getSender();
        ID receiver = iMsg.getReceiver();
        User user = getEntityDelegate().selectLocalUser(receiver);
        assert user != null : "receiver error: " + receiver;

        // pack message
        Envelope env = Envelope.create(user.identifier, sender, null);
        return InstantMessage.create(env, response);
    }

    protected abstract Content process(Content content, ReliableMessage rMsg);

    /**
     *  Register Core Content/Command Factories
     */
    public static void registerCoreFactories() {
        Factories.registerContentFactories();
        Factories.registerCommandFactories();
    }
}
