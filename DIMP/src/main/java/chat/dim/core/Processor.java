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
import java.util.List;
import java.util.Map;

import chat.dim.User;
import chat.dim.crypto.SymmetricKey;
import chat.dim.format.JSON;
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

    private final WeakReference<Message.Delegate> delegateRef;

    protected Processor(Message.Delegate delegate) {
        super();
        delegateRef = new WeakReference<>(delegate);
    }

    protected Message.Delegate getDelegate() {
        return delegateRef.get();
    }

    protected abstract User getLocalUser(ID receiver);
    protected abstract List<ID> getMembers(ID group);
    protected abstract SymmetricKey getSymmetricKey(ID from, ID to);

    //
    //  InstantMessage -> SecureMessage -> ReliableMessage -> Data
    //

    public SecureMessage encryptMessage(InstantMessage iMsg) {
        // check message delegate
        if (iMsg.getDelegate() == null) {
            iMsg.setDelegate(getDelegate());
        }
        ID sender = iMsg.getSender();
        ID receiver = iMsg.getReceiver();
        // if 'group' exists and the 'receiver' is a group ID,
        // they must be equal

        // NOTICE: while sending group message, don't split it before encrypting.
        //         this means you could set group ID into message content, but
        //         keep the "receiver" to be the group ID;
        //         after encrypted (and signed), you could split the message
        //         with group members before sending out, or just send it directly
        //         to the group assistant to let it split messages for you!
        //    BUT,
        //         if you don't want to share the symmetric key with other members,
        //         you could split it (set group ID into message content and
        //         set contact ID to the "receiver") before encrypting, this usually
        //         for sending group command to assistant robot, which should not
        //         share the symmetric key (group msg key) with other members.

        // 1. get symmetric key
        ID group = getDelegate().getOvertGroup(iMsg.getContent());
        SymmetricKey password;
        if (group == null) {
            // personal message or (group) command
            password = getSymmetricKey(sender, receiver);
            assert password != null : "failed to get msg key: " + sender + " -> " + receiver;
        } else {
            // group message (excludes group command)
            password = getSymmetricKey(sender, group);
            assert password != null : "failed to get group msg key: " + sender + " -> " + group;
        }

        // 2. encrypt 'content' to 'data' for receiver/group members
        SecureMessage sMsg;
        if (ID.isGroup(receiver)) {
            // group message
            sMsg = iMsg.encrypt(password, getMembers(receiver));
        } else {
            // personal message (or split group message)
            sMsg = iMsg.encrypt(password);
        }
        if (sMsg == null) {
            // public key for encryption not found
            // TODO: suspend this message for waiting receiver's meta
            return null;
        }

        // overt group ID
        if (group != null && !receiver.equals(group)) {
            // NOTICE: this help the receiver knows the group ID
            //         when the group message separated to multi-messages,
            //         if don't want the others know you are the group members,
            //         remove it.
            sMsg.getEnvelope().setGroup(group);
        }

        // NOTICE: copy content type to envelope
        //         this help the intermediate nodes to recognize message type
        sMsg.getEnvelope().setType(iMsg.getContent().getType());

        // OK
        return sMsg;
    }

    public ReliableMessage signMessage(SecureMessage sMsg) {
        // check message delegate
        if (sMsg.getDelegate() == null) {
            sMsg.setDelegate(getDelegate());
        }
        assert sMsg.getData() != null : "message data cannot be empty";
        // sign 'data' by sender
        return sMsg.sign();
    }

    public byte[] serializeMessage(ReliableMessage rMsg) {
        return JSON.encode(rMsg);
    }

    //
    //  Data -> ReliableMessage -> SecureMessage -> InstantMessage
    //

    @SuppressWarnings("unchecked")
    public ReliableMessage deserializeMessage(byte[] data) {
        Map<String, Object> dict = (Map<String, Object>) JSON.decode(data);
        // TODO: translate short keys
        //       'S' -> 'sender'
        //       'R' -> 'receiver'
        //       'W' -> 'time'
        //       'T' -> 'type'
        //       'G' -> 'group'
        //       ------------------
        //       'D' -> 'data'
        //       'V' -> 'signature'
        //       'K' -> 'key'
        //       ------------------
        //       'M' -> 'meta'
        return ReliableMessage.parse(dict);
    }

    public SecureMessage verifyMessage(ReliableMessage rMsg) {
        // check message delegate
        if (rMsg.getDelegate() == null) {
            rMsg.setDelegate(getDelegate());
        }
        //
        //  TODO: check [Visa Protocol]
        //        make sure the sender's meta(visa) exists
        //        (do in by application)
        //

        assert rMsg.getSignature() != null : "message signature cannot be empty";
        // verify 'data' with 'signature'
        return rMsg.verify();
    }

    public InstantMessage decryptMessage(SecureMessage sMsg) {
        // check message delegate
        if (sMsg.getDelegate() == null) {
            sMsg.setDelegate(getDelegate());
        }
        //
        //  NOTICE: make sure the receiver is YOU!
        //          which means the receiver's private key exists;
        //          if the receiver is a group ID, split it first
        //

        assert sMsg.getData() != null : "message data cannot be empty";
        // decrypt 'data' to 'content'
        return sMsg.decrypt();

        // TODO: check top-secret message
        //       (do it by application)
    }

    //
    //  Processing Message
    //

    public byte[] process(byte[] data) {
        // 1. deserialize message
        ReliableMessage rMsg = deserializeMessage(data);
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
        return serializeMessage(rMsg);
    }

    // TODO: override to check broadcast message before calling it
    // TODO: override to deliver to the receiver when catch exception "receiver error ..."
    public ReliableMessage process(ReliableMessage rMsg) {
        // 1. verify message
        SecureMessage sMsg = verifyMessage(rMsg);
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
        return signMessage(sMsg);
    }

    protected SecureMessage process(SecureMessage sMsg, ReliableMessage rMsg) {
        // 1. decrypt message
        InstantMessage iMsg = decryptMessage(sMsg);
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
        return encryptMessage(iMsg);
    }

    protected InstantMessage process(InstantMessage iMsg, ReliableMessage rMsg) {
        // check message delegate
        if (iMsg.getDelegate() == null) {
            iMsg.setDelegate(getDelegate());
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
        User user = getLocalUser(receiver);
        assert user != null : "receiver error: " + receiver;

        // pack message
        Envelope env = Envelope.create(user.identifier, sender, null);
        return InstantMessage.create(env, response);
    }

    protected abstract Content process(Content content, ReliableMessage rMsg);
}
