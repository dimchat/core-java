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

import java.lang.ref.WeakReference;
import java.nio.charset.Charset;
import java.util.Map;

import chat.dim.crypto.SymmetricKey;
import chat.dim.crypto.impl.SymmetricKeyImpl;
import chat.dim.dkd.*;
import chat.dim.format.Base64;
import chat.dim.format.JSON;
import chat.dim.mkm.*;
import chat.dim.protocol.*;

public class Transceiver implements InstantMessageDelegate, SecureMessageDelegate, ReliableMessageDelegate {

    public Transceiver() {
        super();
    }

    // delegates
    private WeakReference<SocialNetworkDataSource> socialNetworkDataSourceRef = null;
    private WeakReference<CipherKeyDataSource> cipherKeyDataSourceRef = null;
    private WeakReference<TransceiverDelegate> delegateRef = null;

    public SocialNetworkDataSource getSocialNetworkDataSource() {
        if (socialNetworkDataSourceRef == null) {
            return null;
        }
        return socialNetworkDataSourceRef.get();
    }

    public void setSocialNetworkDataSource(SocialNetworkDataSource dataSource) {
        socialNetworkDataSourceRef = new WeakReference<>(dataSource);
    }

    public CipherKeyDataSource getCipherKeyDataSource() {
        if (cipherKeyDataSourceRef == null) {
            return null;
        }
        return cipherKeyDataSourceRef.get();
    }

    public void setCipherKeyDataSource(CipherKeyDataSource dataSource) {
        cipherKeyDataSourceRef = new WeakReference<>(dataSource);
    }

    public TransceiverDelegate getDelegate() {
        if (delegateRef == null) {
            return null;
        }
        return delegateRef.get();
    }

    public void setDelegate(TransceiverDelegate delegate) {
        delegateRef = new WeakReference<>(delegate);
    }

    //--------

    protected ID getID(Object string) {
        SocialNetworkDataSource dataSource = getSocialNetworkDataSource();
        return dataSource.getID(string);
    }

    protected Meta getMeta(ID identifier) {
        SocialNetworkDataSource dataSource = getSocialNetworkDataSource();
        return dataSource.getMeta(identifier);
    }

    protected User getUser(ID identifier) {
        SocialNetworkDataSource dataSource = getSocialNetworkDataSource();
        return dataSource.getUser(identifier);
    }

    protected Group getGroup(ID identifier) {
        SocialNetworkDataSource dataSource = getSocialNetworkDataSource();
        return dataSource.getGroup(identifier);
    }

    protected SymmetricKey getCipherKey(ID from, ID to) {
        CipherKeyDataSource dataSource = getCipherKeyDataSource();
        return dataSource.cipherKey(from, to);
    }

    protected SymmetricKey reuseCipherKey(ID from, ID to, SymmetricKey oldKey) {
        CipherKeyDataSource dataSource = getCipherKeyDataSource();
        return dataSource.reuseCipherKey(from, to, oldKey);
    }

    protected void cacheCipherKey(ID from, ID to, SymmetricKey key) {
        CipherKeyDataSource dataSource = getCipherKeyDataSource();
        dataSource.cacheCipherKey(from, to, key);
    }

    protected boolean sendPackage(byte[] data, CompletionHandler handler) {
        TransceiverDelegate delegate = getDelegate();
        return delegate.sendPackage(data, handler);
    }

    protected String uploadFileData(byte[] data, InstantMessage iMsg) {
        TransceiverDelegate delegate = getDelegate();
        return delegate.uploadFileData(data, iMsg);
    }

    protected byte[] downloadFileData(String url, InstantMessage iMsg) {
        TransceiverDelegate delegate = getDelegate();
        return delegate.downloadFileData(url, iMsg);
    }

    //--------

    private boolean isBroadcast(Message msg) {
        ID receiver = getID(msg.getGroup());
        if (receiver == null) {
            receiver = getID(msg.envelope.receiver);
        }
        return receiver.isBroadcast();
    }

    private SymmetricKey getSymmetricKey(Map<String, Object> password) {
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
                }
            } else {
                newKey = oldKey;
            }
        }
        // 4. update new key into the key store
        if (newKey != null && !newKey.equals(oldKey)) {
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
        if (group != null) {
            // group message
            password = getSymmetricKey(sender, group);
        } else {
            password = getSymmetricKey(sender, receiver);
        }

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
        assert content.equals(iMsg.content);

        String json = JSON.encode(content);
        return json.getBytes(Charset.forName("UTF-8"));
    }

    protected byte[] serializeKey(SymmetricKey password, InstantMessage iMsg) {
        if (isBroadcast(iMsg)) {
            // broadcast message has no key
            throw new RuntimeException("should not call this");
        }
        String json = JSON.encode(password);
        return json.getBytes(Charset.forName("UTF-8"));
    }

    @SuppressWarnings("unchecked")
    protected SymmetricKey deserializeKey(byte[] key, SecureMessage sMsg) {
        assert !isBroadcast(sMsg);

        String json = new String(key, Charset.forName("UTF-8"));
        Map<String, Object> dict = (Map<String, Object>) JSON.decode(json);
        // TODO: translate short keys
        //       'A' -> 'algorithm'
        //       'D' -> 'data'
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
        //       'S' -> 'sender'
        //       'R' -> 'receiver'
        //       'T' -> 'time'
        //       'D' -> 'data'
        //       'V' -> 'signature'
        //       'K' -> 'key'
        //       'M' -> 'meta'
        return Content.getInstance(dict);
    }

    //-------- InstantMessageDelegate

    @Override
    public byte[] encryptContent(Content content, Map<String, Object> password, InstantMessage iMsg) {
        SymmetricKey key = getSymmetricKey(password);
        assert key == password;
        assert key != null;

        // check attachment for File/Image/Audio/Video message content
        if (content instanceof FileContent) {
            FileContent file = (FileContent) content;
            byte[] data = file.getData();
            // encrypt and upload file data onto CDN and save the URL in message content
            data = key.encrypt(data);
            String url = uploadFileData(data, iMsg);
            if (url != null) {
                // replace 'data' with 'URL'
                file.setUrl(url);
                file.setData(null);
            }
        }

        // serialize and encrypt it with password
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
        assert !isBroadcast(iMsg);
        // broadcast message has no key
        return Base64.encode(key);
    }

    //-------- SecureMessageDelegate

    @Override
    public byte[] decodeKey(Object key, SecureMessage sMsg) {
        assert !isBroadcast(sMsg);
        // broadcast message has no key
        return Base64.decode((String) key);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> decryptKey(byte[] keyData, Object sender, Object receiver, SecureMessage sMsg) {
        assert !isBroadcast(sMsg) || keyData == null;
        // broadcast message has no key

        ID from = getID(sender);
        ID to = getID(receiver);
        SymmetricKey key = null;
        if (keyData != null) {
            // decrypt key data with the receiver/group member's private key
            ID identifier = getID(sMsg.envelope.receiver);
            LocalUser user = (LocalUser) getUser(identifier);
            assert user != null;
            byte[] plaintext = user.decrypt(keyData);
            if (plaintext == null || plaintext.length == 0) {
                throw new NullPointerException("failed to decrypt key in msg: " + sMsg);
            }
            // deserialize it to symmetric key
            key = deserializeKey(plaintext, sMsg);
            // cache the new key in key store
            cacheCipherKey(from, to, key);
        }
        if (key == null) {
            // if key data is empty, get it from key store
            key = getCipherKey(from, to);
            assert key != null;
        }
        return key;
    }

    @Override
    public byte[] decodeData(Object data, SecureMessage sMsg) {
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
    @SuppressWarnings("unchecked")
    public Content decryptContent(byte[] data, Map<String, Object> password, SecureMessage sMsg) {
        SymmetricKey key = getSymmetricKey(password);
        assert key == password;
        assert key != null;

        // decrypt message.data to content
        byte[] plaintext = key.decrypt(data);
        if (plaintext == null) {
            throw new NullPointerException("failed to decrypt data: " + password);
        }
        Content content = deserializeContent(plaintext, sMsg);

        // check attachment for File/Image/Audio/Video message content
        if (content instanceof FileContent) {
            FileContent file = (FileContent) content;
            InstantMessage iMsg = new InstantMessage(content, sMsg.envelope);
            // download from CDN
            byte[] fileData = downloadFileData(file.getUrl(), iMsg);
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
    public byte[] signData(byte[] data, Object sender, SecureMessage sMsg) {
        LocalUser user = (LocalUser) getUser(getID(sender));
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
        Content.register(ContentType.TEXT.value, TextContent.class);

        // File
        Content.register(ContentType.FILE.value, FileContent.class);
        // - Image
        Content.register(ContentType.IMAGE.value, ImageContent.class);
        // - Audio
        Content.register(ContentType.AUDIO.value, AudioContent.class);
        // - Video
        Content.register(ContentType.VIDEO.value, VideoContent.class);

        // Page
        Content.register(ContentType.PAGE.value, PageContent.class);

        // Quote

        // Command
        Content.register(ContentType.COMMAND.value, Command.class);
        // - HandshakeCommand
        // - MetaCommand
        //   - ProfileCommand
        // - ReceiptCommand

        // History
        Content.register(ContentType.HISTORY.value, HistoryCommand.class);
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
