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

import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;

import chat.dim.crypto.SymmetricKey;
import chat.dim.crypto.impl.SymmetricKeyImpl;
import chat.dim.dkd.*;
import chat.dim.format.JSON;
import chat.dim.mkm.*;
import chat.dim.protocol.file.FileContent;

public class Transceiver extends Protocol {

    public Transceiver() {
        super();
    }

    // delegates
    public TransceiverDelegate delegate;

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
                // sending group message one by one
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

    /**
     *  Pack instant message to reliable message for delivering
     *
     * @param iMsg - instant message
     * @return ReliableMessage Object
     * @throws NoSuchFieldException when encrypt message content
     */
    public ReliableMessage encryptAndSignMessage(InstantMessage iMsg) throws NoSuchFieldException {
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
        if (iMsg.delegate == null) {
            iMsg.delegate = this;
        }
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
     */
    public InstantMessage verifyAndDecryptMessage(ReliableMessage rMsg) {
        /*
        // [Meta Protocol] check meta in first contact message
        ID sender = barrack.getID(rMsg.envelope.sender);
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
        */
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
        /*
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
        */
        // OK
        return iMsg;
    }

    //-------- InstantMessageDelegate

    @Override
    public byte[] encryptContent(Content content, Map<String, Object> password, InstantMessage iMsg) {
        SymmetricKey key = getSymmetricKey(password);
        assert key == password;

        // check attachment for File/Image/Audio/Video message content
        if (content instanceof FileContent) {
            FileContent file = (FileContent) content;
            byte[] data = file.getData();
            // encrypt and upload file data onto CDN and save the URL in message content
            data = key.encrypt(data);
            String url = delegate.uploadFileData(data, iMsg);
            if (url != null) {
                // replace 'data' with 'URL'
                file.setUrl(url);
                file.setData(null);
            }
        }
        return super.encryptContent(content, key, iMsg);
    }

    //-------- SecureMessageDelegate

    @Override
    @SuppressWarnings("unchecked")
    public Content decryptContent(byte[] data, Map<String, Object> password, SecureMessage sMsg) {
        SymmetricKey key = getSymmetricKey(password);
        assert key == password;

        // decrypt content
        Content content = super.decryptContent(data, key, sMsg);

        // check attachment for File/Image/Audio/Video message content
        if (content instanceof FileContent) {
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
}
