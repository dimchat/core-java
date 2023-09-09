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
package chat.dim.dkd.file;

import java.net.URI;
import java.util.Map;

import chat.dim.crypto.DecryptKey;
import chat.dim.crypto.SymmetricKey;
import chat.dim.dkd.BaseContent;
import chat.dim.format.TransportableData;
import chat.dim.protocol.ContentType;
import chat.dim.protocol.FileContent;

/**
 *  File message: {
 *      type : 0x10,
 *      sn   : 123,
 *
 *      URL      : "http://...", // download from CDN
 *      data     : "...",        // base64_encode(fileContent)
 *      filename : "photo.png"
 *      key      : {             // symmetric key to decrypt file content
 *          algorithm : "AES",   // "DES", ...
 *          data      : "{BASE64_ENCODE}",
 *          ...
 *      }
 *  }
 */
public class BaseFileContent extends BaseContent implements FileContent {

    private TransportableData attachment; // file content (not encrypted)
    private DecryptKey password;          // key to decrypt data

    public BaseFileContent(Map<String, Object> content) {
        super(content);
        // lazy load
        attachment = null;
        password = null;
    }

    public BaseFileContent(int type, String filename, TransportableData data) {
        super(type);
        if (filename != null) {
            put("filename", filename);
        }
        if (data != null) {
            put("data", data.toObject());
        }
        attachment = data;
        password = null;
    }

    public BaseFileContent(ContentType type, String filename, TransportableData data) {
        this(type.value, filename, data);
    }

    public BaseFileContent(String filename, TransportableData data) {
        this(ContentType.FILE, filename, data);
    }

    @Override
    public void setURL(URI url) {
        if (url == null) {
            remove("URL");
        } else {
            put("URL", url.toString());
        }
    }

    @Override
    public URI getURL() {
        String url = getString("URL", null);
        return url == null ? null : URI.create(url);
    }

    @Override
    public void setData(byte[] binary) {
        if (binary != null && binary.length > 0) {
            TransportableData ted = TransportableData.create(binary);
            put("data", ted.toObject());
            attachment = ted;
        } else {
            remove("data");
            attachment = null;
        }
    }

    @Override
    public byte[] getData() {
        TransportableData ted = attachment;
        if (ted == null) {
            String base64 = getString("data", null);
            attachment = ted = TransportableData.parse(base64);
        }
        return ted == null ? null : ted.getData();
    }

    @Override
    public void setFilename(String name) {
        if (name == null) {
            remove("filename");
        } else {
            put("filename", name);
        }
    }

    @Override
    public String getFilename() {
        return getString("filename", null);
    }

    @Override
    public void setPassword(DecryptKey key) {
        setMap("key", key);
        password = key;
    }

    @Override
    public DecryptKey getPassword() {
        if (password == null) {
            Object info = get("key");
            password = SymmetricKey.parse(info);
        }
        return password;
    }
}
