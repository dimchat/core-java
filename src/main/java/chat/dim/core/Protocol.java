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
import java.util.Map;

import chat.dim.crypto.SymmetricKey;
import chat.dim.crypto.impl.SymmetricKeyImpl;
import chat.dim.dkd.*;
import chat.dim.format.Base64;
import chat.dim.format.JSON;
import chat.dim.mkm.ID;
import chat.dim.mkm.LocalUser;
import chat.dim.mkm.User;

import chat.dim.protocol.*;
import chat.dim.protocol.file.AudioContent;
import chat.dim.protocol.file.FileContent;
import chat.dim.protocol.file.ImageContent;
import chat.dim.protocol.file.VideoContent;

public class Protocol implements InstantMessageDelegate, SecureMessageDelegate, ReliableMessageDelegate {

    public Protocol() {
        super();
    }

    // delegates
    public SocialNetworkDataSource barrack;
    public CipherKeyDataSource keyCache;

    protected boolean isBroadcast(Message msg) {
        ID receiver = barrack.getID(msg.getGroup());
        if (receiver == null) {
            receiver = barrack.getID(msg.envelope.receiver);
        }
        return receiver.isBroadcast();
    }

    protected SymmetricKey getSymmetricKey(ID from, ID to) {
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

    protected SymmetricKey getSymmetricKey(Map<String, Object> password, ID from, ID to) {
        SymmetricKey key = getSymmetricKey(password);
        if (key != null) {
            // cache the new key in key store
            keyCache.cacheCipherKey(from, to, key);
        }
        return key;
    }

    protected SymmetricKey getSymmetricKey(Map<String, Object> password) {
        try {
            return SymmetricKeyImpl.getInstance(password);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    //-------- InstantMessageDelegate

    @Override
    public byte[] encryptContent(Content content, Map<String, Object> password, InstantMessage iMsg) {
        SymmetricKey key = getSymmetricKey(password);
        if (key == null) {
            throw new NullPointerException("failed to get symmetric key: " + password);
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
        User contact = barrack.getUser(barrack.getID(receiver));
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
    @SuppressWarnings("unchecked")
    public Map<String, Object> decryptKey(byte[] keyData, Object sender, Object receiver, SecureMessage sMsg) {
        assert !isBroadcast(sMsg) || keyData == null;

        ID from = barrack.getID(sender);
        ID to = barrack.getID(receiver);
        SymmetricKey key = null;
        if (keyData != null) {
            // decrypt key data with the receiver's private key
            ID identifier = barrack.getID(sMsg.envelope.receiver);
            LocalUser user = (LocalUser) barrack.getUser(identifier);
            byte[] plaintext = user == null ? null : user.decrypt(keyData);
            if (plaintext == null || plaintext.length == 0) {
                throw new NullPointerException("failed to decrypt key in msg: " + sMsg);
            }
            // create symmetric key from JsON data
            String json = new String(plaintext, Charset.forName("UTF-8"));
            Map<String, Object> dict = (Map<String, Object>) JSON.decode(json);
            key = getSymmetricKey(dict, from, to);
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
    @SuppressWarnings("unchecked")
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
        Map<String, Object> dict = (Map<String, Object>) JSON.decode(json);
        Content content = Content.getInstance(dict);
        if (content == null) {
            throw new NullPointerException("decrypted content error: " + dict);
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
        LocalUser user = (LocalUser) barrack.getUser(barrack.getID(sender));
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
        User contact = barrack.getUser(barrack.getID(sender));
        if (contact == null) {
            throw new NullPointerException("failed to verify with sender: " + sender);
        }
        return contact.verify(data, signature);
    }

    @Override
    public byte[] decodeSignature(Object signature, ReliableMessage rMsg) {
        return Base64.decode((String) signature);
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
