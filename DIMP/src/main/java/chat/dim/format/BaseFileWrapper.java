/* license: https://mit-license.org
 * ==============================================================================
 * The MIT License (MIT)
 *
 * Copyright (c) 2023 Albert Moky
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
package chat.dim.format;

import java.net.URI;
import java.util.Map;

import chat.dim.protocol.DecryptKey;
import chat.dim.protocol.SymmetricKey;
import chat.dim.protocol.TransportableData;
import chat.dim.type.Dictionary;


/**
 *  File Content MixIn
 *
 *  <blockquote><pre>
 *  {
 *      'data'     : "...",        // base64_encode(fileContent)
 *      'filename' : "photo.png",
 *
 *      'URL'      : "http://...", // download from CDN
 *      // before fileContent uploaded to a public CDN,
 *      // it should be encrypted by a symmetric key
 *      'key'      : {             // symmetric key to decrypt file data
 *          'algorithm' : "AES",   // "DES", ...
 *          'data'      : "{BASE64_ENCODE}",
 *          ...
 *      }
 *  }
 *  </pre></blockquote>
 */
public class BaseFileWrapper extends Dictionary {

    // file content (not encrypted)
    private TransportableData attachment;

    // download from CDN
    private URI remoteURL;
    // key to decrypt data downloaded from CDN
    private DecryptKey password;

    public BaseFileWrapper(Map<String, Object> content) {
        super(content);
        // lazy load
        attachment = null;
        remoteURL = null;
        password = null;
    }

    /**
     *  file data
     */

    public TransportableData getData() {
        TransportableData ted = attachment;
        if (ted == null) {
            Object base64 = get("data");
            attachment = ted = TransportableData.parse(base64);
        }
        return ted;
    }

    public void setData(TransportableData ted) {
        if (ted == null) {
            remove("data");
        } else {
            put("data", ted.toObject());
        }
        attachment = ted;
    }
    // set binary data
    public void setData(byte[] binary) {
        TransportableData ted;
        if (binary == null || binary.length == 0) {
            remove("data");
            ted = null;
        } else {
            ted = TransportableData.create(binary);
            put("data", ted.toObject());
        }
        attachment = ted;
    }

    /**
     *  file name
     */

    public String getFilename() {
        return getString("filename");
    }

    public void setFilename(String name) {
        if (name == null/* || name.isEmpty()*/) {
            remove("filename");
        } else {
            put("filename", name);
        }
    }

    /**
     *  download URL
     */

    public URI getURL() {
        URI remote = remoteURL;
        if (remote == null) {
            String locator = getString("URL");
            if (locator != null && locator.length() > 0) {
                remoteURL = remote = URI.create(locator);
            }
        }
        return remote;
    }

    public void setURL(URI remote) {
        if (remote == null) {
            remove("URL");
        } else {
            put("URL", remote.toString());
        }
        remoteURL = remote;
    }

    /**
     *  decrypt key
     */

    public DecryptKey getPassword() {
        DecryptKey key = password;
        if (key == null) {
            key = SymmetricKey.parse(get("key"));
            password = key;
        }
        return key;
    }

    public void setPassword(DecryptKey key) {
        setMap("key", key);
        password = key;
    }

}
