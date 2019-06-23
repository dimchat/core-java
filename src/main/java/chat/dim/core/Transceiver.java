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
import chat.dim.format.JSON;
import chat.dim.mkm.Account;
import chat.dim.mkm.User;
import chat.dim.mkm.entity.ID;
import chat.dim.mkm.entity.Meta;
import chat.dim.protocol.ContentType;
import chat.dim.protocol.ForwardContent;
import chat.dim.protocol.file.FileContent;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class Transceiver implements InstantMessageDelegate, SecureMessageDelegate, ReliableMessageDelegate {

    private static Transceiver ourInstance = new Transceiver();

    public static Transceiver getInstance() {
        return ourInstance;
    }

    private Transceiver() {
    }

    // delegate
    public TransceiverDelegate delegate;

    /**
     *  Send message (secured + certified) to target station
     *
     * @param iMsg - instant message
     * @param callback - callback function
     * @param split - if it's a group message, split it before sending out
     * @return NO on data/delegate error
     * @throws NoSuchFieldException when 'group' not found
     * @throws ClassNotFoundException when key algorithm not supported
     */
    public boolean sendMessage(InstantMessage iMsg, Callback callback, boolean split)
            throws NoSuchFieldException, ClassNotFoundException {
        // transforming
        ID receiver = ID.getInstance(iMsg.envelope.receiver);
        ID groupID = ID.getInstance(iMsg.content.getGroup());
        ReliableMessage rMsg = encryptAndSignMessage(iMsg);
        if (rMsg == null) {
            // TODO: set iMsg.state = error
            throw new NullPointerException("failed to encrypt and sign message: " + iMsg);
        }
        Barrack barrack = Barrack.getInstance();

        // trying to send out
        boolean OK = true;
        if (split && receiver.getType().isGroup()) {
            List<ID> members = barrack.getMembers(groupID);
            assert members != null;
            List<SecureMessage> messages = rMsg.split(members);
            for (SecureMessage msg: messages) {
                if (sendMessage((ReliableMessage) msg, callback)) {
                    // group message sent
                } else {
                    OK = false;
                }
            }
        } else {
            OK = sendMessage(rMsg, callback);
        }

        // sending status
        if (OK) {
            // TODO: set iMsg.state = sending
        } else {
            // TODO: set iMsg.state = waiting
        }
        return OK;
    }

    private boolean sendMessage(ReliableMessage rMsg, Callback callback) {
        String json = JSON.encode(rMsg);
        byte[] data = json.getBytes(Charset.forName("UTF-8"));
        return delegate.sendPackage(data, new CompletionHandler() {
            @Override
            public void onSuccess() {
                callback.onFinished(rMsg, null);
            }

            @Override
            public void onFailed(Error error) {
                callback.onFinished(rMsg, error);
            }
        });
    }

    /**
     *  Pack instant message to reliable message for delivering
     *
     * @param iMsg - instant message
     * @return ReliableMessage Object
     * @throws NoSuchFieldException when encrypt message content
     */
    public ReliableMessage encryptAndSignMessage(InstantMessage iMsg)
            throws NoSuchFieldException, ClassNotFoundException {
        if (iMsg.delegate == null) {
            iMsg.delegate = this;
        }

        ID receiver = ID.getInstance(iMsg.envelope.receiver);
        ID groupID = ID.getInstance(iMsg.content.getGroup());
        if (groupID != null) {
            // if 'group' exists and the 'receiver' is a group ID,
            // they must be equal
        } else {
            assert receiver != null;
            if (receiver.getType().isGroup()) {
                groupID = receiver;
            }
        }

        // 1. encrypt 'content' to 'data' for receiver
        SecureMessage sMsg;
        if (groupID != null) {
            // group message
            List<ID> members;
            if (receiver.getType().isCommunicator()) {
                // split group message
                members = new ArrayList<>();
                members.add(receiver);
            } else {
                Barrack barrack = Barrack.getInstance();
                members = barrack.getMembers(groupID);
            }
            assert members != null;
            sMsg = iMsg.encrypt(getKey(groupID), members);
        } else {
            // personal message
            sMsg = iMsg.encrypt(getKey(receiver));
        }

        // 2. sign 'data' by sender
        sMsg.delegate = this;
        return sMsg.sign();
    }

    private SymmetricKey getKey(ID receiver) {
        KeyStore store = KeyStore.getInstance();
        User user = store.currentUser;
        if (user == null) {
            throw new NullPointerException("current user not set to key store");
        }
        ID sender = user.identifier;
        SymmetricKey reusedKey, newKey;

        // 1. get old key from store
        reusedKey = store.getKey(sender, receiver);

        // 2. get new key from delegate
        newKey = delegate.reuseCipherKey(sender, receiver, reusedKey);
        if (newKey == null) {
            newKey = reusedKey;
        }
        if (newKey == null) {
            // 3. create a new key
            try {
                newKey = SymmetricKeyImpl.generate(SymmetricKey.AES);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
            if (newKey == null) {
                throw new NullPointerException("failed to get cipher key");
            }
        }

        // 4. save it into the Key Store
        if (newKey != reusedKey) {
            store.setKey(newKey, sender, receiver);
        }
        return newKey;
    }

    /**
     *  Extract instant message from a reliable message received
     *
     * @param rMsg - reliable message
     * @param users - my accounts
     * @return InstantMessage object
     * @throws IOException when saving meta
     * @throws ClassNotFoundException when creating meta
     */
    public InstantMessage verifyAndDecryptMessage(ReliableMessage rMsg, List<User> users)
            throws IOException, ClassNotFoundException, NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {
        ID sender = ID.getInstance(rMsg.envelope.sender);
        ID receiver = ID.getInstance(rMsg.envelope.receiver);

        Barrack barrack = Barrack.getInstance();
        // [Meta Protocol] check meta in first contact message
        Meta meta = barrack.getMeta(sender);
        if (meta == null) {
            // first contact, try meta in message package
            meta = Meta.getInstance(rMsg.getMeta());
            assert meta.matches(sender);
            barrack.saveMeta(meta, sender);
        }

        // 1. verify 'data' with 'signature'
        if (rMsg.delegate == null) {
            rMsg.delegate = this;
        }
        SecureMessage sMsg = rMsg.verify();

        // check recipient
        ID groupID = ID.getInstance(rMsg.getGroup());
        User user = null;
        if (receiver.getType().isGroup()) {
            groupID = receiver;
            // FIXME: maybe other user?
            user = users.get(0);
            receiver = user.identifier;
        } else {
            for (User item : users) {
                if (item.identifier.equals(receiver)) {
                    user = item;
                    // got new message for this user
                    break;
                }
            }
        }
        if (user == null) {
            throw new NullPointerException("wrong recipient: " + receiver);
        }

        // 2. decrypt 'data' to 'content'
        InstantMessage iMsg;
        if (groupID != null) {
            // group message
            sMsg = sMsg.trim(user.identifier.toString());
            sMsg.delegate = this;
            iMsg = sMsg.decrypt(receiver.toString());
        } else {
            // personal message
            sMsg.delegate = this;
            iMsg = sMsg.decrypt();
        }

        // 3. check: top-secret message
        if (iMsg.content.type == ContentType.FORWARD.value) {
            // do it again to drop the wrapper,
            // the secret inside the content is the real message
            ForwardContent content = (ForwardContent) iMsg.content;
            rMsg = content.forwardMessage;

            return verifyAndDecryptMessage(rMsg, users);
        }

        // OK
        return iMsg;
    }

    //-------- InstantMessageDelegate

    @Override
    public byte[] encryptContent(Content content, Map<String, Object> password, InstantMessage iMsg) {
        SymmetricKey key;
        try {
            key = SymmetricKeyImpl.getInstance(password);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }

        // check attachment for File/Image/Audio/Video message content
        int type = content.type;
        if (type == ContentType.FILE.value ||
                type == ContentType.IMAGE.value ||
                type == ContentType.AUDIO.value ||
                type == ContentType.VIDEO.value) {
            // upload file data onto CDN and save the URL in message content
            FileContent file = new FileContent(content);
            byte[] data = file.getData();
            if (data != null) {
                // encrypt it first
                data = key.encrypt(data);
                // upload encrypted data to CDN
                String url = delegate.uploadFileData(data, iMsg);
                if (url != null) {
                    file.setUrl(url);
                    file.setData(null);
                    content = file;
                }
            }
        }

        String json = JSON.encode(content);
        byte[] data = json.getBytes(Charset.forName("UTF-8"));
        return key.encrypt(data);
    }

    @Override
    public byte[] encryptKey(Map<String, Object> password, Object receiver, InstantMessage iMsg) {
        String json = JSON.encode(password);
        byte[] data = json.getBytes(Charset.forName("UTF-8"));
        Barrack barrack = Barrack.getInstance();
        ID identifier;
        try {
            identifier = ID.getInstance(receiver);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
        Account contact = barrack.getAccount(identifier);
        if (contact == null) {
            return null;
        }
        return contact.encrypt(data);
    }

    //-------- SecureMessageDelegate

    @Override
    public Map<String, Object> decryptKey(byte[] keyData, Object sender, Object receiver, SecureMessage sMsg) {
        Barrack barrack = Barrack.getInstance();
        KeyStore store = KeyStore.getInstance();
        ID from;
        ID to;
        ID userID;
        try {
            from = ID.getInstance(sender);
            to = ID.getInstance(receiver);
            userID = ID.getInstance(sMsg.envelope.receiver);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
        SymmetricKey key = null;
        if (keyData != null) {
            // decrypt key data with the receiver's private key
            User user = store.currentUser;
            if (user == null || !user.identifier.equals(userID)) {
                if (userID.getType().isCommunicator()) {
                    user = barrack.getUser(userID);
                }
                if (user == null) {
                    throw new IllegalArgumentException("receiver error: " + sMsg);
                }
            }
            // FIXME: check sMsg.envelope.receiver == user.identifier
            byte[] plaintext = user.decrypt(keyData);
            if (plaintext == null) {
                throw new NullPointerException("failed to decrypt key");
            }
            String json = new String(plaintext, Charset.forName("UTF-8"));
            try {
                // create symmetric key from JsON data
                key = SymmetricKeyImpl.getInstance(JSON.decode(json));
                // set the new key in key store
                store.setKey(key, from, to);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
        if (key == null) {
            // if key data is empty, get it from key store
            key = store.getKey(from, to);
        }
        return key;
    }

    @Override
    public Content decryptContent(byte[] data, Map<String, Object> password, SecureMessage sMsg) {
        SymmetricKey key;
        try {
            key = SymmetricKeyImpl.getInstance(password);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
        // decrypt message.data
        byte[] plaintext = key.decrypt(data);
        if (plaintext == null) {
            throw new NullPointerException("failed to decrypt data: " + password);
        }

        String json = new String(plaintext, Charset.forName("UTF-8"));
        Map<String, Object> dictionary = JSON.decode(json);
        Content content;
        try {
            content = Content.getInstance(dictionary);
        } catch (InvocationTargetException | NoSuchMethodException | InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
            throw new NullPointerException("decrypted content error: " + dictionary);
        }

        // check attachment for File/Image/Audio/Video message content
        int type = content.type;
        if (type == ContentType.FILE.value ||
                type == ContentType.IMAGE.value ||
                type == ContentType.AUDIO.value ||
                type == ContentType.VIDEO.value) {
            // download from CDN
            FileContent file = new FileContent(content);
            String url = file.getUrl();
            assert url != null;
            byte[] fileData = delegate.downloadFileData(url, new InstantMessage(content, sMsg.envelope));
            if (fileData != null) {
                // decrypt file data
                file.setData(key.decrypt(fileData));
            } else {
                // save symmetric key for decrypted file data after download from CDN
                file.setPassword(key);
            }
            content = file;
        }

        return content;
    }

    @Override
    public byte[] signData(byte[] data, Object sender, SecureMessage sMsg) {
        ID identifier = null;
        try {
            identifier = ID.getInstance(sender);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
        Barrack barrack = Barrack.getInstance();
        User user = barrack.getUser(identifier);
        if (user == null) {
            return null;
        }
        return user.sign(data);
    }

    //-------- ReliableMessageDelegate

    @Override
    public boolean verifyData(byte[] data, byte[] signature, Object sender, ReliableMessage rMsg) {
        ID identifier = null;
        try {
            identifier = ID.getInstance(sender);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return false;
        }
        Barrack barrack = Barrack.getInstance();
        Account account = barrack.getAccount(identifier);
        if (account == null) {
            return false;
        }
        return account.verify(data, signature);
    }
}
