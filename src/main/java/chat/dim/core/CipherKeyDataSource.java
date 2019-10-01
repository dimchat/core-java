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

import chat.dim.crypto.SymmetricKey;
import chat.dim.crypto.impl.SymmetricKeyImpl;
import chat.dim.mkm.ID;

import java.util.HashMap;
import java.util.Map;

/**
 *  Symmetric key for broadcast message,
 *  which will do nothing when en/decoding message data
 */
final class PlainKey extends SymmetricKeyImpl {

    private final static String PLAIN = "PLAIN";

    public PlainKey(Map<String, Object> dictionary) {
        super(dictionary);
    }

    @Override
    public byte[] encrypt(byte[] plaintext) {
        return plaintext;
    }

    @Override
    public byte[] decrypt(byte[] ciphertext) {
        return ciphertext;
    }

    @Override
    public byte[] getData() {
        return new byte[0];
    }

    //-------- Runtime --------

    private static SymmetricKey ourInstance = null;

    public static SymmetricKey getInstance() {
        if (ourInstance == null) {
            Map<String, Object> dictionary = new HashMap<>();
            dictionary.put("algorithm", PLAIN);
            ourInstance = new PlainKey(dictionary);
        }
        return ourInstance;
    }

    static {
        // PLAIN
        register(PLAIN, PlainKey.class);
    }
}

public interface CipherKeyDataSource {

    /**
     *  Get cipher key for encrypt message from 'sender' to 'receiver'
     *
     * @param sender - from where (user or contact ID)
     * @param receiver - to where (contact or user/group ID)
     * @return cipher key
     */
    SymmetricKey cipherKey(ID sender, ID receiver);

    /**
     *  Cache cipher key for reusing, with the direction (from 'sender' to 'receiver')
     *
     * @param sender - from where (user or contact ID)
     * @param receiver - to where (contact or user/group ID)
     * @param key - cipher key
     */
    void cacheCipherKey(ID sender, ID receiver, SymmetricKey key);

    /**
     *  Update/create cipher key for encrypt message content
     *
     * @param sender - from where (user ID)
     * @param receiver - to where (contact/group ID)
     * @param key - old key to be reused (nullable)
     * @return new key
     */
    SymmetricKey reuseCipherKey(ID sender, ID receiver, SymmetricKey key);
}
