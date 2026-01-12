/* license: https://mit-license.org
 * ==============================================================================
 * The MIT License (MIT)
 *
 * Copyright (c) 2026 Albert Moky
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
package chat.dim.rfc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import chat.dim.format.DataCoder;
import chat.dim.format.SharedNetworkFormatAccess;
import chat.dim.format.StringCoder;


/**
 *  RFC 2397
 *  ~~~~~~~~
 *  https://www.rfc-editor.org/rfc/rfc2397
 *
 *      data:[<mime type>][;charset=<charset>][;<encoding>],<encoded data>
 */
public class DataURI {

    private final String mimeType;  // default is "text/plain"
    private final String charset;   // default is "us-ascii"
    private final String encoding;  // default is URL Escaped Encoding (RFC 2396)
    private final String body;      // encoded data

    private byte[] binary = null;   // decoded data
    private String plain = null;    // plaintext
    private String uri = null;      // full URI

    public DataURI(String mimeType, String charset, String encoding, String body) {
        super();
        this.mimeType = mimeType;
        this.charset = charset;
        this.encoding = encoding;
        this.body = body;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> info = new HashMap<>();
        if (mimeType != null) {
            info.put("mime-type", mimeType);
        }
        if (charset != null) {
            info.put("charset", charset);
        }
        if (encoding != null) {
            info.put("encoding", encoding);
        }
        assert body != null : "data empty";
        info.put("data", body);
        return info;
    }

    @Override
    public String toString() {
        String text = uri;
        if (text == null) {
            List<String> headers = new ArrayList<>();
            // set 'encoding' & 'charset'
            if (encoding != null) {
                headers.add(encoding);
            }
            if (charset != null) {
                headers.add("charset=" + charset);
            }
            // set 'mime-type'
            if (headers.isEmpty()) {
                if (mimeType != null) {
                    headers.add(mimeType);
                }
            } else if (mimeType != null) {
                headers.add(mimeType);
            } else {
                headers.add("text/plain");
                //assert false : "mime-type should not be empty here: " + toMap();
            }
            // build URI
            assert body != null : "data empty";
            if (headers.isEmpty()) {
                text = "data:," + body;
            } else {
                Collections.reverse(headers);
                String head = String.join(";", headers);
                text = "data:" + head + "," + body;
            }
            uri = text;
        }
        return text;
    }

    /**
     *  Decode data body
     *
     * @return decoded data
     */
    public byte[] getData() {
        byte[] bin = binary;
        if (bin == null) {
            String enc = body;
            if (enc == null || enc.isEmpty()) {
                assert false : "data empty";
                return null;
            }
            DataCoder coder = SharedNetworkFormatAccess.getDataCoder(encoding);
            if (coder != null) {
                bin = coder.decode(enc);
                binary = bin;
            } else {
                assert false : "encoding not supported: " + encoding;
            }
        }
        return bin;
    }

    /**
     *  Decode text body
     *
     * @return decoded text
     */
    public String getText() {
        String text = plain;
        if (text == null) {
            byte[] bin = getData();
            if (bin == null/* || bin.length == 0*/) {
                assert false : "failed to decode data: " + body + ", encoding: " + encoding;
                return null;
            }
            StringCoder coder = SharedNetworkFormatAccess.getStringCoder(charset);
            if (coder != null) {
                text = coder.decode(bin);
                plain = text;
            } else {
                assert false : "charset not supported: " + charset;
            }
        }
        return text;
    }

    /**
     *  Split text string for data URI
     *
     *      0. "data:,A%20brief%20note"
     *      1. "data:text/html,<p>Hello, World!</p>"
     *      2. "data:text/plain;charset=iso-8859-7,%be%fg%be"
     *      3. "data:image/png;base64,{BASE64_ENCODED}"
     */
    public static DataURI parse(final String uri) {
        if (uri == null || uri.isEmpty()) {
            return null;
        } else if (!uri.startsWith("data:")) {
            return null;
        }
        int pos = uri.indexOf(',');
        if (pos < 0) {
            assert false : "data URI error: " + uri;
            return null;
        }
        String body = uri.substring(pos + 1);
        //
        //  parse header
        //
        String mimeType = null;
        String charset = null;
        String encoding = null;
        if (pos > 5) {
            // skip 'data:'
            String head = uri.substring(5, pos);
            String[] headers = head.split(";");
            if (headers.length == 1) {
                // 1. "data:text/html,<p>Hello, World!</p>"
                mimeType = headers[0];
            } else if (headers.length == 2) {
                // 2. "data:text/plain;charset=iso-8859-7,%be%fg%be"
                // 3. "data:image/png;base64,{BASE64_ENCODE}"
                mimeType = headers[0];
                if (headers[1].startsWith("charset=")) {
                    charset = headers[1].substring(8);
                } else if (headers[1].indexOf('=') < 0) {
                    encoding = headers[1];
                } else {
                    assert false : "URI error: " + uri;
                }
            } else {
                assert headers.length == 3 : "URI error: " + uri;
                // data:[<mime type>][;charset=<charset>][;<encoding>],<encoded data>
                mimeType = headers[0];
                if (headers[1].startsWith("charset=")) {
                    charset = headers[1].substring(8);
                } else {
                    assert headers[1].isEmpty() : "URI charset error: " + uri;
                }
                encoding = headers[headers.length - 1];  // [2]
            }
        }
        // OK
        return new DataURI(mimeType, charset, encoding, body);
    }

}
