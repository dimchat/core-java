/* license: https://mit-license.org
 *
 *  Dao-Ke-Dao: Universal Message Module
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
package chat.dim.msg;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import chat.dim.crypto.SymmetricKey;
import chat.dim.protocol.Content;
import chat.dim.protocol.ID;
import chat.dim.protocol.InstantMessage;
import chat.dim.protocol.ReliableMessage;
import chat.dim.protocol.SecureMessage;
import chat.dim.type.Copier;

/**
 *  Secure Message
 *  ~~~~~~~~~~~~~~
 *  Instant Message encrypted by a symmetric key
 *
 *  data format: {
 *      //-- envelope
 *      sender   : "moki@xxx",
 *      receiver : "hulk@yyy",
 *      time     : 123,
 *      //-- content data and key/keys
 *      data     : "...",  // base64_encode(symmetric)
 *      key      : "...",  // base64_encode(asymmetric)
 *      keys     : {
 *          "ID1": "key1", // base64_encode(asymmetric)
 *      }
 *  }
 */
public class EncryptedMessage extends BaseMessage implements SecureMessage {

    private byte[] data;
    private byte[] key;
    private Map<String, Object> keys;

    public EncryptedMessage(Map<String, Object> msg) {
        super(msg);
        // lazy load
        data = null;
        key = null;
        keys = null;
    }

    @Override
    public SecureMessage.Delegate getDelegate() {
        return (SecureMessage.Delegate) super.getDelegate();
    }

    @Override
    public byte[] getData() {
        if (data == null) {
            Object base64 = get("data");
            assert base64 != null : "content data cannot be empty";
            data = getDelegate().decodeData(base64, this);
        }
        return data;
    }

    @Override
    public byte[] getEncryptedKey() {
        if (key == null) {
            Object base64 = get("key");
            if (base64 == null) {
                // check 'keys'
                Map<String, Object> keys = getEncryptedKeys();
                if (keys != null) {
                    base64 = keys.get(getReceiver().toString());
                }
            }
            if (base64 != null) {
                key = getDelegate().decodeKey(base64, this);
            }
        }
        return key;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map<String, Object> getEncryptedKeys() {
        if (keys == null) {
            Object map = get("keys");
            if (map instanceof Map) {
                keys = (Map<String, Object>) map;
            }
        }
        return keys;
    }

    /*
     *  Decrypt the Secure Message to Instant Message
     *
     *    +----------+      +----------+
     *    | sender   |      | sender   |
     *    | receiver |      | receiver |
     *    | time     |  ->  | time     |
     *    |          |      |          |  1. PW      = decrypt(key, receiver.SK)
     *    | data     |      | content  |  2. content = decrypt(data, PW)
     *    | key/keys |      +----------+
     *    +----------+
     */

    /**
     *  Decrypt message, replace encrypted 'data' with 'content' field
     *
     * @return InstantMessage object
     */
    @Override
    public InstantMessage decrypt() {
        ID sender = getSender();
        ID receiver;
        ID group = getGroup();
        if (group == null) {
            // personal message
            // not split group message
            receiver = getReceiver();
        } else {
            // group message
            receiver = group;
        }

        // 1. decrypt 'message.key' to symmetric key
        SecureMessage.Delegate delegate = getDelegate();
        // 1.1. decode encrypted key data
        byte[] key = getEncryptedKey();
        // 1.2. decrypt key data
        if (key != null) {
            key = delegate.decryptKey(key, sender, receiver, this);
            if (key == null) {
                throw new NullPointerException("failed to decrypt key in msg: " + this);
            }
        }
        // 1.3. deserialize key
        //      if key is empty, means it should be reused, get it from key cache
        SymmetricKey password = delegate.deserializeKey(key, sender, receiver, this);
        if (password == null) {
            throw new NullPointerException("failed to get msg key: "
                    + sender + " -> " + receiver + ", " + Arrays.toString(key));
        }

        // 2. decrypt 'message.data' to 'message.content'
        // 2.1. decode encrypted content data
        byte[] data = getData();
        if (data == null) {
            throw new NullPointerException("failed to decode content data: " + this);
        }
        // 2.2. decrypt content data
        data = delegate.decryptContent(data, password, this);
        if (data == null) {
            throw new NullPointerException("failed to decrypt data with key: " + password);
        }
        // 2.3. deserialize content
        Content content = delegate.deserializeContent(data, password, this);
        if (content == null) {
            throw new NullPointerException("failed to deserialize content: " + Arrays.toString(data));
        }
        // 2.4. check attachment for File/Image/Audio/Video message content
        //      if file data not download yet,
        //          decrypt file data with password;
        //      else,
        //          save password to 'message.content.password'.
        //      (do it in 'core' module)

        // 3. pack message
        Map<String, Object> map = copyMap(false);
        map.remove("key");
        map.remove("keys");
        map.remove("data");
        map.put("content", content.toMap());
        return InstantMessage.parse(map);
    }

    /*
     *  Sign the Secure Message to Reliable Message
     *
     *    +----------+      +----------+
     *    | sender   |      | sender   |
     *    | receiver |      | receiver |
     *    | time     |  ->  | time     |
     *    |          |      |          |
     *    | data     |      | data     |
     *    | key/keys |      | key/keys |
     *    +----------+      | signature|  1. signature = sign(data, sender.SK)
     *                      +----------+
     */

    /**
     *  Sign message.data, add 'signature' field
     *
     * @return ReliableMessage object
     */
    @Override
    public ReliableMessage sign() {
        SecureMessage.Delegate delegate = getDelegate();
        // 1. sign with sender's private key
        byte[] signature = delegate.signData(getData(), getSender(), this);
        assert signature != null : "failed to sign message: " + this;
        // 2. encode signature
        Object base64 = delegate.encodeSignature(signature, this);
        assert base64 != null : "failed to encode signature: " + Arrays.toString(signature);
        // 3. pack message
        Map<String, Object> map = copyMap(false);
        map.put("signature", base64);
        return ReliableMessage.parse(map);
    }

    /*
     *  Split/Trim group message
     *
     *  for each members, get key from 'keys' and replace 'receiver' to member ID
     */

    /**
     *  Split the group message to single person messages
     *
     *  @param members - group members
     *  @return secure/reliable message(s)
     */
    @Override
    public List<SecureMessage> split(List<ID> members) {
        Map<String, Object> msg = copyMap(false);
        // check 'keys'
        Map<String, Object> keys = getEncryptedKeys();
        if (keys == null) {
            keys = new HashMap<>();
        } else {
            msg.remove("keys");
        }

        // 1. move the receiver(group ID) to 'group'
        //    this will help the receiver knows the group ID
        //    when the group message separated to multi-messages;
        //    if don't want the others know your membership,
        //    DON'T do this.
        msg.put("group", getReceiver().toString());

        List<SecureMessage> messages = new ArrayList<>(members.size());
        Object base64;
        SecureMessage item;
        for (ID member : members) {
            // 2. change 'receiver' to each group member
            msg.put("receiver", member.toString());
            // 3. get encrypted key
            base64 = keys.get(member.toString());
            if (base64 == null) {
                msg.remove("key");
            } else {
                msg.put("key", base64);
            }
            // 4. repack message
            item = SecureMessage.parse(Copier.copyMap(msg));
            if (item != null) {
                messages.add(item);
            }
        }

        return messages;
    }

    /**
     *  Trim the group message for a member
     *
     * @param member - group member ID/string
     * @return SecureMessage
     */
    @Override
    public SecureMessage trim(ID member) {
        Map<String, Object> msg = copyMap(false);
        // check 'keys'
        Map<String, Object> keys = getEncryptedKeys();
        if (keys != null) {
            // move key data from 'keys' to 'key'
            Object base64 = keys.get(member.toString());
            if (base64 != null) {
                msg.put("key", base64);
            }
            msg.remove("keys");
        }
        // check 'group'
        ID group = getGroup();
        if (group == null) {
            // if 'group' not exists, the 'receiver' must be a group ID here, and
            // it will not be equal to the member of course,
            // so move 'receiver' to 'group'
            msg.put("group", getReceiver().toString());
        }
        msg.put("receiver", member.toString());
        // repack
        return SecureMessage.parse(msg);
    }
}
