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
        // profile
        if (profile != null) {
            dictionary.put("profile", profile);
        }
        this.profile = profile;
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
        // signature
        if (signature != null) {
            dictionary.put("signature", signature);
        }
    }

    /*
     * Profile
     *
     */
    public Profile getProfile() {
        if (profile == null) {
            Object data = dictionary.get("profile");
            if (data instanceof String) {
                // compatible with v1.0
                //    "ID"        : "{ID}",
                //    "profile"   : "{JsON}",
                //    "signature" : "{BASE64}"
                Map<String, Object> map = new HashMap<>();
                map.put("ID", getIdentifier());
                map.put("data", data);
                map.put("signature", dictionary.get("signature"));
                data = map;
            } else {
                // (v1.1)
                //    "ID"      : "{ID}",
                //    "profile" : {
                //        "ID"        : "{ID}",
                //        "data"      : "{JsON}",
                //        "signature" : "{BASE64}"
                //    }
                assert data == null || data instanceof Map: "profile data error: " + data;
            }
            profile = Profile.getInstance(data);
        }
        return profile;
    }

    public String getSignature() {
        return (String) dictionary.get("signature");
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
