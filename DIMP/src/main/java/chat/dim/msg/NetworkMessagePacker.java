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
import java.util.Map;

import chat.dim.protocol.ReliableMessage;
import chat.dim.protocol.SecureMessage;

public class NetworkMessagePacker {

    private final WeakReference<ReliableMessageDelegate> delegateRef;

    public NetworkMessagePacker(ReliableMessageDelegate delegate) {
        super();
        delegateRef = new WeakReference<>(delegate);
    }

    protected ReliableMessageDelegate getDelegate() {
        return delegateRef.get();
    }

    /*
     *  Verify the Reliable Message to Secure Message
     *
     *    +----------+      +----------+
     *    | sender   |      | sender   |
     *    | receiver |      | receiver |
     *    | time     |  ->  | time     |
     *    |          |      |          |
     *    | data     |      | data     |  1. verify(data, signature, sender.PK)
     *    | key/keys |      | key/keys |
     *    | signature|      +----------+
     *    +----------+
     */

    /**
     *  Verify 'data' and 'signature' field with sender's public key
     *
     * @return SecureMessage object
     */
    public SecureMessage verify(ReliableMessage rMsg) {
        byte[] data = rMsg.getData();
        if (data == null) {
            throw new NullPointerException("failed to decode content data: " + rMsg);
        }
        byte[] signature = rMsg.getSignature();
        if (signature == null) {
            throw new NullPointerException("failed to decode message signature: " + rMsg);
        }
        // 1. verify data signature with sender's public key
        if (getDelegate().verifyDataSignature(data, signature, rMsg)) {
            // 2. pack message
            Map<?, ?> map = rMsg.copyMap(false);
            map.remove("signature");
            return SecureMessage.parse(map);
        } else {
            //throw new RuntimeException("message signature not match: " + rMsg);
            return null;
        }
    }

}
