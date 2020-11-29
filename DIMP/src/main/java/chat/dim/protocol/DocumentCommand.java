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
public class DocumentCommand extends MetaCommand {

    private Document doc;

    public DocumentCommand(Map<String, Object> dictionary) {
        super(dictionary);
        // lazy
        doc = null;
    }

    /**
     *  Send Meta and Document to new friend
     *
     * @param identifier - entity ID
     * @param meta - entity Meta
     * @param doc - entity Document
     */
    public DocumentCommand(ID identifier, Meta meta, Document doc) {
        super(PROFILE, identifier, meta);
        // document
        if (doc != null) {
            put("profile", doc.getMap());
        }
        this.doc = doc;
    }

    /**
     *  Response Entity Document
     *
     * @param identifier - entity ID
     * @param doc - entity Document
     */
    public DocumentCommand(ID identifier, Document doc) {
        this(identifier, null, doc);
    }

    /**
     *  Query Entity Document
     *
     * @param identifier - entity ID
     */
    public DocumentCommand(ID identifier) {
        this(identifier, null, null);
    }

    /**
     *  Query Entity Document for updating with current signature
     *
     * @param identifier - entity ID
     * @param signature - document signature
     */
    public DocumentCommand(ID identifier, String signature) {
        this(identifier, null, null);
        // signature
        if (signature != null) {
            put("signature", signature);
        }
    }

    /*
     * Document
     */
    @SuppressWarnings("unchecked")
    public Document getDocument() {
        if (doc == null) {
            Object data = get("profile");
            if (data == null) {
                data = get("document");
            }
            if (data instanceof String) {
                // compatible with v1.0
                //    "ID"        : "{ID}",
                //    "profile"   : "{JsON}",
                //    "signature" : "{BASE64}"
                Map<String, Object> map = new HashMap<>();
                map.put("ID", getIdentifier().toString());
                map.put("data", data);
                map.put("signature", get("signature"));
                data = map;
            } else {
                // (v1.1)
                //    "ID"      : "{ID}",
                //    "profile" : {
                //        "ID"        : "{ID}",
                //        "data"      : "{JsON}",
                //        "signature" : "{BASE64}"
                //    }
                assert data == null || data instanceof Map: "entity document data error: " + data;
            }
            if (data != null) {
                doc = Document.parse((Map<String, Object>) data);
            }
        }
        return doc;
    }

    public String getSignature() {
        return (String) get("signature");
    }

    //
    //  Factories
    //

    public static DocumentCommand query(ID identifier) {
        return new DocumentCommand(identifier);
    }
    public static DocumentCommand query(ID identifier, String signature) {
        return new DocumentCommand(identifier, signature);
    }

    public static DocumentCommand response(ID identifier, Document doc) {
        return new DocumentCommand(identifier, doc);
    }
    public static DocumentCommand response(ID identifier, Meta meta, Document doc) {
        return new DocumentCommand(identifier, meta, doc);
    }
}
