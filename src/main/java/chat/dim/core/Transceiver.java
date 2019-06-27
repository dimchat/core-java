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
import chat.dim.mkm.Group;
import chat.dim.mkm.User;
import chat.dim.mkm.entity.EntityDataSource;
import chat.dim.mkm.entity.ID;
import chat.dim.mkm.entity.Meta;
import chat.dim.protocol.ContentType;
import chat.dim.protocol.ForwardContent;
import chat.dim.protocol.file.FileContent;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Transceiver implements InstantMessageDelegate, SecureMessageDelegate, ReliableMessageDelegate {

    public Transceiver() {
        super();
    }

    // delegates
    public TransceiverDelegate delegate;

    public BarrackDelegate barrackDelegate;
    public EntityDataSource entityDataSource;
    public CipherKeyDataSource cipherKeyDataSource;

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
            throws NoSuchFieldException, ClassNotFoundException {
        // transforming
        ID receiver = ID.getInstance(iMsg.envelope.receiver);
        ID groupID = ID.getInstance(iMsg.content.getGroup());
        ReliableMessage rMsg = encryptAndSignMessage(iMsg);
        if (rMsg == null) {
            // TODO: set iMsg.state = error
            throw new NullPointerException("failed to encrypt and sign message: " + iMsg);
        }

        // trying to send out
        boolean OK = true;
        if (split && receiver.getType().isGroup()) {
            Group group = barrackDelegate.getGroup(groupID);
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

    private SymmetricKey getPassword(ID from, ID to) throws ClassNotFoundException {
        // 1. get old key from store
        SymmetricKey reuseKey = cipherKeyDataSource.cipherKey(from, to);
        // 2. get new key from delegate
        SymmetricKey newKey = cipherKeyDataSource.reuseCipherKey(from, to, reuseKey);
        if (newKey == null) {
            if (reuseKey == null) {
                // 3. create a new key
                newKey = SymmetricKeyImpl.generate(SymmetricKey.AES);
            } else {
                newKey = reuseKey;
            }
        }
        // 4. update new key into the key store
        assert newKey != null;
        if (!newKey.equals(reuseKey)) {
            cipherKeyDataSource.cacheCipherKey(from, to, newKey);
        }
        return newKey;
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
        // 1. encrypt 'content' to 'data' for receiver
        ID sender = ID.getInstance(iMsg.envelope.sender);
        ID receiver = ID.getInstance(iMsg.envelope.receiver);
        ID groupID = ID.getInstance(iMsg.content.getGroup());
        // if 'group' exists and the 'receiver' is a group ID,
        // they must be equal
        assert groupID == null || receiver.getType().isCommunicator() || receiver.equals(groupID);
        if (groupID == null) {
            assert receiver != null;
            if (receiver.getType().isGroup()) {
                groupID = receiver;
            }
        }

        // 1. encrypt 'content' to 'data' for receiver
        SecureMessage sMsg;
        if (groupID == null) {
            // personal message
            SymmetricKey password = getPassword(sender, receiver);
            sMsg = iMsg.encrypt(password);
        } else {
            // group message
            List<ID> members;
            if (receiver.getType().isCommunicator()) {
                // split group message
                members = new ArrayList<>();
                members.add(receiver);
            } else {
                Group group = barrackDelegate.getGroup(groupID);
                members = group.getMembers();
                assert members != null;
            }
            SymmetricKey password = getPassword(sender, groupID);
            sMsg = iMsg.encrypt(password, members);
        }

        // 2. sign 'data' by sender
        sMsg.delegate = this;
        return sMsg.sign();
    }

    /**
     *  Extract instant message from a reliable message received
     *
     * @param rMsg - reliable message
     * @param users - my accounts
     * @return InstantMessage object
     * @throws ClassNotFoundException when saving meta
     */
    public InstantMessage verifyAndDecryptMessage(ReliableMessage rMsg, List<User> users)
            throws ClassNotFoundException {
        ID sender = ID.getInstance(rMsg.envelope.sender);
        ID receiver = ID.getInstance(rMsg.envelope.receiver);

        // [Meta Protocol] check meta in first contact message
        Meta meta = entityDataSource.getMeta(sender);
        if (meta == null) {
            // first contact, try meta in message package
            meta = Meta.getInstance(rMsg.getMeta());
            assert meta.matches(sender);
            entityDataSource.saveMeta(meta, sender);
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
        if (groupID == null) {
            // personal message
            sMsg.delegate = this;
            iMsg = sMsg.decrypt();
        } else {
            // group message
            sMsg = sMsg.trim(user.identifier.toString());
            sMsg.delegate = this;
            iMsg = sMsg.decrypt(receiver.toString());
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
        SymmetricKey key = null;
        try {
            key = SymmetricKeyImpl.getInstance(password);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        if (key == null) {
            throw new NullPointerException("symmetric key error: " + password);
        }
        // check attachment for File/Image/Audio/Video message content
        int type = content.type;
        if (type == ContentType.FILE.value ||
                type == ContentType.IMAGE.value ||
                type == ContentType.AUDIO.value ||
                type == ContentType.VIDEO.value) {
            // upload file data onto CDN and save the URL in message content
            FileContent file = (FileContent) content;
            byte[] data = key.encrypt(file.getData());
            assert data != null;
            String url = delegate.uploadFileData(data, iMsg);
            if (url != null) {
                file.setUrl(url);
                file.setData(null);
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
        ID identifier = ID.getInstance(receiver);
        Account contact = barrackDelegate.getAccount(identifier);
        return contact == null ? null : contact.encrypt(data);
    }

    //-------- SecureMessageDelegate

    @Override
    public Map<String, Object> decryptKey(byte[] keyData, Object sender, Object receiver, SecureMessage sMsg) {
        ID from = ID.getInstance(sender);
        ID to = ID.getInstance(receiver);
        SymmetricKey key = null;
        if (keyData != null) {
            // decrypt key data with the receiver's private key
            ID identifier = ID.getInstance(sMsg.envelope.receiver);
            User user = barrackDelegate.getUser(identifier);
            byte[] plaintext = user == null ? null : user.decrypt(keyData);
            if (plaintext == null || plaintext.length == 0) {
                throw new NullPointerException("failed to decrypt key in msg: " + sMsg);
            }
            // create symmetric key from JsON data
            String json = new String(plaintext, Charset.forName("UTF-8"));
            try {
                key = SymmetricKeyImpl.getInstance(JSON.decode(json));
                // set the new key in key store
                cipherKeyDataSource.cacheCipherKey(from, to, key);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
        if (key == null) {
            // if key data is empty, get it from key store
            key = cipherKeyDataSource.cipherKey(from, to);
            if (key == null) {
                throw new NullPointerException("failed to get password from " + sender + " to " + receiver);
            }
        }
        return key;
    }

    @Override
    public Content decryptContent(byte[] data, Map<String, Object> password, SecureMessage sMsg) {
        SymmetricKey key = null;
        try {
            key = SymmetricKeyImpl.getInstance(password);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
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
        int type = content.type;
        if (type == ContentType.FILE.value ||
                type == ContentType.IMAGE.value ||
                type == ContentType.AUDIO.value ||
                type == ContentType.VIDEO.value) {
            InstantMessage iMsg = new InstantMessage(content, sMsg.envelope);
            // download from CDN
            FileContent file = (FileContent) content;
            byte[] fileData = delegate.downloadFileData(file.getUrl(), iMsg);
            if (fileData != null) {
                // decrypt file data
                file.setData(key.decrypt(fileData));
                file.setUrl(null);
            } else {
                // save symmetric key for decrypted file data after download from CDN
                file.setPassword(key);
            }
        }

        return content;
    }

    @Override
    public byte[] signData(byte[] data, Object sender, SecureMessage sMsg) {
        User user = barrackDelegate.getUser(ID.getInstance(sender));
        if (user == null) {
            throw new NullPointerException("failed to sign with sender: " + sender);
        }
        return user.sign(data);
    }

    //-------- ReliableMessageDelegate

    @Override
    public boolean verifyData(byte[] data, byte[] signature, Object sender, ReliableMessage rMsg) {
        Account account = barrackDelegate.getAccount(ID.getInstance(sender));
        if (account == null) {
            throw new NullPointerException("failed to verify with sender: " + sender);
        }
        return account.verify(data, signature);
    }
}
