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
import chat.dim.SecureMessageDelegate;
import chat.dim.User;
import chat.dim.crypto.SymmetricKey;
import chat.dim.format.Base64;
import chat.dim.format.JSON;
import chat.dim.format.UTF8;
import chat.dim.protocol.AudioContent;
import chat.dim.protocol.Command;
import chat.dim.protocol.ContentType;
import chat.dim.protocol.FileContent;
import chat.dim.protocol.HistoryCommand;
import chat.dim.protocol.ImageContent;
import chat.dim.protocol.PageContent;
import chat.dim.protocol.TextContent;
import chat.dim.protocol.VideoContent;

public class Transceiver implements InstantMessageDelegate, SecureMessageDelegate, ReliableMessageDelegate {

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

    private boolean isBroadcast(Message msg) {
        Object receiver;
        if (msg instanceof InstantMessage) {
            receiver = ((InstantMessage) msg).content.getGroup();
        } else {
            receiver = msg.envelope.getGroup();
        }
        if (receiver == null) {
            receiver = msg.envelope.receiver;
        }
        ID identifier = getEntityDelegate().getID(receiver);
        return identifier != null && identifier.isBroadcast();
    }

    protected SymmetricKey getSymmetricKey(Map<String, Object> password) {
        try {
            return SymmetricKey.getInstance(password);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
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

    private ID getOvertGroup(Content content) {
        Object group = content.getGroup();
        if (group == null) {
            return null;
        }
        ID identifier = getEntityDelegate().getID(group);
        if (identifier.isBroadcast()) {
            // broadcast message is always overt
            return identifier;
        }
        if (content instanceof Command) {
            // group command should be sent to each member directly, so
            // don't expose group ID
            return null;
        }
        return identifier;
    }

    public SecureMessage encryptMessage(InstantMessage iMsg) {
        EntityDelegate barrack = getEntityDelegate();
        ID sender = barrack.getID(iMsg.envelope.sender);
        ID receiver = barrack.getID(iMsg.envelope.receiver);
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
        ID group = getOvertGroup(iMsg.content);
        SymmetricKey password;
        if (group == null) {
            // personal message or (group) command
            password = getSymmetricKey(sender, receiver);
            assert password != null : "failed to get msg key: " + sender + " -> " + receiver;
        } else {
            // group message (excludes group command)
            password = getSymmetricKey(sender, group);
            assert password != null : "failed to get msg key: " + sender + " -> " + group;
        }

        // check message delegate
        if (iMsg.getDelegate() == null) {
            iMsg.setDelegate(this);
        }

        // 2. encrypt 'content' to 'data' for receiver/group members
        SecureMessage sMsg;
        if (receiver.isGroup()) {
            // group message
            Group grp = barrack.getGroup(receiver);
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
            sMsg.envelope.setGroup(group);
        }

        // NOTICE: copy content type to envelope
        //         this help the intermediate nodes to recognize message type
        sMsg.envelope.setType(iMsg.content.type);

        // OK
        return sMsg;
    }

    public ReliableMessage signMessage(SecureMessage sMsg) {
        if (sMsg.getDelegate() == null) {
            sMsg.setDelegate(this);
        }
        assert sMsg.getData() != null : "message data cannot be empty";
        // sign 'data' by sender
        return sMsg.sign();
    }

    protected byte[] serializeMessage(ReliableMessage rMsg) {
        return JSON.encode(rMsg);
    }

    @SuppressWarnings("unchecked")
    protected ReliableMessage deserializeMessage(byte[] data) {
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

    public SecureMessage verifyMessage(ReliableMessage rMsg) {
        //
        //  TODO: check [Meta Protocol]
        //        make sure the sender's meta exists
        //        (do in by application)
        //

        if (rMsg.getDelegate() == null) {
            rMsg.setDelegate(this);
        }
        assert rMsg.getSignature() != null : "message signature cannot be empty";
        // verify 'data' with 'signature'
        return rMsg.verify();
    }

    public InstantMessage decryptMessage(SecureMessage sMsg) {
        //
        //  NOTICE: make sure the receiver is YOU!
        //          which means the receiver's private key exists;
        //          if the receiver is a group ID, split it first
        //

        if (sMsg.getDelegate() == null) {
            sMsg.setDelegate(this);
        }
        assert sMsg.getData() != null : "message data cannot be empty";
        // decrypt 'data' to 'content'
        return sMsg.decrypt();

        // TODO: check top-secret message
        //       (do it by application)
    }

    //-------- InstantMessageDelegate

    @Override
    public byte[] serializeContent(Content content, Map<String, Object> password, InstantMessage iMsg) {
        // NOTICE: check attachment for File/Image/Audio/Video message content
        //         before serialize content, this job should be do in subclass

        assert content == iMsg.content : "message content not match: " + content;
        return JSON.encode(content);
    }

    @Override
    public byte[] encryptContent(byte[] data, Map<String, Object> password, InstantMessage iMsg) {
        SymmetricKey key = getSymmetricKey(password);
        assert key != null && key == password : "irregular symmetric key: " + password;
        return key.encrypt(data);
    }

    @Override
    public Object encodeData(byte[] data, InstantMessage iMsg) {
        if (isBroadcast(iMsg)) {
            // broadcast message content will not be encrypted (just encoded to JsON),
            // so no need to encode to Base64 here
            return UTF8.decode(data);
        }
        return Base64.encode(data);
    }

    @Override
    public byte[] serializeKey(Map<String, Object> password, InstantMessage iMsg) {
        if (isBroadcast(iMsg)) {
            // broadcast message has no key
            return null;
        }
        return JSON.encode(password);
    }

    @Override
    public byte[] encryptKey(byte[] data, Object receiver, InstantMessage iMsg) {
        assert !isBroadcast(iMsg) : "broadcast message has no key: " + iMsg;
        // TODO: make sure the receiver's public key exists
        EntityDelegate barrack = getEntityDelegate();
        User contact = barrack.getUser(barrack.getID(receiver));
        assert contact != null : "failed to get encrypt key for receiver: " + receiver;
        // encrypt with receiver's public key
        return contact.encrypt(data);
    }

    @Override
    public Object encodeKey(byte[] key, InstantMessage iMsg) {
        assert !isBroadcast(iMsg) : "broadcast message has no key: " + iMsg;
        return Base64.encode(key);
    }

    //-------- SecureMessageDelegate

    @Override
    public byte[] decodeKey(Object key, SecureMessage sMsg) {
        assert !isBroadcast(sMsg) : "broadcast message has no key: " + sMsg;
        return Base64.decode((String) key);
    }

    @Override
    @SuppressWarnings("unchecked")
    public byte[] decryptKey(byte[] key, Object sender, Object receiver, SecureMessage sMsg) {
        assert !isBroadcast(sMsg) : "broadcast message has no key: " + sMsg;
        // decrypt key data with the receiver/group member's private key
        EntityDelegate barrack = getEntityDelegate();
        ID identifier = barrack.getID(sMsg.envelope.receiver);
        User user = barrack.getUser(identifier);
        assert user != null : "failed to get decrypt keys: " + identifier;
        return user.decrypt(key);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> deserializeKey(byte[] key, Object sender, Object receiver, SecureMessage sMsg) {
        if (key == null) {
            // get key from cache
            EntityDelegate barrack = getEntityDelegate();
            CipherKeyDelegate keyCache = getCipherKeyDelegate();
            ID from = barrack.getID(sender);
            ID to = barrack.getID(receiver);
            return keyCache.getCipherKey(from, to);
        } else {
            assert !isBroadcast(sMsg) : "broadcast message has no key: " + sMsg;
            Map<String, Object> dict = (Map<String, Object>) JSON.decode(key);
            // TODO: translate short keys
            //       'A' -> 'algorithm'
            //       'D' -> 'data'
            //       'V' -> 'iv'
            //       'M' -> 'mode'
            //       'P' -> 'padding'
            return getSymmetricKey(dict);
        }
    }

    @Override
    public byte[] decodeData(Object data, SecureMessage sMsg) {
        if (isBroadcast(sMsg)) {
            // broadcast message content will not be encrypted (just encoded to JsON),
            // so return the string data directly
            return UTF8.encode((String) data);
        }
        return Base64.decode((String) data);
    }

    @Override
    @SuppressWarnings("unchecked")
    public byte[] decryptContent(byte[] data, Map<String, Object> password, SecureMessage sMsg) {
        SymmetricKey key = getSymmetricKey(password);
        if (key == null) {
            throw new NullPointerException("irregular symmetric key: " + password);
        }
        return key.decrypt(data);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Content deserializeContent(byte[] data, Map<String, Object> password, SecureMessage sMsg) {
        assert sMsg.getData() != null;
        Map<String, Object> dict = (Map<String, Object>) JSON.decode(data);
        // TODO: translate short keys
        //       'T' -> 'type'
        //       'N' -> 'sn'
        //       'G' -> 'group'
        Content content = Content.getInstance(dict);

        if (!isBroadcast(sMsg)) {
            SymmetricKey key = getSymmetricKey(password);
            assert key == password : "irregular symmetric key: " + password;
            // check and cache key for reuse
            EntityDelegate barrack = getEntityDelegate();
            ID sender = barrack.getID(sMsg.envelope.sender);
            ID group = getOvertGroup(content);
            if (group == null) {
                ID receiver = barrack.getID(sMsg.envelope.receiver);
                // personal message or (group) command
                // cache key with direction (sender -> receiver)
                getCipherKeyDelegate().cacheCipherKey(sender, receiver, key);
            } else {
                // group message (excludes group command)
                // cache the key with direction (sender -> group)
                getCipherKeyDelegate().cacheCipherKey(sender, group, key);
            }
        }

        // NOTICE: check attachment for File/Image/Audio/Video message content
        //         after deserialize content, this job should be do in subclass
        return content;
    }

    @Override
    public byte[] signData(byte[] data, Object sender, SecureMessage sMsg) {
        EntityDelegate barrack = getEntityDelegate();
        ID from = barrack.getID(sender);
        User user = barrack.getUser(from);
        assert user != null : "failed to get sign key for sender: " + sender;
        return user.sign(data);
    }

    @Override
    public Object encodeSignature(byte[] signature, SecureMessage sMsg) {
        return Base64.encode(signature);
    }

    //-------- ReliableMessageDelegate

    @Override
    public byte[] decodeSignature(Object signature, ReliableMessage rMsg) {
        return Base64.decode((String) signature);
    }

    @Override
    public boolean verifyDataSignature(byte[] data, byte[] signature, Object sender, ReliableMessage rMsg) {
        EntityDelegate barrack = getEntityDelegate();
        User contact = barrack.getUser(barrack.getID(sender));
        assert contact != null : "failed to get verify key for sender: " + sender;
        return contact.verify(data, signature);
    }

    static {
        // Text
        Content.register(ContentType.TEXT, TextContent.class);

        // File
        Content.register(ContentType.FILE, FileContent.class);
        // - Image
        Content.register(ContentType.IMAGE, ImageContent.class);
        // - Audio
        Content.register(ContentType.AUDIO, AudioContent.class);
        // - Video
        Content.register(ContentType.VIDEO, VideoContent.class);

        // Page
        Content.register(ContentType.PAGE, PageContent.class);

        // Quote

        // Command
        Content.register(ContentType.COMMAND, Command.class);
        // - MetaCommand
        //   - ProfileCommand

        // History
        Content.register(ContentType.HISTORY, HistoryCommand.class);
        // - GroupCommand
        //   - InviteCommand
        //   - ExpelCommand
        //   - JoinCommand
        //   - QuitCommand
        //   - QueryCommand
        //   - ResetCommand

        // ...
    }
}
