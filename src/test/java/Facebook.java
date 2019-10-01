
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import chat.dim.core.Barrack;
import chat.dim.crypto.PrivateKey;
import chat.dim.crypto.impl.PrivateKeyImpl;
import chat.dim.format.Base64;
import chat.dim.format.JSON;
import chat.dim.mkm.*;

public class Facebook extends Barrack {
    private static Facebook ourInstance = new Facebook();
    public static Facebook getInstance() { return ourInstance; }
    private Facebook() {
        super();
    }

    // memory caches
    private Map<Address, PrivateKey> privateKeyMap = new HashMap<>();
    private Map<ID, Profile>         profileMap    = new HashMap<>();

    // "/sdcard/chat.dim.sechat/.mkm/"
    public String metaDirectory = "/tmp/.mkm/";

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

    //---- Private Key

    protected boolean cachePrivateKey(PrivateKey key, ID identifier) {
        assert identifier.isValid();
        privateKeyMap.put(identifier.address, key);
        return true;
    }

    //---- Profile

    protected boolean cacheProfile(Profile profile) {
        profileMap.put(profile.identifier, profile);
        return true;
    }

    //-------- SocialNetworkDataSource

    @Override
    public User getUser(ID identifier) {
        User user = super.getUser(identifier);
        if (user != null) {
            return user;
        }
        PrivateKey key = getPrivateKeyForSignature(identifier);
        if (key == null) {
            user = new User(identifier);
        } else {
            user = new LocalUser(identifier);
        }
        cacheUser(user);
        return user;
    }

    @Override
    public Group getGroup(ID identifier) {
        Group group = super.getGroup(identifier);
        if (group == null) {
            group = new Group(identifier);
            cacheGroup(group);
        }
        return group;
    }

    //---- EntityDataSource

    @Override
    public Meta getMeta(ID identifier) {
        Meta meta = super.getMeta(identifier);
        if (meta == null) {
            try {
                meta = loadMeta(identifier);
                if (meta != null) {
                    cacheMeta(meta, identifier);
                }
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
        return meta;
    }

    @Override
    public Profile getProfile(ID entity) {
        return profileMap.get(entity.address);
    }

    //---- UserDataSource

    @Override
    public PrivateKey getPrivateKeyForSignature(ID user) {
        return privateKeyMap.get(user.address);
    }

    @Override
    public List<PrivateKey> getPrivateKeysForDecryption(ID user) {
        List<PrivateKey> list = new ArrayList<>();
        PrivateKey key = privateKeyMap.get(user.address);
        if (key != null) {
            list.add(key);
        }
        return list;
    }

    @Override
    public List<ID> getContacts(ID user) {
        return null;
    }

    //---- GroupDataSource

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

    @SuppressWarnings("unchecked")
    static LocalUser loadBuiltInAccount(String filename) throws IOException, ClassNotFoundException {
        String json = Utils.readTextFile(filename);
        Map<String, Object> dict = (Map<String, Object>) JSON.decode(json);

        // ID
        ID identifier = ID.getInstance(dict.get("ID"));
        assert identifier != null;
        // meta
        Meta meta = Meta.getInstance(dict.get("meta"));
        assert meta != null && meta.matches(identifier);
        getInstance().cacheMeta(meta, identifier);
        // private key
        PrivateKey privateKey = PrivateKeyImpl.getInstance(dict.get("privateKey"));
        if (meta.key.matches(privateKey)) {
            // store private key into keychain
            getInstance().cachePrivateKey(privateKey, identifier);
        } else {
            throw new IllegalArgumentException("private key not match meta public key: " + privateKey);
        }
        // create user
        LocalUser user = new LocalUser(identifier);
        user.setDataSource(getInstance());

        // profile
        Profile profile = getProfile((Map) dict.get("profile"), identifier, privateKey);
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
