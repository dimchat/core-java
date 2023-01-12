/* license: https://mit-license.org
 *
 *  Ming-Ke-Ming : Decentralized User Identity Authentication
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
package chat.dim.mkm;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import chat.dim.crypto.SignKey;
import chat.dim.crypto.VerifyKey;
import chat.dim.format.Base64;
import chat.dim.format.JSONMap;
import chat.dim.format.UTF8;
import chat.dim.protocol.Document;
import chat.dim.protocol.ID;
import chat.dim.type.Dictionary;

public class BaseDocument extends Dictionary implements Document {

    private ID identifier;

    private String json;  // JsON.encode(properties)
    private byte[] sig;   // LocalUser(identifier).sign(data)

    private Map<String, Object> properties;
    private int status;        // 1 for valid, -1 for invalid

    /**
     *  Create Entity Document
     *
     *  @param dictionary - info
     */
    public BaseDocument(Map<String, Object> dictionary) {
        super(dictionary);
        // lazy
        identifier = null;

        json = null;
        sig = null;

        properties = null;

        status = 0;
    }

    /**
     *  Create entity document with data and signature loaded from local storage
     *
     * @param identifier - entity ID
     * @param data - document data in JsON format
     * @param signature - signature of document data in Base64 format
     */
    public BaseDocument(ID identifier, String data, String signature) {
        super();

        // ID
        put("ID", identifier.toString());
        this.identifier = identifier;

        // json data
        put("data", data);
        this.json = data;

        // signature
        put("signature", signature);
        this.sig = null;  // lazy

        properties = null;

        // all documents must be verified before saving into local storage
        status = 1;
    }

    /**
     *  Create a new empty document
     *
     * @param identifier - entity ID
     * @param type       - document type
     */
    public BaseDocument(ID identifier, String type) {
        super();

        // ID
        put("ID", identifier.toString());
        this.identifier = identifier;

        json = null;
        sig = null;

        if (type != null && type.length() > 0) {
            properties = new HashMap<>();
            properties.put("type", type);
        } else {
            properties = null;
        }

        status = 0;
    }

    @Override
    public boolean isValid() {
        return status > 0;
    }

    @Override
    public String getType() {
        String type = (String) getProperty("type");
        if (type == null) {
            type = getString("type");
        }
        return type;
    }

    @Override
    public ID getIdentifier() {
        if (identifier == null) {
            identifier = ID.parse(get("ID"));
        }
        return identifier;
    }

    /**
     *  Get serialized properties
     *
     * @return JsON string
     */
    private String getData() {
        if (json == null) {
            json = getString("data");
        }
        return json;
    }

    /**
     *  Get signature for serialized properties
     *
     * @return signature data
     */
    private byte[] getSignature() {
        if (sig == null) {
            String base64 = getString("signature");
            if (base64 != null) {
                sig = Base64.decode(base64);
            }
        }
        return sig;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> getProperties() {
        if (status < 0) {
            // invalid
            return null;
        }
        if (properties == null) {
            String data = getData();
            if (data == null) {
                // create new properties
                properties = new HashMap<>();
            } else {
                Map<String, Object> info = (Map<String, Object>) JSONMap.decode(data);
                assert info != null : "document data error: " + toMap();
                properties = info;
            }
        }
        return properties;
    }

    @Override
    public Object getProperty(String name) {
        Map<String, Object> dict = getProperties();
        if (dict == null) {
            return null;
        }
        return dict.get(name);
    }

    @Override
    public void setProperty(String name, Object value) {
        // 1. reset status
        assert status >= 0 : "status error: " + this;
        status = 0;
        // 2. update property value with name
        Map<String, Object> dict = getProperties();
        assert dict != null : "failed to get properties: " + this;
        if (value == null) {
            dict.remove(name);
        } else {
            dict.put(name, value);
        }
        // 3. clear data signature after properties changed
        remove("data");
        remove("signature");
        json = null;
        sig = null;
    }

    @Override
    public boolean verify(VerifyKey publicKey) {
        if (status > 0) {
            // already verify OK
            return true;
        }
        String data = getData();
        byte[] signature = getSignature();
        if (data == null) {
            // NOTICE: if data is empty, signature should be empty at the same time
            //         this happen while entity document not found
            if (signature == null) {
                status = 0;
            } else {
                // data signature error
                status = -1;
            }
        } else if (signature == null) {
            // signature error
            status = -1;
        } else if (publicKey.verify(UTF8.encode(data), signature)) {
            // signature matched
            status = 1;
        }
        // NOTICE: if status is 0, it doesn't mean the entity document is invalid,
        //         try another key
        return status == 1;
    }

    @Override
    public byte[] sign(SignKey privateKey) {
        if (status > 0) {
            // already signed/verified
            assert json != null : "document data error";
            return getSignature();
        }
        // 1. update sign time
        setProperty("time", System.currentTimeMillis() / 1000.0d);
        // 2. encode & sign
        String data = JSONMap.encode(getProperties());
        if (data == null || data.length() == 0) {
            // properties error
            return null;
        }
        byte[] signature = privateKey.sign(UTF8.encode(data));
        if (signature == null || signature.length == 0) {
            // signature error
            return null;
        }
        // 3. update 'data' & 'signature' fields
        put("data", data);
        put("signature", Base64.encode(signature));
        json = data;
        sig = signature;
        // 4. update status
        status = 1;
        return sig;
    }

    //---- properties getter/setter

    @Override
    public Date getTime() {
        Object timestamp = getProperty("time");
        if (timestamp == null) {
            return null;
        }
        double seconds = ((Number) timestamp).doubleValue();
        long millis = (long) (seconds * 1000);
        return new Date(millis);
    }

    @Override
    public String getName() {
        return (String) getProperty("name");
    }

    @Override
    public void setName(String value) {
        setProperty("name", value);
    }
}

