/* license: https://mit-license.org
 *
 *  DIMP : Decentralized Instant Messaging Protocol
 *
 *                                Written in 2021 by Moky <albert.moky@gmail.com>
 *
 * ==============================================================================
 * The MIT License (MIT)
 *
 * Copyright (c) 2021 Albert Moky
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
package chat.dim;

import java.util.Arrays;

import chat.dim.crypto.SymmetricKey;
import chat.dim.dkd.InstantMessageDelegate;
import chat.dim.dkd.ReliableMessageDelegate;
import chat.dim.dkd.SecureMessageDelegate;
import chat.dim.format.JSON;
import chat.dim.format.UTF8;
import chat.dim.mkm.Entity;
import chat.dim.mkm.User;
import chat.dim.msg.BaseMessage;
import chat.dim.protocol.Content;
import chat.dim.protocol.ID;
import chat.dim.protocol.InstantMessage;
import chat.dim.protocol.ReliableMessage;
import chat.dim.protocol.SecureMessage;

/**
 *  Message Transceiver
 *  ~~~~~~~~~~~~~~~~~~~
 *
 *  Converting message format between PlainMessage and NetworkMessage
 */
public abstract class Transceiver implements InstantMessageDelegate, SecureMessageDelegate, ReliableMessageDelegate {

    // barrack
    protected abstract Entity.Delegate getEntityDelegate();

    //-------- InstantMessageDelegate

    @Override
    public byte[] serializeContent(Content content, SymmetricKey password, InstantMessage iMsg) {
        // NOTICE: check attachment for File/Image/Audio/Video message content
        //         before serialize content, this job should be do in subclass
        return UTF8.encode(JSON.encode(content));
    }

    @Override
    public byte[] encryptContent(byte[] data, SymmetricKey password, InstantMessage iMsg) {
        return password.encrypt(data, iMsg);
    }

    @Override
    public byte[] serializeKey(SymmetricKey password, InstantMessage iMsg) {
        if (BaseMessage.isBroadcast(iMsg)) {
            // broadcast message has no key
            return null;
        }
        return UTF8.encode(JSON.encode(password));
    }

    @Override
    public byte[] encryptKey(byte[] data, ID receiver, InstantMessage iMsg) {
        assert !BaseMessage.isBroadcast(iMsg) : "broadcast message has no key: " + iMsg;
        Entity.Delegate barrack = getEntityDelegate();
        assert barrack != null : "entity delegate not set yet";
        // TODO: make sure the receiver's public key exists
        User contact = barrack.getUser(receiver);
        assert contact != null : "failed to encrypt for receiver: " + receiver;
        // encrypt with receiver's public key
        return contact.encrypt(data);
    }

    //-------- SecureMessageDelegate

    @Override
    public byte[] decryptKey(byte[] key, ID sender, ID receiver, SecureMessage sMsg) {
        // NOTICE: the receiver will be group ID in a group message here
        assert !BaseMessage.isBroadcast(sMsg) : "broadcast message has no key: " + sMsg;
        Entity.Delegate barrack = getEntityDelegate();
        assert barrack != null : "entity delegate not set yet";
        // decrypt key data with the receiver/group member's private key
        ID identifier = sMsg.getReceiver();
        User user = barrack.getUser(identifier);
        assert user != null : "failed to create local user: " + identifier;
        return user.decrypt(key);
    }

    @Override
    public SymmetricKey deserializeKey(byte[] key, ID sender, ID receiver, SecureMessage sMsg) {
        // NOTICE: the receiver will be group ID in a group message here
        assert !BaseMessage.isBroadcast(sMsg) : "broadcast message has no key: " + sMsg;
        assert key != null : "reused key? get it from local cache: " + sender + " -> " + receiver;
        String json = UTF8.decode(key);
        assert json != null : "key data error: " + Arrays.toString(key);
        Object dict = JSON.decode(json);
        // TODO: translate short keys
        //       'A' -> 'algorithm'
        //       'D' -> 'data'
        //       'V' -> 'iv'
        //       'M' -> 'mode'
        //       'P' -> 'padding'
        return SymmetricKey.parse(dict);
    }

    @Override
    public byte[] decryptContent(byte[] data, SymmetricKey password, SecureMessage sMsg) {
        return password.decrypt(data, sMsg);
    }

    @Override
    public Content deserializeContent(byte[] data, SymmetricKey password, SecureMessage sMsg) {
        //assert sMsg.getData() != null : "message data empty";
        String json = UTF8.decode(data);
        assert json != null : "content data error: " + Arrays.toString(data);
        Object dict = JSON.decode(json);
        // TODO: translate short keys
        //       'T' -> 'type'
        //       'N' -> 'sn'
        //       'W' -> 'time'
        //       'G' -> 'group'
        return Content.parse(dict);
    }

    @Override
    public byte[] signData(byte[] data, ID sender, SecureMessage sMsg) {
        Entity.Delegate barrack = getEntityDelegate();
        assert barrack != null : "entity delegate not set yet";
        User user = barrack.getUser(sender);
        assert user != null : "failed to sign with sender: " + sender;
        return user.sign(data);
    }

    //-------- ReliableMessageDelegate

    @Override
    public boolean verifyDataSignature(byte[] data, byte[] signature, ID sender, ReliableMessage rMsg) {
        Entity.Delegate barrack = getEntityDelegate();
        assert barrack != null : "entity delegate not set yet";
        User contact = barrack.getUser(sender);
        assert contact != null : "failed to verify signature for sender: " + sender;
        return contact.verify(data, signature);
    }
}
