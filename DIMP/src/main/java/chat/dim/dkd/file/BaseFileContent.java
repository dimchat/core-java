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
 *      data     : "...",        // base64_encode(fileContent)
 *      filename : "photo.png",
 *
 *      URL      : "http://...", // download from CDN
 *      // before fileContent uploaded to a public CDN,
 *      // it should be encrypted by a symmetric key
 *      key      : {             // symmetric key to decrypt file content
 *          algorithm : "AES",   // "DES", ...
 *          data      : "{BASE64_ENCODE}",
 *          ...
 *      }
 *  }
 */
public class BaseFileContent extends BaseContent implements FileContent {

    // file content (not encrypted)
    private TransportableData attachment;

    // download from CDN
    private URI remoteURL;
    // key to decrypt data downloaded from CDN
    private DecryptKey password;

    public BaseFileContent(Map<String, Object> content) {
        super(content);
        // lazy load
        attachment = null;
        remoteURL = null;
        password = null;
    }

    public BaseFileContent(int type, byte[] data, String filename, URI url, DecryptKey key) {
        super(type);
        //
        //  file data
        //
        if (data == null) {
            attachment = null;
        } else {
            setData(data);
        }
        //
        //  filename
        //
        if (filename != null) {
            put("filename", filename);
        }
        //
        //  remote URL
        //
        if (url == null) {
            remoteURL = null;
        } else {
            setURL(url);
        }
        //
        //  decrypt key
        //
        if (key == null) {
            password = null;
        } else {
            setPassword(key);
        }
    }
    public BaseFileContent(ContentType type, byte[] data, String filename, URI url, DecryptKey key) {
        this(type.value, data, filename, url, key);
    }

    @Override
    public void setData(byte[] data) {
        TransportableData ted;
        if (data == null/* || data.length == 0*/) {
            ted = null;
            remove("data");
        } else {
            ted = TransportableData.create(data);
            put("data", ted.toObject());
        }
        attachment = ted;
    }

    @Override
    public byte[] getData() {
        TransportableData ted = attachment;
        if (ted == null) {
            Object base64 = get("data");
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
    public void setURL(URI url) {
        if (url == null) {
            remove("URL");
        } else {
            put("URL", url.toString());
        }
        remoteURL = url;
    }

    @Override
    public URI getURL() {
        URI url = remoteURL;
        if (url == null) {
            String remote = getString("URL", null);
            if (remote != null/* && remote.length() > 0*/) {
                remoteURL = url = URI.create(remote);
            }
        }
        return url;
    }

    @Override
    public void setPassword(DecryptKey key) {
        setMap("key", key);
        password = key;
    }

    @Override
    public DecryptKey getPassword() {
        if (password == null) {
            password = SymmetricKey.parse(get("key"));
        }
        return password;
    }
}
