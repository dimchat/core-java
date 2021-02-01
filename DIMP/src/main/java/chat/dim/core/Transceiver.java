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
import java.util.Map;

import chat.dim.Group;
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

public class Transceiver implements InstantMessage.Delegate, ReliableMessage.Delegate {

    // delegates
    private WeakReference<EntityDelegate> entityDelegateRef = null;
    private WeakReference<CipherKeyDelegate> keyDelegateRef = null;

    private WeakReference<Packer> packerRef = null;
    private WeakReference<Processor> processorRef = null;

    public Transceiver() {
        super();
    }

    /**
     *  Delegate for getting entity
     *
     * @param barrack - entity delegate
     */
    public void setEntityDelegate(EntityDelegate barrack) {
        entityDelegateRef = new WeakReference<>(barrack);
    }
    protected EntityDelegate getEntityDelegate() {
        return entityDelegateRef == null ? null : entityDelegateRef.get();
    }

    /**
     *  Delegate for getting message key
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
     *  Delegate for packing message
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
     *  Delegate for processing message
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
    public User selectLocalUser(ID receiver) {
        return getEntityDelegate().selectLocalUser(receiver);
    }

    public User getUser(ID identifier) {
        return getEntityDelegate().getUser(identifier);
    }

    public Group getGroup(ID identifier) {
        return getEntityDelegate().getGroup(identifier);
    }

    //
    //  Interfaces for Cipher Key
    //
    public SymmetricKey getCipherKey(ID sender, ID receiver, boolean generate) {
        return getCipherKeyDelegate().getCipherKey(sender, receiver, generate);
    }

    public void cacheCipherKey(ID sender, ID receiver, SymmetricKey key) {
        getCipherKeyDelegate().cacheCipherKey(sender, receiver, key);
    }

    //
    //  Interfaces for Packing Message
    //
    public ID getOvertGroup(Content content) {
        return getPacker().getOvertGroup(content);
    }

    public SecureMessage encryptMessage(InstantMessage iMsg) {
        return getPacker().encryptMessage(iMsg);
    }

    public ReliableMessage signMessage(SecureMessage sMsg) {
        return getPacker().signMessage(sMsg);
    }

    public byte[] serializeMessage(ReliableMessage rMsg) {
        return getPacker().serializeMessage(rMsg);
    }

    public ReliableMessage deserializeMessage(byte[] data) {
        return getPacker().deserializeMessage(data);
    }

    public SecureMessage verifyMessage(ReliableMessage rMsg) {
        return getPacker().verifyMessage(rMsg);
    }

    public InstantMessage decryptMessage(SecureMessage sMsg) {
        return getPacker().decryptMessage(sMsg);
    }

    //
    //  Interfaces for Processing Message
    //
    public byte[] process(byte[] data) {
        return getProcessor().process(data);
    }

    public ReliableMessage process(ReliableMessage rMsg) {
        return getProcessor().process(rMsg);
    }

    public SecureMessage process(SecureMessage sMsg, ReliableMessage rMsg) {
        return getProcessor().process(sMsg, rMsg);
    }

    public InstantMessage process(InstantMessage iMsg, ReliableMessage rMsg) {
        return getProcessor().process(iMsg, rMsg);
    }

    public Content process(Content content, ReliableMessage rMsg) {
        return getProcessor().process(content, rMsg);
    }

    private boolean isBroadcast(Message msg) {
        ID receiver = msg.getGroup();
        if (receiver == null) {
            receiver = msg.getReceiver();
        }
        return receiver.isBroadcast();
    }

    //-------- InstantMessageDelegate

    @Override
    public byte[] serializeContent(Content content, SymmetricKey password, InstantMessage iMsg) {
        // NOTICE: check attachment for File/Image/Audio/Video message content
        //         before serialize content, this job should be do in subclass
        return JSON.encode(content);
    }

    @Override
    public byte[] encryptContent(byte[] data, SymmetricKey password, InstantMessage iMsg) {
        return password.encrypt(data);
    }

    @Override
    public Object encodeData(byte[] data, InstantMessage iMsg) {
        if (isBroadcast(iMsg)) {
            // broadcast message content will not be encrypted (just encoded to JsON),
            // so no need to encode to Base64 here
            return UTF8.decode(data);
        }
        return Base64.encode(data);
    }

    @Override
    public byte[] serializeKey(SymmetricKey password, InstantMessage iMsg) {
        if (isBroadcast(iMsg)) {
            // broadcast message has no key
            return null;
        }
        return JSON.encode(password);
    }

    @Override
    public byte[] encryptKey(byte[] data, ID receiver, InstantMessage iMsg) {
        assert !isBroadcast(iMsg) : "broadcast message has no key: " + iMsg;
        // NOTICE: make sure the receiver's public key exists
        return getUser(receiver).encrypt(data);
    }

    @Override
    public Object encodeKey(byte[] key, InstantMessage iMsg) {
        assert !isBroadcast(iMsg) : "broadcast message has no key: " + iMsg;
        return Base64.encode(key);
    }

    //-------- SecureMessageDelegate

    @Override
    public byte[] decodeKey(Object key, SecureMessage sMsg) {
        assert !isBroadcast(sMsg) : "broadcast message has no key: " + sMsg;
        return Base64.decode((String) key);
    }

    @Override
    public byte[] decryptKey(byte[] key, ID sender, ID receiver, SecureMessage sMsg) {
        // NOTICE: the receiver will be group ID in a group message here
        assert !isBroadcast(sMsg) : "broadcast message has no key: " + sMsg;
        // decrypt key data with the receiver/group member's private key
        ID identifier = sMsg.getReceiver();
        return getUser(identifier).decrypt(key);
    }

    @SuppressWarnings("unchecked")
    @Override
    public SymmetricKey deserializeKey(byte[] key, ID sender, ID receiver, SecureMessage sMsg) {
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
    public byte[] decodeData(Object data, SecureMessage sMsg) {
        if (isBroadcast(sMsg)) {
            // broadcast message content will not be encrypted (just encoded to JsON),
            // so return the string data directly
            return UTF8.encode((String) data);
        }
        return Base64.decode((String) data);
    }

    @Override
    public byte[] decryptContent(byte[] data, SymmetricKey password, SecureMessage sMsg) {
        return password.decrypt(data);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Content deserializeContent(byte[] data, SymmetricKey password, SecureMessage sMsg) {
        assert sMsg.getData() != null;
        Map<String, Object> dict = (Map<String, Object>) JSON.decode(data);
        // TODO: translate short keys
        //       'T' -> 'type'
        //       'N' -> 'sn'
        //       'G' -> 'group'
        Content content = Content.parse(dict);
        assert content != null : "content error: " + data.length;

        if (!isBroadcast(sMsg)) {
            // check and cache key for reuse
            ID sender = sMsg.getSender();
            ID group = getOvertGroup(content);
            if (group == null) {
                ID receiver = sMsg.getReceiver();
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
    public byte[] signData(byte[] data, ID sender, SecureMessage sMsg) {
        return getUser(sender).sign(data);
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
    public boolean verifyDataSignature(byte[] data, byte[] signature, ID sender, ReliableMessage rMsg) {
        return getUser(sender).verify(data, signature);
    }

    public interface Packer {

        /**
         *  Get group ID which should be exposed to public network
         *
         * @param content - message content
         * @return exposed group ID
         */
        ID getOvertGroup(Content content);

        //
        //  InstantMessage -> SecureMessage -> ReliableMessage -> Data
        //

        /**
         *  Encrypt message content
         *
         * @param iMsg - plain message
         * @return encrypted message
         */
        SecureMessage encryptMessage(InstantMessage iMsg);

        /**
         *  Sign content data
         *
         * @param sMsg - encrypted message
         * @return network message
         */
        ReliableMessage signMessage(SecureMessage sMsg);

        /**
         *  Serialize network message
         *
         * @param rMsg - network message
         * @return data package
         */
        byte[] serializeMessage(ReliableMessage rMsg);

        //
        //  Data -> ReliableMessage -> SecureMessage -> InstantMessage
        //

        /**
         *  Deserialize network message
         *
         * @param data - data package
         * @return network message
         */
        ReliableMessage deserializeMessage(byte[] data);

        /**
         *  Verify encrypted content data
         *
         * @param rMsg - network message
         * @return encrypted message
         */
        SecureMessage verifyMessage(ReliableMessage rMsg);

        /**
         *  Decrypt message content
         *
         * @param sMsg - encrypted message
         * @return plain message
         */
        InstantMessage decryptMessage(SecureMessage sMsg);
    }

    public interface Processor {

        /**
         *  Process data package
         *
         * @param data - data to be processed
         * @return response data
         */
        byte[] process(byte[] data);

        /**
         *  Process network message
         *
         * @param rMsg - message to be processed
         * @return response message
         */
        ReliableMessage process(ReliableMessage rMsg);

        /**
         *  Process encrypted message
         *
         * @param sMsg - message to be processed
         * @param rMsg - message received
         * @return response message
         */
        SecureMessage process(SecureMessage sMsg, ReliableMessage rMsg);

        /**
         *  Process plain message
         *
         * @param iMsg - message to be processed
         * @param rMsg - message received
         * @return response message
         */
        InstantMessage process(InstantMessage iMsg, ReliableMessage rMsg);

        /**
         *  Process message content
         *
         * @param content - content to be processed
         * @param rMsg - message received
         * @return response content
         */
        Content process(Content content, ReliableMessage rMsg);
    }
}
