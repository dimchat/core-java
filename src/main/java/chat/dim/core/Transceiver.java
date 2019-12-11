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
import java.nio.charset.Charset;
import java.util.Map;

import chat.dim.*;
import chat.dim.crypto.SymmetricKey;
import chat.dim.format.Base64;
import chat.dim.format.JSON;
import chat.dim.impl.SymmetricKeyImpl;
import chat.dim.protocol.*;

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

    protected ID getID(Object string) {
        EntityDelegate delegate = getEntityDelegate();
        return delegate.getID(string);
    }

    protected User getUser(ID identifier) {
        EntityDelegate delegate = getEntityDelegate();
        return delegate.getUser(identifier);
    }

    protected Group getGroup(ID identifier) {
        EntityDelegate delegate = getEntityDelegate();
        return delegate.getGroup(identifier);
    }

    protected SymmetricKey getCipherKey(ID from, ID to) {
        CipherKeyDelegate delegate = getCipherKeyDelegate();
        return delegate.getCipherKey(from, to);
    }

    protected SymmetricKey reuseCipherKey(ID from, ID to, SymmetricKey oldKey) {
        CipherKeyDelegate delegate = getCipherKeyDelegate();
        return delegate.reuseCipherKey(from, to, oldKey);
    }

    protected void cacheCipherKey(ID from, ID to, SymmetricKey key) {
        CipherKeyDelegate delegate = getCipherKeyDelegate();
        delegate.cacheCipherKey(from, to, key);
    }

    //--------

    private boolean isBroadcast(Message msg) {
        ID receiver = getID(msg.getGroup());
        if (receiver == null) {
            receiver = getID(msg.envelope.receiver);
        }
        return receiver.isBroadcast();
    }

    protected SymmetricKey getSymmetricKey(Map<String, Object> password) {
        try {
            return SymmetricKeyImpl.getInstance(password);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    private SymmetricKey getSymmetricKey(ID from, ID to) {
        // 1. get old key from store
        SymmetricKey oldKey = getCipherKey(from, to);
        // 2. get new key from delegate
        SymmetricKey newKey = reuseCipherKey(from, to, oldKey);
        if (newKey == null) {
            if (oldKey == null) {
                // 3. create a new key
                try {
                    newKey = SymmetricKeyImpl.generate(SymmetricKey.AES);
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                    return null;
                }
            } else {
                newKey = oldKey;
            }
        }
        // 4. update new key into the key store
        if (!newKey.equals(oldKey)) {
            cacheCipherKey(from, to, newKey);
        }
        return newKey;
    }

    //-------- Transform

    public SecureMessage encryptMessage(InstantMessage iMsg) {
        ID sender = getID(iMsg.envelope.sender);
        ID receiver = getID(iMsg.envelope.receiver);
        // if 'group' exists and the 'receiver' is a group ID,
        // they must be equal
        ID group = getID(iMsg.getGroup());

        // 1. get symmetric key
        SymmetricKey password;
        if (group == null) {
            password = getSymmetricKey(sender, receiver);
        } else {
            // group message
            password = getSymmetricKey(sender, group);
        }
        // check message delegate
        if (iMsg.getDelegate() == null) {
            iMsg.setDelegate(this);
        }
        assert iMsg.content != null;

        // 2. encrypt 'content' to 'data' for receiver/group members
        SecureMessage sMsg;
        if (receiver.getType().isGroup()) {
            // group message
            Group grp = getGroup(receiver);
            sMsg = iMsg.encrypt(password, grp.getMembers());
        } else {
            // personal message (or split group message)
            assert receiver.getType().isUser();
            sMsg = iMsg.encrypt(password);
        }

        // OK
        return sMsg;
    }

    public ReliableMessage signMessage(SecureMessage sMsg) {
        if (sMsg.getDelegate() == null) {
            sMsg.setDelegate(this);
        }
        assert sMsg.getData() != null;
        // sign 'data' by sender
        return sMsg.sign();
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
        assert rMsg.getSignature() != null;
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
        assert sMsg.getData() != null;
        // decrypt 'data' to 'content'
        return sMsg.decrypt();

        // TODO: check top-secret message
        //       (do it by application)
    }

    //-------- De/serialize message content and symmetric key

    protected byte[] serializeContent(Content content, InstantMessage iMsg) {
        assert content == iMsg.content;
        String json = JSON.encode(content);
        return json.getBytes(Charset.forName("UTF-8"));
    }

    protected byte[] serializeKey(SymmetricKey password, InstantMessage iMsg) {
        assert !isBroadcast(iMsg); // broadcast message has no key
        String json = JSON.encode(password);
        return json.getBytes(Charset.forName("UTF-8"));
    }

    protected byte[] serializeMessage(ReliableMessage rMsg) {
        String json = JSON.encode(rMsg);
        return json.getBytes(Charset.forName("UTF-8"));
    }

    @SuppressWarnings("unchecked")
    protected ReliableMessage deserializeMessage(byte[] data) {
        String json = new String(data, Charset.forName("UTF-8"));
        Map<String, Object> dict = (Map<String, Object>) JSON.decode(json);
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

    @SuppressWarnings("unchecked")
    protected SymmetricKey deserializeKey(byte[] key, SecureMessage sMsg) {
        assert !isBroadcast(sMsg); // broadcast message has no key
        String json = new String(key, Charset.forName("UTF-8"));
        Map<String, Object> dict = (Map<String, Object>) JSON.decode(json);
        // TODO: translate short keys
        //       'A' -> 'algorithm'
        //       'D' -> 'data'
        //       'V' -> 'iv'
        //       'M' -> 'mode'
        //       'P' -> 'padding'
        return getSymmetricKey(dict);
    }

    @SuppressWarnings("unchecked")
    protected Content deserializeContent(byte[] data, SecureMessage sMsg) {
        assert sMsg.getData() != null;
        String json = new String(data, Charset.forName("UTF-8"));
        Map<String, Object> dict = (Map<String, Object>) JSON.decode(json);
        // TODO: translate short keys
        //       'T' -> 'type'
        //       'N' -> 'sn'
        //       'G' -> 'group'
        return Content.getInstance(dict);
    }

    //-------- InstantMessageDelegate

    @Override
    public byte[] encryptContent(Content content, Map<String, Object> password, InstantMessage iMsg) {
        // NOTICE: check attachment for File/Image/Audio/Video message content
        //         before serialize content, this job should be do in subclass
        SymmetricKey key = getSymmetricKey(password);
        assert key == password && key != null;
        byte[] data = serializeContent(content, iMsg);
        return key.encrypt(data);
    }

    @Override
    public Object encodeData(byte[] data, InstantMessage iMsg) {
        if (isBroadcast(iMsg)) {
            // broadcast message content will not be encrypted (just encoded to JsON),
            // so no need to encode to Base64 here
            return new String(data, Charset.forName("UTF-8"));
        }
        return Base64.encode(data);
    }

    @Override
    public byte[] encryptKey(Map<String, Object> password, Object receiver, InstantMessage iMsg) {
        if (isBroadcast(iMsg)) {
            // broadcast message has no key
            return null;
        }
        SymmetricKey key = getSymmetricKey(password);
        assert key == password;
        // TODO: check whether support reused key

        byte[] data = serializeKey(key, iMsg);
        // encrypt with receiver's public key
        User contact = getUser(getID(receiver));
        assert contact != null;
        return contact.encrypt(data);
    }

    @Override
    public Object encodeKey(byte[] key, InstantMessage iMsg) {
        assert !isBroadcast(iMsg); // broadcast message has no key
        return Base64.encode(key);
    }

    //-------- SecureMessageDelegate

    @Override
    public byte[] decodeKey(Object key, SecureMessage sMsg) {
        assert !isBroadcast(sMsg); // broadcast message has no key
        return Base64.decode((String) key);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> decryptKey(byte[] keyData, Object sender, Object receiver, SecureMessage sMsg) {
        assert !isBroadcast(sMsg) || keyData == null; // broadcast message has no key
        ID from = getID(sender);
        ID to = getID(receiver);
        SymmetricKey password = null;
        if (keyData == null) {
            // if key data is empty, get it from key store
            password = getCipherKey(from, to);
        } else {
            // decrypt key data with the receiver/group member's private key
            ID identifier = getID(sMsg.envelope.receiver);
            User user = getUser(identifier);
            assert user != null;
            byte[] plaintext = user.decrypt(keyData);
            if (plaintext == null || plaintext.length == 0) {
                throw new NullPointerException("failed to decrypt key in msg: " + sMsg);
            }
            // deserialize it to symmetric key
            password = deserializeKey(plaintext, sMsg);
            // cache the new key in key store
            cacheCipherKey(from, to, password);
        }
        assert password != null;
        return password;
    }

    @Override
    public byte[] decodeData(Object data, SecureMessage sMsg) {
        if (isBroadcast(sMsg)) {
            // broadcast message content will not be encrypted (just encoded to JsON),
            // so return the string data directly
            String json = (String) data;
            return json.getBytes(Charset.forName("UTF-8"));
        }
        return Base64.decode((String) data);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Content decryptContent(byte[] data, Map<String, Object> password, SecureMessage sMsg) {
        SymmetricKey key = getSymmetricKey(password);
        assert key == password;
        if (key == null) {
            // irregular symmetric key
            return null;
        }
        // decrypt message.data to content
        byte[] plaintext = key.decrypt(data);
        if (plaintext == null) {
            //throw new NullPointerException("failed to decrypt data: " + password);
            return null;
        }
        // NOTICE: check attachment for File/Image/Audio/Video message content
        //         after deserialize content, this job should be do in subclass
        return deserializeContent(plaintext, sMsg);
    }

    @Override
    public byte[] signData(byte[] data, Object sender, SecureMessage sMsg) {
        ID from = getID(sender);
        User user = getUser(from);
        assert user != null;
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
        User contact = getUser(getID(sender));
        assert contact != null;
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
        // - HandshakeCommand
        // - MetaCommand
        //   - ProfileCommand
        // - ReceiptCommand

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
