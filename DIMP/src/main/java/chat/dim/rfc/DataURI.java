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
import java.util.List;


/**
 *  RFC 2397
 *  ~~~~~~~~
 *  https://www.rfc-editor.org/rfc/rfc2397
 *
 *      data:[<mime type>][;charset=<charset>][;<encoding>],<encoded data>
 */
public class DataURI {

    public final String mimeType;  // default is "text/plain"
    public final String charset;   // default is "us-ascii"
    public final String encoding;  // default is URL Escaped Encoding (RFC 2396)
    public final String body;      // encoded data

    private String uriString;      // full URI

    public DataURI(String mimeType, String charset, String encoding, String body) {
        super();
        this.mimeType = mimeType;
        this.charset = charset;
        this.encoding = encoding;
        this.body = body;
        // lazy load
        uriString = null;
    }

    public boolean isEmpty() {
        String encoded = body;
        return encoded == null || encoded.isEmpty();
    }

    @Override
    public String toString() {
        String text = uriString;
        if (text == null) {
            List<String> headers = new ArrayList<>();
            // 1. 'mime-type'
            if (mimeType != null) {
                headers.add(mimeType);
            } else if (charset != null || encoding != null) {
                // make sure 'mime-type' is the first header
                headers.add(MIME.ContentType.TEXT_PLAIN);
            }
            // 2. 'charset'
            if (charset != null) {
                headers.add("charset=" + charset);
            }
            // 3. 'encoding'
            if (encoding != null) {
                headers.add(encoding);
            }
            // build URI
            assert body != null : "data empty";
            if (headers.isEmpty()) {
                text = "data:," + body;
            } else {
                String head = String.join(";", headers);
                text = "data:" + head + "," + body;
            }
            uriString = text;
        }
        return text;
    }

    /**
     *  Split text string for data URI
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
        final String body = uri.substring(pos + 1);
        assert !body.isEmpty() : "data URI body empty: " + uri;
        //
        //  parse header
        //
        String mimeType = null;
        String charset = null;
        String encoding = null;
        if (pos > 5) {
            // skip 'data:'
            final String head = uri.substring(5, pos);
            final String[] headers = head.split(";");
            for (final String item : headers) {
                // samples:
                //    "data:,A%20simple%20text"
                //    "data:text/html,<p>Hello, World!</p>"
                //    "data:text/plain;charset=iso-8859-7,%be%fg%be"
                //    "data:image/png;base64,{BASE64_ENCODE}"
                //    "data:text/plain;charset=utf-8;base64,SGVsbG8sIHdvcmxkIQ=="
                if (item.length() == 0) {
                    assert false : "header error: " + uri;
                } else if (item.indexOf('=') > 0) {
                    // 2. 'charset'
                    if (item.toLowerCase().startsWith("charset=")) {
                        assert charset == null : "duplicated charset: " + uri;
                        charset = item.substring(8);
                    } else {
                        assert item.startsWith("filename=") : "unknown header: " + item + ", " + uri;
                    }
                } else if (item.indexOf('/') > 0) {
                    // 1. 'mime-type'
                    assert mimeType == null : "duplicate mime-type: " + uri;
                    mimeType = item;
                } else {
                    // 3. 'encoding'
                    assert encoding == null : "header error: " + uri;
                    encoding = item;
                }
            }
        }
        // OK
        return new DataURI(mimeType, charset, encoding, body);
    }

}
