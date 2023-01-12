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
package chat.dim.dkd;

import java.util.Map;

import chat.dim.crypto.DecryptKey;
import chat.dim.crypto.SymmetricKey;
import chat.dim.format.Base64;
import chat.dim.protocol.ContentType;
import chat.dim.protocol.FileContent;

/**
 *  File message: {
 *      type : 0x10,
 *      sn   : 123,
 *
 *      URL      : "http://", // upload to CDN
 *      data     : "...",     // if (!URL) base64_encode(fileContent)
 *      filename : "..."
 *  }
 */
public class BaseFileContent extends BaseContent implements FileContent {

    private byte[] data; // file data (plaintext)
    private DecryptKey key; // key to decrypt data

    public BaseFileContent(Map<String, Object> content) {
        super(content);
        // lazy load
        data = null;
        key = null;
    }

    protected BaseFileContent(int type, String filename, String encoded) {
        super(type);
        if (filename != null) {
            put("filename", filename);
        }
        if (encoded != null) {
            put("data", encoded);
        }
        // lazy load
        data = null;
        key = null;
    }
    protected BaseFileContent(int type, String filename, byte[] binary) {
        super(type);
        if (filename != null) {
            put("filename", filename);
        }
        if (binary != null) {
            put("data", Base64.encode(binary));
        }
        data = binary;
        key = null;
    }

    protected BaseFileContent(ContentType type, String filename, String encoded) {
        this(type.value, filename, encoded);
    }
    protected BaseFileContent(ContentType type, String filename, byte[] binary) {
        this(type.value, filename, binary);
    }

    public BaseFileContent(String filename, String encoded) {
        this(ContentType.FILE, filename, encoded);
    }
    public BaseFileContent(String filename, byte[] binary) {
        this(ContentType.FILE, filename, binary);
    }

    @Override
    public void setURL(String urlString) {
        if (urlString == null) {
            remove("URL");
        } else {
            put("URL", urlString);
        }
    }

    @Override
    public String getURL() {
        return getString("URL");
    }

    @Override
    public void setData(byte[] fileData) {
        if (fileData != null && fileData.length > 0) {
            // file data
            put("data", Base64.encode(fileData));
        } else {
            remove("data");
        }
        data = fileData;
    }

    @Override
    public byte[] getData() {
        if (data == null) {
            String base64 = getString("data");
            if (base64 != null) {
                data = Base64.decode(base64);
            }
        }
        return data;
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
        return getString("filename");
    }

    @Override
    public void setPassword(DecryptKey password) {
        setMap("password", password);
        key = password;
    }

    @Override
    public DecryptKey getPassword() {
        if (key == null) {
            Object password = get("password");
            key = SymmetricKey.parse(password);
        }
        return key;
    }
}
