/* license: https://mit-license.org
 *
 *  DIMP : Decentralized Instant Messaging Protocol
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
package chat.dim.protocol;

import java.util.HashMap;
import java.util.Map;

import chat.dim.ID;
import chat.dim.Meta;
import chat.dim.Profile;

/**
 *  Command message: {
 *      type : 0x88,
 *      sn   : 123,
 *
 *      command   : "profile", // command name
 *      ID        : "{ID}",    // entity ID
 *      meta      : {...},     // only for handshaking with new friend
 *      profile   : {...},     // when profile is empty, means query for ID
 *      signature : "..."      // old profile's signature for querying
 *  }
 */
public class ProfileCommand extends MetaCommand {

    private Profile profile;

    public ProfileCommand(Map<String, Object> dictionary) {
        super(dictionary);
        // lazy
        profile = null;
    }

    /**
     *  Send Meta and Profile to new friend
     *
     * @param identifier - entity ID
     * @param meta - entity Meta
     * @param profile - entity Profile
     */
    public ProfileCommand(ID identifier, Meta meta, Profile profile) {
        super(PROFILE, identifier, meta);
        setProfile(profile);
    }

    /**
     *  Response Profile
     *
     * @param identifier - entity ID
     * @param profile - entity Profile
     */
    public ProfileCommand(ID identifier, Profile profile) {
        this(identifier, null, profile);
    }

    /**
     *  Query Profile
     *
     * @param identifier - entity ID
     */
    public ProfileCommand(ID identifier) {
        this(identifier, null, null);
    }

    /**
     *  Query profile for updating with current signature
     *
     * @param identifier - entity ID
     * @param signature - profile signature
     */
    public ProfileCommand(ID identifier, String signature) {
        this(identifier, null, null);
        setSignature(signature);
    }

    /*
     * Profile
     *
     */
    public Profile getProfile() {
        if (this.profile == null) {
            Object data = dictionary.get("profile");
            if (data instanceof Profile) {
                this.profile = (Profile) data;
            } else if (data instanceof Map) {
                // (v1.1)
                //  profile (dictionary): {
                //      "ID"        : "{ID}",
                //      "data"      : "{...}",
                //      "signature" : "{BASE64}"
                //  }
                this.profile = Profile.getInstance(data);
                // put back the Profile object for next access
                dictionary.put("profile", this.profile);
            } else if (data instanceof String) {
                // (v1.0)
                //  profile data (JsON)
                //  profile signature (Base64)
                Map<String, Object> map = new HashMap<>();
                map.put("ID", getIdentifier());
                map.put("data", data);
                map.put("signature", dictionary.get("signature"));
                this.profile = Profile.getInstance(map);
            } else {
                assert data == null;
                this.profile = null;
            }
        }
        return this.profile;
    }

    public void setProfile(Profile profile) {
        if (profile == null) {
            dictionary.remove("profile");
        } else {
            assert dictionary.get("data") == null;
            assert dictionary.get("signature") == null;
            dictionary.put("profile", profile);
        }
        this.profile = profile;
    }

    public String getSignature() {
        //assert dictionary.get("profile") == null;
        return (String) dictionary.get("signature");
    }

    public void setSignature(String signature) {
        if (signature == null) {
            dictionary.remove("signature");
        } else {
            assert dictionary.get("data") == null;
            assert dictionary.get("profile") == null;
            assert dictionary.get("meta") == null;
            dictionary.put("signature", signature);
        }
    }

    //
    //  Factories
    //

    public static ProfileCommand query(ID identifier) {
        return new ProfileCommand(identifier);
    }
    public static ProfileCommand query(ID identifier, String signature) {
        return new ProfileCommand(identifier, signature);
    }

    public static ProfileCommand response(ID identifier, Profile profile) {
        return new ProfileCommand(identifier, profile);
    }
    public static ProfileCommand response(ID identifier, Meta meta, Profile profile) {
        return new ProfileCommand(identifier, meta, profile);
    }
}
