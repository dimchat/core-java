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

import java.util.Map;

import chat.dim.Content;
import chat.dim.format.Base64;

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
public class FileContent extends Content {

    private byte[] data; // file data (plaintext)

    @SuppressWarnings("unchecked")
    public FileContent(Map<String, Object> dictionary) {
        super(dictionary);
        data = null;
    }

    protected FileContent(ContentType type, byte[] data, String filename) {
        this(type.value, data, filename);
    }
    protected FileContent(int type, byte[] data, String filename) {
        super(type);
        setURL(null);
        setFilename(filename);
        setData(data);
        setPassword(null);
    }

    public FileContent(byte[] data, String filename) {
        this(ContentType.FILE, data, filename);
    }

    //-------- setters/getters --------

    public void setURL(String urlString) {
        if (urlString == null) {
            remove("URL");
        } else {
            put("URL", urlString);
        }
    }

    public String getURL() {
        return (String) get("URL");
    }

    public void setData(byte[] fileData) {
        data = fileData;

        if (fileData != null && fileData.length > 0) {
            // file data
            put("data", Base64.encode(fileData));
        } else {
            remove("data");
        }
    }

    public byte[] getData() {
        if (data == null) {
            String base64 = (String) get("data");
            if (base64 != null) {
                data = Base64.decode(base64);
            }
        }
        return data;
    }

    public void setFilename(String name) {
        if (name == null) {
            remove("filename");
        } else {
            put("filename", name);
        }
    }

    public String getFilename() {
        return (String) get("filename");
    }

    // symmetric key to decrypt the encrypted data from URL
    public void setPassword(Map<String, Object> key) {
        if (key == null) {
            remove("password");
        } else {
            put("password", key);
        }
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getPassword() {
        return (Map<String, Object>) get("password");
    }
}
