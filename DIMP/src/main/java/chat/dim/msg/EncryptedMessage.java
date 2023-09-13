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

import java.util.Map;

import chat.dim.format.TransportableData;
import chat.dim.format.UTF8;
import chat.dim.protocol.SecureMessage;

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
 *      data     : "...",  // base64_encode( symmetric_encrypt(content))
 *      key      : "...",  // base64_encode(asymmetric_encrypt(password))
 *      keys     : {
 *          "ID1": "key1", // base64_encode(asymmetric_encrypt(password))
 *      }
 *  }
 */
public class EncryptedMessage extends BaseMessage implements SecureMessage {

    private TransportableData body;
    private TransportableData encryptedKey;
    private Map<String, Object> keys;  // String => String

    public EncryptedMessage(Map<String, Object> msg) {
        super(msg);
        // lazy load
        body = null;
        encryptedKey = null;
        keys = null;
    }

    @Override
    public byte[] getData() {
        TransportableData ted = body;
        if (ted == null) {
            Object base64 = get("data");
            assert base64 != null : "message data cannot be empty: " + toMap();
            if (!isBroadcast(this)) {
                // message content had been encrypted by a symmetric key,
                // so the data should be encoded here (with algorithm 'base64' as default).
                body = ted = TransportableData.parse(base64);
            } else if (base64 instanceof String) {
                // broadcast message content will not be encrypted (just encoded to JsON),
                // so return the string data directly
                byte[] plaintext = UTF8.encode((String) base64);  // JsON
                body = ted = TransportableData.create(plaintext);
            } else {
                assert false : "content data error: " + base64;
            }
        }
        return ted == null ? null : ted.getData();
    }

    @Override
    public byte[] getEncryptedKey() {
        TransportableData ted = encryptedKey;
        if (ted == null) {
            Object base64 = get("key");
            if (base64 == null) {
                // check 'keys'
                Map<?, ?> keys = getEncryptedKeys();
                if (keys != null) {
                    base64 = keys.get(getReceiver().toString());
                }
            }
            encryptedKey = ted = TransportableData.parse(base64);
        }
        return ted == null ? null : ted.getData();
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

}
