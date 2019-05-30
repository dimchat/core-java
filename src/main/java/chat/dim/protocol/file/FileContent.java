/* license: https://mit-license.org
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
package chat.dim.protocol.file;

import chat.dim.dkd.Content;
import chat.dim.protocol.ContentType;

import java.util.Map;

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

    private String url;
    private byte[] data; // file data (plaintext)
    private String filename;

    private Map<String, Object> password; // symmetric key to decrypt the encrypted data from URL

    @SuppressWarnings("unchecked")
    public FileContent(Map<String, Object> dictionary) {
        super(dictionary);
        url = (String) dictionary.get("URL");
        data = null; // NOTICE: file data should not exists here
        filename = (String) dictionary.get("filename");
        password = (Map<String, Object>) dictionary.get("password");
    }

    protected FileContent(int type, byte[] data, String filename) {
        super(type);
        setUrl(null);
        setData(data);
        setFilename(filename);
        setPassword(null);
    }

    public FileContent(byte[] data, String filename) {
        this(ContentType.FILE.value, data, filename);
    }

    //-------- setters/getters --------

    public void setUrl(String urlString) {
        url = urlString;
        if (urlString == null) {
            dictionary.remove("URL");
        } else {
            dictionary.put("URL", urlString);
        }
    }

    public String getUrl() {
        return url;
    }

    public void setData(byte[] fileData) {
        data = fileData;
        // NOTICE: do not set file data in dictionary, which will be post onto the DIM network
    }

    public byte[] getData() {
        return data;
    }

    public void setFilename(String name) {
        filename = name;
        if (name == null) {
            dictionary.remove("filename");
        } else {
            dictionary.put("filename", name);
        }
    }

    public String getFilename() {
        return filename;
    }

    public void setPassword(Map<String, Object> key) {
        password = key;
        if (key == null) {
            dictionary.remove("password");
        } else {
            dictionary.put("password", key);
        }
    }

    public Map<String, Object> getPassword() {
        return password;
    }
}