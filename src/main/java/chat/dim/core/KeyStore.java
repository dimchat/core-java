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
import chat.dim.mkm.entity.ID;
import chat.dim.mkm.entity.NetworkType;

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

public abstract class KeyStore implements CipherKeyDataSource {

    // memory caches
    private Map<ID, Map<ID, SymmetricKey>> keyMap = new HashMap<>();
    private boolean isDirty = false;

    protected KeyStore() {
        super();
        // load keys from local storage
        try {
            updateKeys(loadKeys());
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     *  Trigger for saving cipher key table
     */
    public void flush() {
        if (!isDirty) {
            // nothing changed
            return;
        }
        if (saveKeys(keyMap)) {
            // keys saved
            isDirty = false;
        }
    }

    /**
     *  Callback for saving cipher key table into local storage
     *  (Override it to access database)
     *
     * @param keyMap - all cipher keys(with direction) from memory cache
     * @return YES on success
     */
    public abstract boolean saveKeys(Map keyMap);

    /**
     *  Load cipher key table from local storage
     *  (Override it to access database)
     *
     * @return keys map
     */
    public abstract Map loadKeys();

    /**
     *  Update cipher key table into memory cache
     *
     * @param keyMap - cipher keys(with direction) from local storage
     * @return NO on nothing changed
     * @throws ClassNotFoundException when key error
     */
    @SuppressWarnings("unchecked")
    public boolean updateKeys(Map keyMap) throws ClassNotFoundException {
        if (keyMap == null || keyMap.isEmpty()) {
            return false;
        }
        boolean changed = false;
        Map<String, Map<String, Object>> map = (Map<String, Map<String, Object>>)keyMap;
        for (Map.Entry<String, Map<String, Object>> entry1 : map.entrySet()) {
            ID from = ID.getInstance(entry1.getKey());
            Map<String, Object> table = entry1.getValue();
            for (Map.Entry<String, Object> entity2 : table.entrySet()) {
                ID to = ID.getInstance(entity2.getKey());
                SymmetricKey newKey = SymmetricKeyImpl.getInstance(entity2.getValue());
                assert newKey != null;
                // check whether exists an old key
                SymmetricKey oldKey = getCipherKey(from, to);
                if (oldKey != newKey) {
                    changed = true;
                }
                // cache key with direction
                setCipherKey(from, to, newKey);
            }
        }
        return changed;
    }

    private SymmetricKey getCipherKey(ID from, ID to) {
        assert from.isValid() && to.isValid();
        Map<ID, SymmetricKey> keyTable = keyMap.get(from);
        return keyTable == null ? null : keyTable.get(to);
    }

    private void setCipherKey(ID from, ID to, SymmetricKey key) {
        assert from.isValid() && to.isValid();
        Map<ID, SymmetricKey> keyTable = keyMap.computeIfAbsent(from, k -> new HashMap<>());
        assert key != null;
        keyTable.put(to, key);
    }

    static boolean isBroadcast(ID to) {
        NetworkType network = to.getType();
        if (network.isPerson()) {
            return to.equals(ID.ANYONE);
        } else if (network.isGroup()) {
            return to.equals(ID.EVERYONE);
        } else {
            return false;
        }
    }

    //-------- CipherKeyDataSource

    @Override
    public SymmetricKey cipherKey(ID sender, ID receiver) {
        if (isBroadcast(receiver)) {
            return PlainKey.getInstance();
        }
        return getCipherKey(sender, receiver);
    }

    @Override
    public void cacheCipherKey(ID sender, ID receiver, SymmetricKey key) {
        if (isBroadcast(receiver)) {
            return;
        }
        setCipherKey(sender, receiver, key);
        isDirty = true;
    }

    @Override
    public abstract SymmetricKey reuseCipherKey(ID sender, ID receiver, SymmetricKey key);
}
