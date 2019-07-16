import chat.dim.core.BarrackDelegate;
import chat.dim.crypto.PrivateKey;
import chat.dim.crypto.impl.PrivateKeyImpl;
import chat.dim.format.Base64;
import chat.dim.format.JSON;
import chat.dim.mkm.*;
import chat.dim.mkm.entity.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Facebook implements EntityDataSource, UserDataSource, GroupDataSource, BarrackDelegate {

    private static Facebook ourInstance = new Facebook();

    public static Facebook getInstance() {
        return ourInstance;
    }

    private Facebook() {
    }

    // memory caches
    private Map<Address, PrivateKey> privateKeyMap = new HashMap<>();
    private Map<Address, Meta>       metaMap       = new HashMap<>();
    private Map<Address, Profile>    profileMap    = new HashMap<>();
    private Map<Address, Account>    accountMap    = new HashMap<>();
    private Map<Address, User>       userMap       = new HashMap<>();

    public EntityDataSource entityDataSource;
    public UserDataSource userDataSource;
    public GroupDataSource groupDataSource;

    // "/sdcard/chat.dim.sechat/.mkm/"
    public String metaDirectory = null;

    // "/sdcard/chat.dim.sechat/.mkm/{address}.meta"
    private File getMetaFile(ID identifier) throws FileNotFoundException {
        if (metaDirectory == null) {
            throw new FileNotFoundException("meta directory not set");
        }
        return new File(metaDirectory, identifier.address + ".meta");
    }

    // local storage
    private Meta loadMeta(ID identifier) throws IOException, ClassNotFoundException {
        File file = getMetaFile(identifier);
        if (!file.exists()) {
            // meta file not found
            return null;
        }
        // load from JsON file
        FileInputStream fis = new FileInputStream(file);
        byte[] data = new byte[fis.available()];
        fis.read(data);
        fis.close();
        String json = new String(data, Charset.forName("UTF-8"));
        return Meta.getInstance(JSON.decode(json));
    }

    public boolean cachePrivateKey(PrivateKey privateKey, ID identifier) {
        privateKeyMap.put(identifier.address, privateKey);
        return true;
    }

    public boolean cacheMeta(Meta meta, ID identifier) {
        metaMap.put(identifier.address, meta);
        return true;
    }

    public boolean cacheProfile(Profile profile) {
        profileMap.put(profile.identifier.address, profile);
        return true;
    }

    public boolean cacheAccount(Account account) {
        if (account instanceof User) {
            return cacheUser((User) account);
        }
        if (account.dataSource == null) {
            account.dataSource = this;
        }
        accountMap.put(account.identifier.address, account);
        return true;
    }

    public boolean cacheUser(User user) {
        if (user.dataSource == null) {
            user.dataSource = this;
        }
        userMap.put(user.identifier.address, user);
        return true;
    }

    //---- UserDataSource

    @Override
    public PrivateKey getPrivateKeyForSignature(ID user) {
        PrivateKey key = privateKeyMap.get(user.address);
        if (key == null && userDataSource != null) {
            key = userDataSource.getPrivateKeyForSignature(user);
        }
        return key;
    }

    @Override
    public List<PrivateKey> getPrivateKeysForDecryption(ID user) {
        List<PrivateKey> list = userDataSource == null ? null : userDataSource.getPrivateKeysForDecryption(user);
        if (list != null && list.size() > 0) {
            privateKeyMap.put(user.address, list.get(0));
            return list;
        }
        PrivateKey key = privateKeyMap.get(user.address);
        if (key != null) {
            list = new ArrayList<>();
            list.add(key);
        }
        return list;
    }

    @Override
    public List<ID> getContacts(ID user) {
        return userDataSource == null ? null : userDataSource.getContacts(user);
    }

    //---- EntityDataSource

    @Override
    public boolean saveMeta(Meta meta, ID identifier) {
        return entityDataSource != null && entityDataSource.saveMeta(meta, identifier);
    }

    @Override
    public Meta getMeta(ID entity) {
        Meta meta = metaMap.get(entity.address);
        if (meta != null) {
            return meta;
        }
        if (entityDataSource == null) {
            return null;
        }
        meta = entityDataSource.getMeta(entity);
        if (meta != null) {
            cacheMeta(meta, entity);
        }
        return meta;
    }

    @Override
    public Profile getProfile(ID entity) {
        Profile profile = entityDataSource == null ? null : entityDataSource.getProfile(entity);
        if (profile == null) {
            profile = profileMap.get(entity.address);
        } else {
            profileMap.put(entity.address, profile);
        }
        return profile;
    }

    @Override
    public ID getFounder(ID group) {
        return null;
    }

    @Override
    public ID getOwner(ID group) {
        return null;
    }

    @Override
    public List<ID> getMembers(ID group) {
        return null;
    }

    //-------- BarrackDelegate

    @Override
    public Account getAccount(ID identifier) {
        Account account = accountMap.get(identifier.address);
        if (account == null) {
            account = userMap.get(identifier.address);
            if (account == null) {
                account = new Account(identifier);
            }
        }
        if (account.dataSource == null) {
            account.dataSource = this;
        }
        return account;
    }

    @Override
    public User getUser(ID identifier) {
        User user = userMap.get(identifier.address);
        if (user == null) {
            user = new User(identifier);
        }
        if (user.dataSource == null) {
            user.dataSource = this;
        }
        return user;
    }

    @Override
    public Group getGroup(ID identifier) {
        return null;
    }

    //-------- load immortals

    @SuppressWarnings("unchecked")
    private static Profile getProfile(Map dictionary, ID identifier, PrivateKey privateKey) {
        Profile profile;
        String profile_data = (String) dictionary.get("data");
        if (profile_data == null) {
            profile = new Profile(identifier);
            // set name
            String name = (String) dictionary.get("name");
            if (name == null) {
                List<String> names = (List<String>) dictionary.get("names");
                if (names != null) {
                    if (names.size() > 0) {
                        name = names.get(0);
                    }
                }
            }
            profile.setName(name);
            for (Object key : dictionary.keySet()) {
                if (key.equals("ID")) {
                    continue;
                }
                if (key.equals("name") || key.equals("names")) {
                    continue;
                }
                profile.setData((String) key, dictionary.get(key));
            }
            // sign profile
            profile.sign(privateKey);
        } else {
            String signature = (String) dictionary.get("signature");
            if (signature == null) {
                profile = new Profile(identifier, profile_data, null);
                // sign profile
                profile.sign(privateKey);
            } else {
                profile = new Profile(identifier, profile_data, Base64.decode(signature));
                // verify
                profile.verify(privateKey.getPublicKey());
            }
        }
        return profile;
    }

    static User loadBuiltInAccount(String filename) throws IOException, ClassNotFoundException {
        String jsonString = Utils.readTextFile(filename);
        Map<String, Object> dictionary = JSON.decode(jsonString);

        // ID
        ID identifier = ID.getInstance(dictionary.get("ID"));
        assert identifier != null;
        // meta
        Meta meta = Meta.getInstance(dictionary.get("meta"));
        assert meta != null && meta.matches(identifier);
        getInstance().cacheMeta(meta, identifier);
        // private key
        PrivateKey privateKey = PrivateKeyImpl.getInstance(dictionary.get("privateKey"));
        if (meta.key.matches(privateKey)) {
            // store private key into keychain
            getInstance().cachePrivateKey(privateKey, identifier);
        } else {
            throw new IllegalArgumentException("private key not match meta public key: " + privateKey);
        }
        // create user
        User user = new User(identifier);
        getInstance().cacheUser(user);

        // profile
        Profile profile = getProfile((Map) dictionary.get("profile"), identifier, privateKey);
        getInstance().cacheProfile(profile);

        return user;
    }

    static {
        try {
            loadBuiltInAccount("/mkm_hulk.js");
            loadBuiltInAccount("/mkm_moki.js");
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}
