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

import chat.dim.mkm.ID;
import chat.dim.mkm.Meta;
import chat.dim.mkm.Profile;

/**
 *  Command message: {
 *      type : 0x88,
 *      sn   : 123,
 *
 *      command   : "profile", // command name
 *      ID        : "{ID}",    // entity ID
 *      meta      : {...},     // only for handshaking with new friend
 *      profile   : {...}      // when profile is empty, means query for ID
 *  }
 */
public class ProfileCommand extends MetaCommand {

    public ProfileCommand(Map<String, Object> dictionary) {
        super(dictionary);
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
        setMeta(meta);
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

    /*
     * Profile
     *
     */
    public Profile getProfile() {
        Profile profile;
        Object data = dictionary.get("profile");
        if (data instanceof Profile) {
            profile = (Profile) data;
        } else if (data instanceof Map) {
            // (v1.1)
            //  profile (dictionary): {
            //      "ID"        : "{ID}",
            //      "data"      : "{...}",
            //      "signature" : "{BASE64}"
            //  }
            profile = Profile.getInstance(data);
            // put back the Profile object for next access
            dictionary.put("profile", profile);
        } else if (data instanceof String) {
            // (v1.0)
            //  profile data (JsON)
            //  profile signature (Base64)
            Map<String, Object> map = new HashMap<>();
            map.put("ID", getIdentifier());
            map.put("data", data);
            map.put("signature", dictionary.get("signature"));
            profile = Profile.getInstance(map);
        } else {
            assert data == null;
            profile = null;
        }
        return profile;
    }

    public void setProfile(Profile profile) {
        if (profile == null) {
            dictionary.remove("profile");
        } else {
            dictionary.put("profile", profile);
        }
    }
}
