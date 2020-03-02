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
import chat.dim.digest.MD5;
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

    protected FileContent(ContentType type, byte[] data, String filename) {
        this(type.value, data, filename);
    }
    protected FileContent(int type, byte[] data, String filename) {
        super(type);
        setURL(null);
        setData(data);
        setFilename(filename);
        setPassword(null);
    }

    public FileContent(byte[] data, String filename) {
        this(ContentType.FILE, data, filename);
    }

    //-------- setters/getters --------

    public void setURL(String urlString) {
        url = urlString;
        if (urlString == null) {
            dictionary.remove("URL");
        } else {
            dictionary.put("URL", urlString);
        }
    }

    public String getURL() {
        return url;
    }

    public String getFileExt() {
        if (filename == null) {
            return null;
        }
        int pos = filename.lastIndexOf('.');
        if (pos < 0) {
            return null;
        }
        return filename.substring(pos + 1);
    }

    private static String hexEncode(byte[] data) {
        StringBuilder sb = new StringBuilder();
        String hex;
        for (byte ch : data) {
            hex = Integer.toHexString(ch & 0xFF);
            sb.append(hex.length() == 1 ? "0" + hex : hex);
        }
        return sb.toString();
    }

    public void setData(byte[] fileData) {
        data = fileData;

        if (fileData != null && fileData.length > 0) {
            byte[] hash = MD5.digest(fileData);
            assert hash != null : "md5 error";
            String filename = hexEncode(hash);
            String ext = getFileExt();
            if (ext != null) {
                filename = filename + "." + ext;
            }
            dictionary.put("filename", filename);

            // file data
            dictionary.put("data", Base64.encode(fileData));
        } else {
            dictionary.remove("data");
        }
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
