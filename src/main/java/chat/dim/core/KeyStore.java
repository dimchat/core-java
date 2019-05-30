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
import chat.dim.format.JSON;
import chat.dim.mkm.User;
import chat.dim.mkm.entity.Address;
import chat.dim.mkm.entity.ID;

import java.io.*;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public final class KeyStore {

    private static KeyStore ourInstance = new KeyStore();

    public static KeyStore getInstance() {
        return ourInstance;
    }

    private KeyStore() {
    }

    public User currentUser;

    // memory caches
    private Map<Address, Map<Address, SymmetricKey>> keyTable = new HashMap<>();
    private boolean isDirty = false;

    public SymmetricKey getKey(ID sender, ID receiver) {
        Map<Address, SymmetricKey> keyMap = keyTable.get(sender.address);
        if (keyMap == null) {
            return null;
        } else {
            return keyMap.get(receiver.address);
        }
    }

    public void setKey(SymmetricKey key, ID sender, ID receiver) {
        setKey(key, sender.address, receiver.address);
    }

    private void setKey(SymmetricKey key, Address from, Address to) {
        Map<Address, SymmetricKey> keyMap = keyTable.computeIfAbsent(from, k -> new HashMap<>());
        keyMap.put(to, key);
        isDirty = true;
    }

    public boolean flush(String path) throws IOException {
        if (!isDirty) {
            return false;
        }

        // write into key store file
        File file = new File(path);
        if (file.exists()) {
            file.delete();
        }
        FileOutputStream fos = new FileOutputStream(file);
        String json = JSON.encode(keyTable);
        fos.write(json.getBytes(Charset.forName("UTF-8")));
        fos.close();
        return true;
    }

    @SuppressWarnings("unchecked")
    public boolean reload(String path) throws IOException, ClassNotFoundException {
        File file = new File(path);
        if (!file.exists()) {
            return false;
        }
        // load from key store file
        FileInputStream fis = new FileInputStream(file);
        byte[] data = new byte[fis.available()];
        if (fis.read(data) <= 0) {
            fis.close();
            throw new EOFException("nothing read from the key store file:" + path);
        }
        fis.close();
        String json = new String(data, Charset.forName("UTF-8"));
        Map<String, Object> table = JSON.decode(json);
        boolean dirty = isDirty;

        Map<String, Object> map;
        Set<String> senders = table.keySet();
        Set<String> receivers;

        for (String sender : senders) {
            map = (Map) table.get(sender);
            receivers = map.keySet();
            for (String receiver : receivers) {
                setKey(SymmetricKey.getInstance(map.get(receiver)),
                        Address.getInstance(sender),
                        Address.getInstance(receiver));
            }
        }

        isDirty = dirty;
        return true;
    }
}
