/* license: https://mit-license.org
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

import chat.dim.crypto.SymmetricKey;
import chat.dim.crypto.impl.SymmetricKeyImpl;
import chat.dim.dkd.*;
import chat.dim.format.Base64;
import chat.dim.format.JSON;
import chat.dim.mkm.Account;
import chat.dim.mkm.Group;
import chat.dim.mkm.User;
import chat.dim.mkm.entity.ID;
import chat.dim.mkm.entity.Meta;
import chat.dim.mkm.entity.NetworkType;
import chat.dim.protocol.ContentType;
import chat.dim.protocol.ForwardContent;
import chat.dim.protocol.file.FileContent;

import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;

public class Transceiver implements InstantMessageDelegate, SecureMessageDelegate, ReliableMessageDelegate {

    public Transceiver() {
        super();
    }

    // delegates
    public TransceiverDelegate delegate;
    public Barrack             barrack;
    public KeyCache            keyCache;

    private SymmetricKey getSymmetricKey(ID from, ID to) {
        // 1. get old key from store
        SymmetricKey oldKey = keyCache.cipherKey(from, to);
        // 2. get new key from delegate
        SymmetricKey newKey = keyCache.reuseCipherKey(from, to, oldKey);
        if (newKey == null) {
            if (oldKey == null) {
                // 3. create a new key
                try {
                    newKey = SymmetricKeyImpl.generate(SymmetricKey.AES);
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            } else {
                newKey = oldKey;
            }
        }
        // 4. update new key into the key store
        if (newKey != null && !newKey.equals(oldKey)) {
            keyCache.cacheCipherKey(from, to, newKey);
        }
        return newKey;
    }

    private SymmetricKey getSymmetricKey(Map<String, Object> password, ID from, ID to) {
        SymmetricKey key = getSymmetricKey(password);
        if (key != null) {
            // cache the new key in key store
            keyCache.cacheCipherKey(from, to, key);
        }
        return key;
    }

    private SymmetricKey getSymmetricKey(Map<String, Object> password) {
        try {
            return SymmetricKeyImpl.getInstance(password);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    private boolean isBroadcast(Message msg) {
        ID receiver = barrack.getID(msg.getGroup());
        if (receiver == null) {
            receiver = barrack.getID(msg.envelope.receiver);
        }
        return receiver.isBroadcast();
    }

    /**
     *  Send message (secured + certified) to target station
     *
     * @param iMsg - instant message
     * @param callback - callback function
     * @param split - if it's a group message, split it before sending out
     * @return NO on data/delegate error
     * @throws NoSuchFieldException when 'group' not found
     */
    public boolean sendMessage(InstantMessage iMsg, Callback callback, boolean split)
            throws NoSuchFieldException {
        // transforming
        ReliableMessage rMsg = encryptAndSignMessage(iMsg);
        if (rMsg == null) {
            // TODO: set iMsg.state = error
            throw new NullPointerException("failed to encrypt and sign message: " + iMsg);
        }

        // trying to send out
        boolean OK = true;
        ID receiver = barrack.getID(iMsg.envelope.receiver);
        if (split && receiver.getType().isGroup()) {
            Group group = barrack.getGroup(receiver);
            List<ID> members = group == null ? null : group.getMembers();
            List<SecureMessage> messages = members == null ? null : rMsg.split(members);
            if (messages == null || messages.size() == 0) {
                // failed to split msg, send it to group
                OK = sendMessage(rMsg, callback);
            } else {
                for (SecureMessage msg: messages) {
                    if (!sendMessage((ReliableMessage) msg, callback)) {
                        OK = false;
                    }
                }
            }
        } else {
            OK = sendMessage(rMsg, callback);
        }

        // TODO: if OK, set iMsg.state = sending; else, set iMsg.state = waiting;
        return OK;
    }

    private boolean sendMessage(ReliableMessage rMsg, Callback callback) {
        CompletionHandler handler = new CompletionHandler() {
            @Override
            public void onSuccess() {
                callback.onFinished(rMsg, null);
            }

            @Override
            public void onFailed(Error error) {
                callback.onFinished(rMsg, error);
            }
        };
        String json = JSON.encode(rMsg);
        byte[] data = json.getBytes(Charset.forName("UTF-8"));
        return delegate.sendPackage(data, handler);
    }

    /**
     *  Pack instant message to reliable message for delivering
     *
     * @param iMsg - instant message
     * @return ReliableMessage Object
     * @throws NoSuchFieldException when encrypt message content
     */
    public ReliableMessage encryptAndSignMessage(InstantMessage iMsg) throws NoSuchFieldException {
        if (iMsg.delegate == null) {
            iMsg.delegate = this;
        }
        ID sender = barrack.getID(iMsg.envelope.sender);
        ID receiver = barrack.getID(iMsg.envelope.receiver);
        // if 'group' exists and the 'receiver' is a group ID,
        // they must be equal
        Group group = null;
        if (receiver.getType().isGroup()) {
            group = barrack.getGroup(receiver);
        } else {
            Object gid = iMsg.getGroup();
            if (gid != null) {
                group = barrack.getGroup(barrack.getID(gid));
            }
        }

        // 1. encrypt 'content' to 'data' for receiver
        SecureMessage sMsg;
        if (group == null) {
            // personal message
            SymmetricKey password = getSymmetricKey(sender, receiver);
            sMsg = iMsg.encrypt(password);
        } else {
            // group message
            SymmetricKey password = getSymmetricKey(sender, group.identifier);
            sMsg = iMsg.encrypt(password, group.getMembers());
        }

        // 2. sign 'data' by sender
        if (sMsg.delegate == null) {
            sMsg.delegate = this;
        }
        return sMsg.sign();
    }

    /**
     *  Extract instant message from a reliable message received
     *
     * @param rMsg - reliable message
     * @return InstantMessage object
     * @throws ClassNotFoundException when saving meta
     */
    public InstantMessage verifyAndDecryptMessage(ReliableMessage rMsg) throws ClassNotFoundException {
        ID sender = barrack.getID(rMsg.envelope.sender);
        // [Meta Protocol] check meta in first contact message
        Meta meta = barrack.getMeta(sender);
        if (meta == null) {
            // first contact, try meta in message package
            meta = Meta.getInstance(rMsg.getMeta());
            if (meta == null) {
                // TODO: query meta for sender from DIM network
                throw new NullPointerException("failed to get meta for sender: " + sender);
            }
            assert meta.matches(sender);
            if (!barrack.saveMeta(meta, sender)) {
                throw new IllegalArgumentException("save meta error: " + sender + ", " + meta);
            }
        }

        // 1. verify 'data' with 'signature'
        if (rMsg.delegate == null) {
            rMsg.delegate = this;
        }
        SecureMessage sMsg = rMsg.verify();

        // 2. decrypt 'data' to 'content'
        if (sMsg.delegate == null) {
            sMsg.delegate = this;
        }
        InstantMessage iMsg = sMsg.decrypt();

        // 3. check: top-secret message
        if (iMsg.content.type == ContentType.FORWARD.value) {
            // do it again to drop the wrapper,
            // the secret inside the content is the real message
            ForwardContent content = (ForwardContent) iMsg.content;
            rMsg = content.forwardMessage;

            InstantMessage secret = verifyAndDecryptMessage(rMsg);
            if (secret != null) {
                return secret;
            }
            // FIXME: not for you?
        }

        // OK
        return iMsg;
    }

    //-------- InstantMessageDelegate

    @Override
    public byte[] encryptContent(Content content, Map<String, Object> password, InstantMessage iMsg) {
        SymmetricKey key = getSymmetricKey(password);
        if (key == null) {
            throw new NullPointerException("failed to get symmetric key: " + password);
        }

        // check attachment for File/Image/Audio/Video message content
        if (content instanceof FileContent) {
            // assert content.type in [FILE, IMAGE, AUDIO, VIDEO]
            FileContent file = (FileContent) content;
            byte[] data = file.getData();
            data = key.encrypt(data);
            // upload (encrypted) file data onto CDN and save the URL in message content
            String url = delegate.uploadFileData(data, iMsg);
            if (url != null) {
                file.setUrl(url);
                file.setData(null);
            }
        }

        // encrypt it with password
        String json = JSON.encode(content);
        byte[] data = json.getBytes(Charset.forName("UTF-8"));
        return key.encrypt(data);
    }

    @Override
    public Object encodeContentData(byte[] data, InstantMessage iMsg) {
        if (isBroadcast(iMsg)) {
            // broadcast message content will not be encrypted (just encoded to JsON),
            // so no need to encode to Base64 here
            return new String(data, Charset.forName("UTF-8"));
        }
        // encode to Base64
        return Base64.encode(data);
    }

    @Override
    public byte[] encryptKey(Map<String, Object> password, Object receiver, InstantMessage iMsg) {
        if (isBroadcast(iMsg)) {
            // broadcast message has no key
            return null;
        }
        // TODO: check whether support reused key

        // encrypt with receiver's public key
        Account contact = barrack.getAccount(barrack.getID(receiver));
        if (contact == null) {
            return null;
        }
        String json = JSON.encode(password);
        byte[] data = json.getBytes(Charset.forName("UTF-8"));
        return contact.encrypt(data);
    }

    @Override
    public Object encodeKeyData(byte[] key, InstantMessage iMsg) {
        assert !isBroadcast(iMsg) || key == null;
        // encode to Base64
        return key == null ? null : Base64.encode(key);
    }

    //-------- SecureMessageDelegate

    @Override
    public Map<String, Object> decryptKey(byte[] keyData, Object sender, Object receiver, SecureMessage sMsg) {
        assert !isBroadcast(sMsg) || keyData == null;

        ID from = barrack.getID(sender);
        ID to = barrack.getID(receiver);
        SymmetricKey key = null;
        if (keyData != null) {
            // decrypt key data with the receiver's private key
            ID identifier = barrack.getID(sMsg.envelope.receiver);
            User user = barrack.getUser(identifier);
            byte[] plaintext = user == null ? null : user.decrypt(keyData);
            if (plaintext == null || plaintext.length == 0) {
                throw new NullPointerException("failed to decrypt key in msg: " + sMsg);
            }
            // create symmetric key from JsON data
            String json = new String(plaintext, Charset.forName("UTF-8"));
            key = getSymmetricKey(JSON.decode(json), from, to);
        }
        if (key == null) {
            // if key data is empty, get it from key store
            key = getSymmetricKey(from, to);
            if (key == null) {
                throw new NullPointerException("failed to get password from " + sender + " to " + receiver);
            }
        }
        return key;
    }

    @Override
    public byte[] decodeKeyData(Object key, SecureMessage sMsg) {
        assert !isBroadcast(sMsg) || key == null;
        // decode from Base64
        return key == null ? null : Base64.decode((String) key);
    }

    @Override
    public Content decryptContent(byte[] data, Map<String, Object> password, SecureMessage sMsg) {
        SymmetricKey key = getSymmetricKey(password);
        if (key == null) {
            throw new NullPointerException("symmetric key error: " + password);
        }

        // decrypt message.data
        byte[] plaintext = key.decrypt(data);
        if (plaintext == null) {
            throw new NullPointerException("failed to decrypt data: " + password);
        }
        // build Content with JsON
        String json = new String(plaintext, Charset.forName("UTF-8"));
        Map<String, Object> dictionary = JSON.decode(json);
        Content content = Content.getInstance(dictionary);
        if (content == null) {
            throw new NullPointerException("decrypted content error: " + dictionary);
        }

        // check attachment for File/Image/Audio/Video message content
        if (content instanceof FileContent) {
            // assert content.type in [FILE, IMAGE, AUDIO, VIDEO]
            FileContent file = (FileContent) content;
            InstantMessage iMsg = new InstantMessage(content, sMsg.envelope);
            // download from CDN
            byte[] fileData = delegate.downloadFileData(file.getUrl(), iMsg);
            if (fileData == null) {
                // save symmetric key for decrypted file data after download from CDN
                file.setPassword(key);
            } else {
                // decrypt file data
                file.setData(key.decrypt(fileData));
                file.setUrl(null);
            }
        }

        return content;
    }

    @Override
    public byte[] decodeContentData(Object data, SecureMessage sMsg) {
        if (isBroadcast(sMsg)) {
            // broadcast message content will not be encrypted (just encoded to JsON),
            // so return the string data directly
            String json = (String) data;
            return json.getBytes(Charset.forName("UTF-8"));
        }
        // decode from Base64
        return Base64.decode((String) data);
    }

    @Override
    public byte[] signData(byte[] data, Object sender, SecureMessage sMsg) {
        User user = barrack.getUser(barrack.getID(sender));
        if (user == null) {
            throw new NullPointerException("failed to sign with sender: " + sender);
        }
        return user.sign(data);
    }

    @Override
    public Object encodeSignature(byte[] signature, SecureMessage sMsg) {
        return Base64.encode(signature);
    }

    //-------- ReliableMessageDelegate

    @Override
    public boolean verifyDataSignature(byte[] data, byte[] signature, Object sender, ReliableMessage rMsg) {
        Account account = barrack.getAccount(barrack.getID(sender));
        if (account == null) {
            throw new NullPointerException("failed to verify with sender: " + sender);
        }
        return account.verify(data, signature);
    }

    @Override
    public byte[] decodeSignature(Object signature, ReliableMessage rMsg) {
        return Base64.decode((String) signature);
    }
}
