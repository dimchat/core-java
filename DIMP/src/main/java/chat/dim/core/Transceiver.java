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
import java.util.List;
import java.util.Map;

import chat.dim.CipherKeyDelegate;
import chat.dim.Entity;
import chat.dim.Group;
import chat.dim.Packer;
import chat.dim.Processor;
import chat.dim.User;
import chat.dim.crypto.SymmetricKey;
import chat.dim.format.Base64;
import chat.dim.format.JSON;
import chat.dim.format.UTF8;
import chat.dim.protocol.Content;
import chat.dim.protocol.ID;
import chat.dim.protocol.InstantMessage;
import chat.dim.protocol.Message;
import chat.dim.protocol.ReliableMessage;
import chat.dim.protocol.SecureMessage;

/**
 *  Core Transceiver
 *  ~~~~~~~~~~~~~~~~
 */
public class Transceiver implements chat.dim.Transceiver, InstantMessage.Delegate, ReliableMessage.Delegate {

    private WeakReference<Entity.Delegate> entityDelegateRef = null;
    private WeakReference<CipherKeyDelegate> keyDelegateRef = null;

    private WeakReference<Packer> packerRef = null;
    private WeakReference<Processor> processorRef = null;

    public Transceiver() {
        super();
    }

    /**
     *  Delegate for User/Group
     *
     * @param barrack - entity delegate
     */
    public void setEntityDelegate(Entity.Delegate barrack) {
        entityDelegateRef = new WeakReference<>(barrack);
    }
    protected Entity.Delegate getEntityDelegate() {
        return entityDelegateRef == null ? null : entityDelegateRef.get();
    }

    /**
     *  Delegate for Cipher Key
     *
     * @param keyCache - key store
     */
    public void setCipherKeyDelegate(CipherKeyDelegate keyCache) {
        keyDelegateRef = new WeakReference<>(keyCache);
    }
    protected CipherKeyDelegate getCipherKeyDelegate() {
        return keyDelegateRef == null ? null : keyDelegateRef.get();
    }

    /**
     *  Delegate for Packing Message
     *
     * @param packer - message packer
     */
    public void setPacker(Packer packer) {
        packerRef = new WeakReference<>(packer);
    }
    protected Packer getPacker() {
        return packerRef == null ? null : packerRef.get();
    }

    /**
     *  Delegate for Processing Message
     *
     * @param processor - message processor
     */
    public void setProcessor(Processor processor) {
        processorRef = new WeakReference<>(processor);
    }
    protected Processor getProcessor() {
        return processorRef == null ? null : processorRef.get();
    }

    //
    //  Interfaces for User/Group
    //
    @Override
    public User selectLocalUser(final ID receiver) {
        return getEntityDelegate().selectLocalUser(receiver);
    }

    @Override
    public User getUser(final ID identifier) {
        return getEntityDelegate().getUser(identifier);
    }

    @Override
    public Group getGroup(final ID identifier) {
        return getEntityDelegate().getGroup(identifier);
    }

    //
    //  Interfaces for Cipher Key
    //
    @Override
    public SymmetricKey getCipherKey(final ID sender, final ID receiver, final boolean generate) {
        return getCipherKeyDelegate().getCipherKey(sender, receiver, generate);
    }

    @Override
    public void cacheCipherKey(final ID sender, final ID receiver, final SymmetricKey key) {
        getCipherKeyDelegate().cacheCipherKey(sender, receiver, key);
    }

    //
    //  Interfaces for Packing Message
    //
    @Override
    public ID getOvertGroup(final Content content) {
        return getPacker().getOvertGroup(content);
    }

    @Override
    public SecureMessage encryptMessage(final InstantMessage iMsg) {
        return getPacker().encryptMessage(iMsg);
    }

    @Override
    public ReliableMessage signMessage(final SecureMessage sMsg) {
        return getPacker().signMessage(sMsg);
    }

    @Override
    public byte[] serializeMessage(final ReliableMessage rMsg) {
        return getPacker().serializeMessage(rMsg);
    }

    @Override
    public ReliableMessage deserializeMessage(final byte[] data) {
        return getPacker().deserializeMessage(data);
    }

    @Override
    public SecureMessage verifyMessage(final ReliableMessage rMsg) {
        return getPacker().verifyMessage(rMsg);
    }

    @Override
    public InstantMessage decryptMessage(final SecureMessage sMsg) {
        return getPacker().decryptMessage(sMsg);
    }

    //
    //  Interfaces for Processing Message
    //
    @Override
    public List<byte[]> process(final byte[] data) {
        return getProcessor().process(data);
    }

    @Override
    public List<ReliableMessage> process(final ReliableMessage rMsg) {
        return getProcessor().process(rMsg);
    }

    @Override
    public List<SecureMessage> process(final SecureMessage sMsg, final ReliableMessage rMsg) {
        return getProcessor().process(sMsg, rMsg);
    }

    @Override
    public List<InstantMessage> process(final InstantMessage iMsg, final ReliableMessage rMsg) {
        return getProcessor().process(iMsg, rMsg);
    }

    @Override
    public List<Content> process(final Content content, final ReliableMessage rMsg) {
        return getProcessor().process(content, rMsg);
    }

    private boolean isBroadcast(final Message msg) {
        ID receiver = msg.getGroup();
        if (receiver == null) {
            receiver = msg.getReceiver();
        }
        return receiver.isBroadcast();
    }

    //-------- InstantMessageDelegate

    @Override
    public byte[] serializeContent(final Content content, final SymmetricKey password, final InstantMessage iMsg) {
        // NOTICE: check attachment for File/Image/Audio/Video message content
        //         before serialize content, this job should be do in subclass
        return JSON.encode(content);
    }

    @Override
    public byte[] encryptContent(final byte[] data, final SymmetricKey password, final InstantMessage iMsg) {
        return password.encrypt(data);
    }

    @Override
    public Object encodeData(final byte[] data, final InstantMessage iMsg) {
        if (isBroadcast(iMsg)) {
            // broadcast message content will not be encrypted (just encoded to JsON),
            // so no need to encode to Base64 here
            return UTF8.decode(data);
        }
        return Base64.encode(data);
    }

    @Override
    public byte[] serializeKey(final SymmetricKey password, final InstantMessage iMsg) {
        if (isBroadcast(iMsg)) {
            // broadcast message has no key
            return null;
        }
        return JSON.encode(password);
    }

    @Override
    public byte[] encryptKey(final byte[] data, final ID receiver, final InstantMessage iMsg) {
        assert !isBroadcast(iMsg) : "broadcast message has no key: " + iMsg;
        // NOTICE: make sure the receiver's public key exists
        return getUser(receiver).encrypt(data);
    }

    @Override
    public Object encodeKey(final byte[] key, final InstantMessage iMsg) {
        assert !isBroadcast(iMsg) : "broadcast message has no key: " + iMsg;
        return Base64.encode(key);
    }

    //-------- SecureMessageDelegate

    @Override
    public byte[] decodeKey(final Object key, final SecureMessage sMsg) {
        assert !isBroadcast(sMsg) : "broadcast message has no key: " + sMsg;
        return Base64.decode((String) key);
    }

    @Override
    public byte[] decryptKey(final byte[] key, final ID sender, final ID receiver, final SecureMessage sMsg) {
        // NOTICE: the receiver will be group ID in a group message here
        assert !isBroadcast(sMsg) : "broadcast message has no key: " + sMsg;
        // decrypt key data with the receiver/group member's private key
        final ID identifier = sMsg.getReceiver();
        return getUser(identifier).decrypt(key);
    }

    @SuppressWarnings("unchecked")
    @Override
    public SymmetricKey deserializeKey(final byte[] key, final ID sender, final ID receiver, final SecureMessage sMsg) {
        // NOTICE: the receiver will be group ID in a group message here
        if (key == null) {
            // get key from cache
            return getCipherKey(sender, receiver, false);
        } else {
            assert !isBroadcast(sMsg) : "broadcast message has no key: " + sMsg;
            Map<String, Object> dict = (Map<String, Object>) JSON.decode(key);
            // TODO: translate short keys
            //       'A' -> 'algorithm'
            //       'D' -> 'data'
            //       'V' -> 'iv'
            //       'M' -> 'mode'
            //       'P' -> 'padding'
            return SymmetricKey.parse(dict);
        }
    }

    @Override
    public byte[] decodeData(final Object data, final SecureMessage sMsg) {
        if (isBroadcast(sMsg)) {
            // broadcast message content will not be encrypted (just encoded to JsON),
            // so return the string data directly
            return UTF8.encode((String) data);
        }
        return Base64.decode((String) data);
    }

    @Override
    public byte[] decryptContent(final byte[] data, final SymmetricKey password, final SecureMessage sMsg) {
        return password.decrypt(data);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Content deserializeContent(final byte[] data, final SymmetricKey password, final SecureMessage sMsg) {
        assert sMsg.getData() != null;
        final Map<String, Object> dict = (Map<String, Object>) JSON.decode(data);
        // TODO: translate short keys
        //       'T' -> 'type'
        //       'N' -> 'sn'
        //       'G' -> 'group'
        final Content content = Content.parse(dict);
        assert content != null : "content error: " + data.length;

        if (!isBroadcast(sMsg)) {
            // check and cache key for reuse
            final ID sender = sMsg.getSender();
            final ID group = getOvertGroup(content);
            if (group == null) {
                final ID receiver = sMsg.getReceiver();
                // personal message or (group) command
                // cache key with direction (sender -> receiver)
                cacheCipherKey(sender, receiver, password);
            } else {
                // group message (excludes group command)
                // cache the key with direction (sender -> group)
                cacheCipherKey(sender, group, password);
            }
        }

        // NOTICE: check attachment for File/Image/Audio/Video message content
        //         after deserialize content, this job should be do in subclass
        return content;
    }

    @Override
    public byte[] signData(final byte[] data, final ID sender, final SecureMessage sMsg) {
        return getUser(sender).sign(data);
    }

    @Override
    public Object encodeSignature(final byte[] signature, final SecureMessage sMsg) {
        return Base64.encode(signature);
    }

    //-------- ReliableMessageDelegate

    @Override
    public byte[] decodeSignature(final Object signature, final ReliableMessage rMsg) {
        return Base64.decode((String) signature);
    }

    @Override
    public boolean verifyDataSignature(final byte[] data, final byte[] signature,
                                       final ID sender, final ReliableMessage rMsg) {
        return getUser(sender).verify(data, signature);
    }
}
