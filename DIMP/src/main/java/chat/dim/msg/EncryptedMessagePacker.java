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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import chat.dim.crypto.SymmetricKey;
import chat.dim.format.TransportableData;
import chat.dim.protocol.Content;
import chat.dim.protocol.ID;
import chat.dim.protocol.InstantMessage;
import chat.dim.protocol.ReliableMessage;
import chat.dim.protocol.SecureMessage;
import chat.dim.type.Copier;

public class EncryptedMessagePacker {

    private final WeakReference<SecureMessageDelegate> delegateRef;

    public EncryptedMessagePacker(SecureMessageDelegate delegate) {
        super();
        delegateRef = new WeakReference<>(delegate);
    }

    protected SecureMessageDelegate getDelegate() {
        return delegateRef.get();
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
    public InstantMessage decrypt(SecureMessage sMsg) {
        ID receiver;
        ID group = sMsg.getGroup();
        if (group == null) {
            // personal message
            // not split group message
            receiver = sMsg.getReceiver();
        } else {
            // group message
            receiver = group;
        }

        // 1. decrypt 'message.key' to symmetric key
        SecureMessageDelegate delegate = getDelegate();
        // 1.1. decode encrypted key data
        byte[] key = sMsg.getEncryptedKey();
        // 1.2. decrypt key data
        if (key != null) {
            key = delegate.decryptKey(key, receiver, sMsg);
            if (key == null) {
                throw new NullPointerException("failed to decrypt key in msg: " + sMsg);
            }
        }
        // 1.3. deserialize key
        //      if key is empty, means it should be reused, get it from key cache
        SymmetricKey password = delegate.deserializeKey(key, receiver, sMsg);
        if (password == null) {
            throw new NullPointerException("failed to get msg key: "
                    + sMsg.getSender() + " -> " + receiver + ", " + Arrays.toString(key));
        }

        // 2. decrypt 'message.data' to 'message.content'
        // 2.1. decode encrypted content data
        byte[] data = sMsg.getData();
        if (data == null) {
            throw new NullPointerException("failed to decode content data: " + sMsg);
        }
        // 2.2. decrypt content data
        data = delegate.decryptContent(data, password, sMsg);
        if (data == null) {
            throw new NullPointerException("failed to decrypt data with key: " + password);
        }
        // 2.3. deserialize content
        Content content = delegate.deserializeContent(data, password, sMsg);
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
        Map<String, Object> map = sMsg.copyMap(false);
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
    public ReliableMessage sign(SecureMessage sMsg) {
        SecureMessageDelegate delegate = getDelegate();
        // 1. sign with sender's private key
        byte[] signature = delegate.signData(sMsg.getData(), sMsg);
        assert signature != null : "failed to sign message: " + sMsg;
        // 2. encode signature
        Object base64 = TransportableData.encode(signature);
        assert base64 != null : "failed to encode signature: " + Arrays.toString(signature);
        // 3. pack message
        Map<String, Object> map = sMsg.copyMap(false);
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
    public List<SecureMessage> split(SecureMessage sMsg, List<ID> members) {
        Map<String, Object> msg = sMsg.copyMap(false);
        // check 'keys'
        Map<?, ?> keys = sMsg.getEncryptedKeys();
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
        msg.put("group", sMsg.getReceiver().toString());

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
    public SecureMessage trim(SecureMessage sMsg, ID member) {
        Map<String, Object> msg = sMsg.copyMap(false);
        // check 'keys'
        Map<?, ?> keys = sMsg.getEncryptedKeys();
        if (keys != null) {
            // move key data from 'keys' to 'key'
            Object base64 = keys.get(member.toString());
            if (base64 != null) {
                msg.put("key", base64);
            }
            msg.remove("keys");
        }
        // check 'group'
        ID group = sMsg.getGroup();
        if (group == null) {
            // if 'group' not exists, the 'receiver' must be a group ID here, and
            // it will not be equal to the member of course,
            // so move 'receiver' to 'group'
            msg.put("group", sMsg.getReceiver().toString());
        }
        msg.put("receiver", member.toString());
        // repack
        return SecureMessage.parse(msg);
    }

}
