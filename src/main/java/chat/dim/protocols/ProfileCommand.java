package chat.dim.protocols;

import chat.dim.core.Barrack;
import chat.dim.crypto.PrivateKey;
import chat.dim.crypto.PublicKey;
import chat.dim.dkd.Utils;
import chat.dim.mkm.Profile;
import chat.dim.mkm.entity.ID;
import chat.dim.mkm.entity.Meta;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

/**
 *  Command message: {
 *      type : 0x88,
 *      sn   : 123,
 *
 *      command   : "profile",  // command name
 *      ID        : "{ID}",     // entity ID
 *      meta      : {...},      // only for handshaking with new friend
 *      profile   : "{...}",    // json(profile); when profile is empty, means query for ID
 *      signature : "{BASE64}", // sign(json(profile))
 *  }
 */
public class ProfileCommand extends MetaCommand {

    public final Profile profile;
    public final byte[] signature;

    public ProfileCommand(ProfileCommand content) {
        super(content);
        this.profile   = content.profile;
        this.signature = content.signature;
    }

    public ProfileCommand(HashMap<String, Object> dictionary) throws ClassNotFoundException {
        super(dictionary);
        // profile in JsON string
        String json   = (String) dictionary.get("profile");
        String base64 = (String) dictionary.get("signature");
        if (json == null || base64 == null) {
            this.profile   = null;
            this.signature = null;
        } else {
            byte[] data = json.getBytes(StandardCharsets.UTF_8);
            byte[] signature = Utils.base64Decode(base64);
            // get public key with ID
            PublicKey publicKey = Barrack.getInstance().getPublicKey(identifier);
            if (publicKey != null && publicKey.verify(data, signature)) {
                // convert JsON to profile
                this.profile = Profile.getInstance(Utils.jsonDecode(json));
                this.signature = signature;
            } else {
                throw new IllegalArgumentException("signature not match:" + dictionary);
            }
        }
    }

    public ProfileCommand(ID identifier, Meta meta, String profile, byte[] signature) {
        super(identifier, meta);
        this.profile   = Profile.getInstance(Utils.jsonDecode(profile));
        this.signature = signature;
        if (profile != null) {
            this.dictionary.put("profile", profile);
        }
        if (signature != null) {
            this.dictionary.put("signature", Utils.base64Encode(signature));
        }
    }

    public ProfileCommand(ID identifier, String profile, byte[] signature) {
        this(identifier, null, profile, signature);
    }

    public ProfileCommand(ID identifier, Meta meta, String profile, PrivateKey privateKey) {
        this(identifier, meta, profile, privateKey.sign(profile.getBytes(StandardCharsets.UTF_8)));
    }

    public ProfileCommand(ID identifier, String profile, PrivateKey privateKey) {
        this(identifier, null, profile, privateKey);
    }

    public ProfileCommand(ID identifier, Meta meta, Profile profile, PrivateKey privateKey) {
        this(identifier, meta, Utils.jsonEncode(profile), privateKey);
    }

    public ProfileCommand(ID identifier, Profile profile, PrivateKey privateKey) {
        this(identifier, null, profile, privateKey);
    }
}
