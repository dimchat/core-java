/* license: https://mit-license.org
 *
 *  Dao-Ke-Dao: Universal Message Module
 *
 *                                Written in 2023 by Moky <albert.moky@gmail.com>
 *
 * ==============================================================================
 * The MIT License (MIT)
 *
 * Copyright (c) 2023 Albert Moky
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
package chat.dim.msg;

import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import chat.dim.crypto.SymmetricKey;
import chat.dim.format.TransportableData;
import chat.dim.format.UTF8;
import chat.dim.protocol.ID;
import chat.dim.protocol.InstantMessage;
import chat.dim.protocol.SecureMessage;

public class PlainMessagePacker {

    private final WeakReference<InstantMessageDelegate> delegateRef;

    public PlainMessagePacker(InstantMessageDelegate delegate) {
        super();
        delegateRef = new WeakReference<>(delegate);
    }

    protected InstantMessageDelegate getDelegate() {
        return delegateRef.get();
    }

    /*
     *  Encrypt the Instant Message to Secure Message
     *
     *    +----------+      +----------+
     *    | sender   |      | sender   |
     *    | receiver |      | receiver |
     *    | time     |  ->  | time     |
     *    |          |      |          |
     *    | content  |      | data     |  1. data = encrypt(content, PW)
     *    +----------+      | key/keys |  2. key  = encrypt(PW, receiver.PK)
     *                      +----------+
     */

    /**
     *  Encrypt message, replace 'content' field with encrypted 'data'
     *
     * @param password - symmetric key
     * @return SecureMessage object
     */
    public SecureMessage encrypt(InstantMessage iMsg, SymmetricKey password) {
        // 0. check attachment for File/Image/Audio/Video message content
        //    (do it in 'core' module)

        // 1. encrypt 'message.content' to 'message.data'
        Map<String, Object> map = prepareData(iMsg, password);

        // 2. encrypt symmetric key(password) to 'message.key'
        InstantMessageDelegate delegate = getDelegate();
        // 2.1. serialize symmetric key
        byte[] key = delegate.serializeKey(password, iMsg);
        if (key == null) {
            // A) broadcast message has no key
            // B) reused key
            return SecureMessage.parse(map);
        }

        // 2.2. encrypt symmetric key data
        key = delegate.encryptKey(key, iMsg.getReceiver(), iMsg);
        if (key == null) {
            // public key for encryption not found
            // TODO: suspend this message for waiting receiver's visa
            return null;
        }
        // 2.3. encode encrypted key data
        Object base64 = TransportableData.encode(key);
        assert base64 != null : "failed to encode key data: " + Arrays.toString(key);
        // 2.4. insert as 'key'
        map.put("key", base64);

        // 3. pack message
        return SecureMessage.parse(map);
    }

    /**
     *  Encrypt group message, replace 'content' field with encrypted 'data'
     *
     * @param password - symmetric key
     * @param members - group members
     * @return SecureMessage object
     */
    public SecureMessage encrypt(InstantMessage iMsg, SymmetricKey password, List<ID> members) {
        // 0. check attachment for File/Image/Audio/Video message content
        //    (do it in 'core' module)

        // 1. encrypt 'message.content' to 'message.data'
        Map<String, Object> map = prepareData(iMsg, password);

        // 2. serialize symmetric key
        InstantMessageDelegate delegate = getDelegate();
        // 2.1. serialize symmetric key
        byte[] key = delegate.serializeKey(password, iMsg);
        if (key == null) {
            // A) broadcast message has no key
            // B) reused key
            return SecureMessage.parse(map);
        }

        // encrypt key data to 'message.keys'
        Map<Object, Object> keys = new HashMap<>();
        int count = 0;
        byte[] data;
        Object base64;
        for (ID member: members) {
            // 2.2. encrypt symmetric key data
            data = delegate.encryptKey(key, member, iMsg);
            if (data == null) {
                // public key for member not found
                // TODO: suspend this message for waiting member's visa
                continue;
            }
            // 2.3. encode encrypted key data
            base64 = TransportableData.encode(data);
            assert base64 != null : "failed to encode key data: " + Arrays.toString(data);
            // 2.4. insert to 'message.keys' with member ID
            keys.put(member.toString(), base64);
            ++count;
        }
        if (count == 0) {
            // public key for member(s) not found
            // TODO: suspend this message for waiting member's visa
            return null;
        }
        map.put("keys", keys);

        // 3. pack message
        return SecureMessage.parse(map);
    }

    private Map<String, Object> prepareData(InstantMessage iMsg, SymmetricKey password) {
        InstantMessageDelegate delegate = getDelegate();
        // 1. serialize message content
        byte[] data = delegate.serializeContent(iMsg.getContent(), password, iMsg);
        assert data != null : "failed to serialize content: " + iMsg.getContent();
        // 2. encrypt content data with password
        data = delegate.encryptContent(data, password, iMsg);
        assert data != null : "failed to encrypt content with key: " + password;
        // 3. encode encrypted data
        Object encoded;
        if (BaseMessage.isBroadcast(iMsg)) {
            // broadcast message content will not be encrypted (just encoded to JsON),
            // so no need to encode to Base64 here
            encoded = UTF8.decode(data);
        } else {
            // message content had been encrypted by a symmetric key,
            // so the data should be encoded here (with algorithm 'base64' as default).
            encoded = TransportableData.encode(data);
        }
        assert encoded != null : "failed to encode content data: " + Arrays.toString(data);
        // 4. replace 'content' with encrypted 'data'
        Map<String, Object> map = iMsg.copyMap(false);
        map.remove("content");
        map.put("data", encoded);
        return map;
    }

}
