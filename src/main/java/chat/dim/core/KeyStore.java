package chat.dim.core;

import chat.dim.crypto.SymmetricKey;
import chat.dim.crypto.Utils;
import chat.dim.mkm.User;
import chat.dim.mkm.entity.Address;
import chat.dim.mkm.entity.ID;

import java.io.*;
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
        SymmetricKey key;
        Map<Address, SymmetricKey> keyMap = keyTable.get(sender.address);
        if (keyMap == null) {
            key = null;
        } else {
            key = keyMap.get(receiver.address);
        }
        if (key == null) {
            // create a new key & save it into the Key Store
            Map<String, Object> map = new HashMap<>();
            map.put("algorithm", "AES");
            try {
                key = SymmetricKey.getInstance(map);
                setKey(key, sender, receiver);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
        return key;
    }

    public boolean setKey(SymmetricKey key, ID sender, ID receiver) {
        return setKey(key, sender.address, receiver.address);
    }

    private boolean setKey(SymmetricKey key, Address from, Address to) {
        Map<Address, SymmetricKey> keyMap = keyTable.computeIfAbsent(from, k -> new HashMap<>());
        keyMap.put(to, key);
        isDirty = true;
        return true;
    }

    public boolean flush(String path) throws IOException {
        if (!isDirty) {
            return false;
        }

        // transform Address -> String
        // transform SymmetricKey -> Map
        Map<Address, SymmetricKey> keyMap;
        SymmetricKey key;

        Map<String, Object> table = new HashMap<>();
        Map<String, Object> map;

        Set<Address> senders = keyTable.keySet();
        Set<Address> receivers;

        for (Address sender : senders) {
            map = new HashMap<>();
            keyMap = keyTable.get(sender);
            receivers = keyMap.keySet();
            for (Address receiver :
                    receivers) {
                key = keyMap.get(receiver);
                map.put(receiver.toString(), key.toDictionary());
            }
            table.put(sender.toString(), map);
        }

        // write into key store file
        File file = new File(path);
        if (file.exists()) {
            file.delete();
        }
        FileOutputStream fos = new FileOutputStream(file);
        String json = Utils.jsonEncode(table);
        fos.write(json.getBytes("UTF-8"));
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
        fis.read(data);
        fis.close();
        String json = new String(data, "UTF-8");
        Map<String, Object> table = Utils.jsonDecode(json);
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
