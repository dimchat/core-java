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

class Protocol implements InstantMessageDelegate, SecureMessageDelegate, ReliableMessageDelegate {

    Protocol() {
        super();
    }

    // delegates
    public SocialNetworkDataSource barrack;
    public CipherKeyDataSource keyCache;

    private boolean isBroadcast(Message msg) {
        ID receiver = barrack.getID(msg.getGroup());
        if (receiver == null) {
            receiver = barrack.getID(msg.envelope.receiver);
        }
        return receiver.isBroadcast();
    }

    SymmetricKey getSymmetricKey(Map<String, Object> password) {
        try {
            return SymmetricKeyImpl.getInstance(password);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    protected byte[] serializeContent(Content content, InstantMessage iMsg) {
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
        // encrypt it with password
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
        User contact = barrack.getUser(barrack.getID(receiver));
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

        ID from = barrack.getID(sender);
        ID to = barrack.getID(receiver);
        SymmetricKey key = null;
        if (keyData != null) {
            // decrypt key data with the receiver/group member's private key
            ID identifier = barrack.getID(sMsg.envelope.receiver);
            LocalUser user = (LocalUser) barrack.getUser(identifier);
            assert user != null;
            byte[] plaintext = user.decrypt(keyData);
            if (plaintext == null || plaintext.length == 0) {
                throw new NullPointerException("failed to decrypt key in msg: " + sMsg);
            }
            // deserialize it to symmetric key
            key = deserializeKey(plaintext, sMsg);
            // cache the new key in key store
            keyCache.cacheCipherKey(from, to, key);
        }
        if (key == null) {
            // if key data is empty, get it from key store
            key = keyCache.cipherKey(from, to);
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
        // decrypt message.data
        byte[] plaintext = key.decrypt(data);
        if (plaintext == null) {
            throw new NullPointerException("failed to decrypt data: " + password);
        }
        return deserializeContent(plaintext, sMsg);
    }

    @Override
    public byte[] signData(byte[] data, Object sender, SecureMessage sMsg) {
        LocalUser user = (LocalUser) barrack.getUser(barrack.getID(sender));
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
        User contact = barrack.getUser(barrack.getID(sender));
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
