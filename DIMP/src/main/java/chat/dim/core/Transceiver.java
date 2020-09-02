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
package chat.dim.core;

import java.lang.ref.WeakReference;
import java.util.Map;

import chat.dim.Content;
import chat.dim.Group;
import chat.dim.ID;
import chat.dim.InstantMessage;
import chat.dim.InstantMessageDelegate;
import chat.dim.Message;
import chat.dim.ReliableMessage;
import chat.dim.ReliableMessageDelegate;
import chat.dim.SecureMessage;
import chat.dim.User;
import chat.dim.crypto.SymmetricKey;
import chat.dim.format.Base64;
import chat.dim.format.JSON;
import chat.dim.format.UTF8;
import chat.dim.protocol.Command;

public class Transceiver implements InstantMessageDelegate<ID, SymmetricKey>, ReliableMessageDelegate<ID, SymmetricKey> {

    // delegates
    private WeakReference<EntityDelegate> entityDelegateRef = null;
    private WeakReference<CipherKeyDelegate> cipherKeyDelegateRef = null;

    public Transceiver() {
        super();
    }

    public EntityDelegate getEntityDelegate() {
        if (entityDelegateRef == null) {
            return null;
        }
        return entityDelegateRef.get();
    }

    public void setEntityDelegate(EntityDelegate delegate) {
        entityDelegateRef = new WeakReference<>(delegate);
    }

    public CipherKeyDelegate getCipherKeyDelegate() {
        if (cipherKeyDelegateRef == null) {
            return null;
        }
        return cipherKeyDelegateRef.get();
    }

    public void setCipherKeyDelegate(CipherKeyDelegate delegate) {
        cipherKeyDelegateRef = new WeakReference<>(delegate);
    }

    //--------

    private boolean isBroadcast(Message<ID> msg) {
        // check message delegate
        if (msg.getDelegate() == null) {
            msg.setDelegate(this);
        }
        ID receiver = msg.getGroup();
        if (receiver == null) {
            receiver = msg.getReceiver();
        }
        return receiver.isBroadcast();
    }

    private SymmetricKey getSymmetricKey(ID from, ID to) {
        CipherKeyDelegate keyCache = getCipherKeyDelegate();
        // get old key from cache
        SymmetricKey key = keyCache.getCipherKey(from, to);
        if (key == null) {
            try {
                // create new key and cache it
                key = SymmetricKey.generate(SymmetricKey.AES);
                assert key != null : "failed to generate AES key";
                keyCache.cacheCipherKey(from, to, key);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
                return null;
            }
        }
        return key;
    }

    //-------- Transform

    private static ID getOvertGroup(Content<ID> content) {
        ID group = content.getGroup();
        if (group == null) {
            return null;
        }
        if (group.isBroadcast()) {
            // broadcast message is always overt
            return group;
        }
        if (content instanceof Command) {
            // group command should be sent to each member directly, so
            // don't expose group ID
            return null;
        }
        return group;
    }

    public SecureMessage<ID, SymmetricKey> encryptMessage(InstantMessage<ID, SymmetricKey> iMsg) {
        // check message delegate
        if (iMsg.getDelegate() == null) {
            iMsg.setDelegate(this);
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
        ID group = getOvertGroup(iMsg.getContent());
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
        SecureMessage<ID, SymmetricKey> sMsg;
        if (receiver.isGroup()) {
            // group message
            Group grp = getEntityDelegate().getGroup(receiver);
            if (grp == null) {
                throw new NullPointerException("failed to get group: " + receiver);
            }
            sMsg = iMsg.encrypt(password, grp.getMembers());
        } else {
            // personal message (or split group message)
            assert receiver.isUser() : "receiver ID error: " + receiver;
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

    public ReliableMessage<ID, SymmetricKey> signMessage(SecureMessage<ID, SymmetricKey> sMsg) {
        // check message delegate
        if (sMsg.getDelegate() == null) {
            sMsg.setDelegate(this);
        }
        assert sMsg.getData() != null : "message data cannot be empty";
        // sign 'data' by sender
        return sMsg.sign();
    }

    protected byte[] serializeMessage(ReliableMessage<ID, SymmetricKey> rMsg) {
        return JSON.encode(rMsg);
    }

    @SuppressWarnings("unchecked")
    protected ReliableMessage<ID, SymmetricKey> deserializeMessage(byte[] data) {
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
        return ReliableMessage.getInstance(dict);
    }

    public SecureMessage<ID, SymmetricKey> verifyMessage(ReliableMessage<ID, SymmetricKey> rMsg) {
        // check message delegate
        if (rMsg.getDelegate() == null) {
            rMsg.setDelegate(this);
        }
        //
        //  TODO: check [Meta Protocol]
        //        make sure the sender's meta exists
        //        (do in by application)
        //

        assert rMsg.getSignature() != null : "message signature cannot be empty";
        // verify 'data' with 'signature'
        return rMsg.verify();
    }

    public InstantMessage<ID, SymmetricKey> decryptMessage(SecureMessage<ID, SymmetricKey> sMsg) {
        // check message delegate
        if (sMsg.getDelegate() == null) {
            sMsg.setDelegate(this);
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

    //-------- MessageDelegate

    @Override
    public ID getID(Object identifier) {
        return getEntityDelegate().getID(identifier);
    }

    @Override
    public Content<ID> getContent(Map<String, Object> dictionary) {
        Content<ID> content = chat.dim.protocol.Content.getInstance(dictionary);
        content.setDelegate(this);
        return content;
    }

    //-------- InstantMessageDelegate

    @Override
    public byte[] serializeContent(Content<ID> content, SymmetricKey password, InstantMessage<ID, SymmetricKey> iMsg) {
        // NOTICE: check attachment for File/Image/Audio/Video message content
        //         before serialize content, this job should be do in subclass
        return JSON.encode(content);
    }

    @Override
    public byte[] encryptContent(byte[] data, SymmetricKey password, InstantMessage<ID, SymmetricKey> iMsg) {
        return password.encrypt(data);
    }

    @Override
    public Object encodeData(byte[] data, InstantMessage<ID, SymmetricKey> iMsg) {
        if (isBroadcast(iMsg)) {
            // broadcast message content will not be encrypted (just encoded to JsON),
            // so no need to encode to Base64 here
            return UTF8.decode(data);
        }
        return Base64.encode(data);
    }

    @Override
    public byte[] serializeKey(SymmetricKey password, InstantMessage<ID, SymmetricKey> iMsg) {
        if (isBroadcast(iMsg)) {
            // broadcast message has no key
            return null;
        }
        return JSON.encode(password);
    }

    @Override
    public byte[] encryptKey(byte[] data, ID receiver, InstantMessage<ID, SymmetricKey> iMsg) {
        assert !isBroadcast(iMsg) : "broadcast message has no key: " + iMsg;
        // TODO: make sure the receiver's public key exists
        User contact = getEntityDelegate().getUser(receiver);
        assert contact != null : "failed to get encrypt key for receiver: " + receiver;
        // encrypt with receiver's public key
        return contact.encrypt(data);
    }

    @Override
    public Object encodeKey(byte[] key, InstantMessage<ID, SymmetricKey> iMsg) {
        assert !isBroadcast(iMsg) : "broadcast message has no key: " + iMsg;
        return Base64.encode(key);
    }

    //-------- SecureMessageDelegate

    @Override
    public byte[] decodeKey(Object key, SecureMessage<ID, SymmetricKey> sMsg) {
        assert !isBroadcast(sMsg) : "broadcast message has no key: " + sMsg;
        return Base64.decode((String) key);
    }

    @Override
    public byte[] decryptKey(byte[] key, ID sender, ID receiver, SecureMessage<ID, SymmetricKey> sMsg) {
        // NOTICE: the receiver will be group ID in a group message here
        assert !isBroadcast(sMsg) : "broadcast message has no key: " + sMsg;
        // decrypt key data with the receiver/group member's private key
        ID identifier = sMsg.getReceiver();
        User user = getEntityDelegate().getUser(identifier);
        assert user != null : "failed to get decrypt keys: " + identifier;
        return user.decrypt(key);
    }

    @SuppressWarnings("unchecked")
    @Override
    public SymmetricKey deserializeKey(byte[] key, ID sender, ID receiver, SecureMessage<ID, SymmetricKey> sMsg) {
        // NOTICE: the receiver will be group ID in a group message here
        if (key == null) {
            // get key from cache
            CipherKeyDelegate keyCache = getCipherKeyDelegate();
            return keyCache.getCipherKey(sender, receiver);
        } else {
            assert !isBroadcast(sMsg) : "broadcast message has no key: " + sMsg;
            Map<String, Object> dict = (Map<String, Object>) JSON.decode(key);
            // TODO: translate short keys
            //       'A' -> 'algorithm'
            //       'D' -> 'data'
            //       'V' -> 'iv'
            //       'M' -> 'mode'
            //       'P' -> 'padding'
            try {
                return SymmetricKey.getInstance(dict);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
                return null;
            }
        }
    }

    @Override
    public byte[] decodeData(Object data, SecureMessage<ID, SymmetricKey> sMsg) {
        if (isBroadcast(sMsg)) {
            // broadcast message content will not be encrypted (just encoded to JsON),
            // so return the string data directly
            return UTF8.encode((String) data);
        }
        return Base64.decode((String) data);
    }

    @Override
    public byte[] decryptContent(byte[] data, SymmetricKey password, SecureMessage<ID, SymmetricKey> sMsg) {
        return password.decrypt(data);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Content<ID> deserializeContent(byte[] data, SymmetricKey password, SecureMessage<ID, SymmetricKey> sMsg) {
        assert sMsg.getData() != null;
        Map<String, Object> dict = (Map<String, Object>) JSON.decode(data);
        // TODO: translate short keys
        //       'T' -> 'type'
        //       'N' -> 'sn'
        //       'G' -> 'group'
        Content<ID> content = getContent(dict);

        if (!isBroadcast(sMsg)) {
            // check and cache key for reuse
            ID sender = sMsg.getSender();
            ID group = getOvertGroup(content);
            if (group == null) {
                ID receiver = sMsg.getReceiver();
                // personal message or (group) command
                // cache key with direction (sender -> receiver)
                getCipherKeyDelegate().cacheCipherKey(sender, receiver, password);
            } else {
                // group message (excludes group command)
                // cache the key with direction (sender -> group)
                getCipherKeyDelegate().cacheCipherKey(sender, group, password);
            }
        }

        // NOTICE: check attachment for File/Image/Audio/Video message content
        //         after deserialize content, this job should be do in subclass
        return content;
    }

    @Override
    public byte[] signData(byte[] data, ID sender, SecureMessage<ID, SymmetricKey> sMsg) {
        User user = getEntityDelegate().getUser(sender);
        assert user != null : "failed to get sign key for sender: " + sender;
        return user.sign(data);
    }

    @Override
    public Object encodeSignature(byte[] signature, SecureMessage<ID, SymmetricKey> sMsg) {
        return Base64.encode(signature);
    }

    //-------- ReliableMessageDelegate

    @Override
    public byte[] decodeSignature(Object signature, ReliableMessage<ID, SymmetricKey> rMsg) {
        return Base64.decode((String) signature);
    }

    @Override
    public boolean verifyDataSignature(byte[] data, byte[] signature, ID sender, ReliableMessage<ID, SymmetricKey> rMsg) {
        User contact = getEntityDelegate().getUser(sender);
        assert contact != null : "failed to get verify key for sender: " + sender;
        return contact.verify(data, signature);
    }
}
