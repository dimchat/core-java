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
package chat.dim.protocol.command;

import chat.dim.core.Barrack;
import chat.dim.core.JsON;
import chat.dim.crypto.Base64;
import chat.dim.crypto.PrivateKey;
import chat.dim.crypto.PublicKey;
import chat.dim.mkm.Profile;
import chat.dim.mkm.entity.ID;
import chat.dim.mkm.entity.Meta;

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

    public ProfileCommand(HashMap<String, Object> dictionary) throws ClassNotFoundException {
        super(dictionary);
        // profile in JsON string
        String json   = (String) dictionary.get("profile");
        String base64 = (String) dictionary.get("signature");
        if (json == null || base64 == null) {
            profile = null;
            signature = null;
        } else {
            byte[] data = json.getBytes(StandardCharsets.UTF_8);
            byte[] sig = Base64.decode(base64);
            // get public key with ID
            PublicKey publicKey = Barrack.getInstance().getPublicKey(identifier);
            if (publicKey != null && publicKey.verify(data, sig)) {
                // convert JsON to profile
                profile = Profile.getInstance(JsON.decode(json));
                signature = sig;
            } else {
                throw new IllegalArgumentException("signature not match:" + dictionary);
            }
        }
    }

    public ProfileCommand(ID identifier, Meta meta, String json, byte[] signature) {
        super(identifier, meta);
        this.profile = Profile.getInstance(JsON.decode(json));
        this.signature = signature;
        if (json != null) {
            dictionary.put("profile", json);
        }
        if (signature != null) {
            dictionary.put("signature", Base64.encode(signature));
        }
    }

    public ProfileCommand(ID identifier, String json, byte[] signature) {
        this(identifier, null, json, signature);
    }

    public ProfileCommand(ID identifier, Meta meta, String json, PrivateKey privateKey) {
        this(identifier, meta, json, privateKey.sign(json.getBytes(StandardCharsets.UTF_8)));
    }

    public ProfileCommand(ID identifier, String json, PrivateKey privateKey) {
        this(identifier, null, json, privateKey);
    }

    public ProfileCommand(ID identifier, Meta meta, Profile json, PrivateKey privateKey) {
        this(identifier, meta, JsON.encode(json), privateKey);
    }

    public ProfileCommand(ID identifier, Profile json, PrivateKey privateKey) {
        this(identifier, null, json, privateKey);
    }
}
