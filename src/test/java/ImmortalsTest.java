
import chat.dim.crypto.PrivateKey;
import chat.dim.crypto.impl.PrivateKeyImpl;
import chat.dim.format.Base64;
import chat.dim.format.JSON;
import chat.dim.mkm.User;
import chat.dim.mkm.entity.ID;
import chat.dim.mkm.entity.Meta;
import chat.dim.mkm.entity.Profile;
import org.junit.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class ImmortalsTest {

    private Facebook facebook = Facebook.getInstance();

    @SuppressWarnings("unchecked")
    private User loadBuiltInAccount(String filename) throws IOException, ClassNotFoundException {
        String jsonString = Utils.readTextFile(filename);
        Map<String, Object> dictionary = JSON.decode(jsonString);

        // ID
        ID identifier = ID.getInstance(dictionary.get("ID"));
        assert identifier != null;
        // meta
        Meta meta = Meta.getInstance(dictionary.get("meta"));
        assert meta != null && meta.matches(identifier);
        facebook.cacheMeta(meta, identifier);
        // private key
        PrivateKey privateKey = PrivateKeyImpl.getInstance(dictionary.get("privateKey"));
        if (meta.key.matches(privateKey)) {
            // store private key into keychain
            facebook.cachePrivateKey(privateKey, identifier);
        } else {
            throw new IllegalArgumentException("private key not match meta public key: " + privateKey);
        }
        // create user
        User user = new User(identifier);
        facebook.cacheUser(user);

        // profile
        Profile profile;
        Map<String, Object> profile_dict = (Map<String, Object>) dictionary.get("profile");
        String profile_data = (String) profile_dict.get("data");
        if (profile_data == null) {
            profile = new Profile(identifier);
            // set name
            String name = (String) profile_dict.get("name");
            if (name == null) {
                List<String> names = (List<String>) profile_dict.get("names");
                if (names != null) {
                    if (names.size() > 0) {
                        name = names.get(0);
                    }
                }
            }
            profile.setName(name);
            for (String key : profile_dict.keySet()) {
                if (key.equals("ID")) {
                    continue;
                }
                if (key.equals("name") || key.equals("names")) {
                    continue;
                }
                profile.setData(key, profile_dict.get(key));
            }
            // sign profile
            profile.sign(privateKey);
        } else {
            String signature = (String) profile_dict.get("signature");
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
        facebook.cacheProfile(profile);

        return user;
    }

    @Test
    public void testImmortals() throws IOException, ClassNotFoundException {
        // Immortal Hulk
        User hulk = loadBuiltInAccount("/mkm_hulk.js");
        Log.info("hulk: " + hulk);

        Log.info("name: " + hulk.getName());
        Log.info("profile: " + facebook.getProfile(hulk.identifier));

        // Monkey King
        User moki = loadBuiltInAccount("/mkm_moki.js");
        Log.info("moki: " + moki);

        Log.info("name: " + moki.getName());
        Log.info("profile: " + facebook.getProfile(moki.identifier));
    }
}