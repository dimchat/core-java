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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 *  RFC 2397
 *  ~~~~~~~~
 *  https://www.rfc-editor.org/rfc/rfc2397
 *
 *      data:[<mime type>][;charset=<charset>][;<encoding>],<encoded data>
 */
public class DataURI {

    public final Header head;  // "mime-type", "charset", "encoding"
    public final String body;  // encoded data

    private String uriString;  // built string

    public DataURI(Header head, String body) {
        super();
        this.head = head;
        this.body = body;
        // lazy load
        uriString = null;
    }

    public boolean isEmpty() {
        String encoded = body;
        return encoded == null || encoded.isEmpty();
    }

    public String getHeader(String name) {
        String value = head.getExtraValue(name);
        if (value != null) {
            // charset
            // filename
            return value;
        } else if ("encoding".equalsIgnoreCase(name)) {
            return head.encoding;
        } else if ("mime-type".equalsIgnoreCase(name)) {
            return head.mimeType;
        } else if ("content-type".equalsIgnoreCase(name)) {
            return head.mimeType;
        } else {
            return null;
        }
    }

    public String getCharset() {
        return head.getExtraValue("charset");
    }

    public String getFilename() {
        return head.getExtraValue("filename");
    }

    @Override
    public String toString() {
        String text = uriString;
        if (text == null) {
            String header = head.toString();
            if (/*header == null || */header.isEmpty()) {
                text = "data:," + body;
            } else {
                text = "data:" + header + "," + body;
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
        Header head = Header.splitHeader(uri, pos);
        String body = uri.substring(pos + 1);
        return new DataURI(head, body);
    }

    /**
     *  Head of data URI
     *  ~~~~~~~~~~~~~~~~
     */
    public static class Header {

        public final String mimeType;  // default is "text/plain"
        public final String encoding;  // default is URL Escaped Encoding (RFC 2396)

        private final Map<String, String> extra;

        private String headerString;   // built string

        public Header(String mimeType, String encoding, Map<String, String> extra) {
            super();
            this.mimeType = mimeType;
            this.encoding = encoding;
            this.extra = extra;
            // lazy load
            headerString = null;
        }

        public Set<String> getExtraKeys() {
            if (extra == null) {
                return null;
            }
            return extra.keySet();
        }

        // charset: default is "us-ascii"
        // filename: "avatar.png"
        public String getExtraValue(String name) {
            if (extra == null) {
                //assert false : "extra info is empty";
                return null;
            } else if (name == null || name.isEmpty()) {
                assert false : "header name should not be empty";
                return null;
            } else {
                name = name.toLowerCase();
            }
            return extra.get(name);
        }

        @Override
        public String toString() {
            String text = headerString;
            if (text == null) {
                List<String> items = new ArrayList<>();
                //
                //  1. 'mime-type'
                //
                if (mimeType != null && !mimeType.isEmpty()) {
                    items.add(mimeType);
                } else if (encoding != null && !encoding.isEmpty()) {
                    // make sure 'mime-type' is the first header
                    items.add(MIME.ContentType.TEXT_PLAIN);
                } else if (extra != null && !extra.isEmpty()) {
                    // make sure 'mime-type' is the first header
                    items.add(MIME.ContentType.TEXT_PLAIN);
                }
                //
                //  2. extra info: 'charset' & 'filename'
                //
                if (extra != null/* && !extra.isEmpty()*/) {
                    for (Map.Entry<String, String> entry : extra.entrySet()) {
                        items.add(entry.getKey() + "=" + entry.getValue());
                    }
                }
                //
                //  3. 'encoding'
                //
                if (encoding != null && !encoding.isEmpty()) {
                    items.add(encoding);
                }
                // build header
                if (items.isEmpty()) {
                    text = "";
                } else {
                    text = String.join(";", items);
                }
                headerString = text;
            }
            return text;
        }

        // samples:
        //    "data:,A%20simple%20text"
        //    "data:text/html,<p>Hello, World!</p>"
        //    "data:text/plain;charset=iso-8859-7,%be%fg%be"
        //    "data:image/png;base64,{BASE64_ENCODE}"
        //    "data:text/plain;charset=utf-8;base64,SGVsbG8sIHdvcmxkIQ=="

        /**
         *  Split headers between 'data:' and first ',' from URI string
         */
        private static Header splitHeader(final String uri, final int end) {
            if (end < 6) {
                // header empty
                return new Header(null, null, null);
            }
            assert end < uri.length() - 1 : "data URI error: " + uri;
            final String[] array = uri.substring(5, end).split(";");
            // split main info
            String mimeType = null;
            String encoding = null;
            // split extra info
            Map<String, String> extra = null;
            int pos;
            String name;
            String value;
            for (final String item : array) {
                if (item.length() == 0) {
                    assert false : "header error: " + uri;
                    continue;
                }
                //
                //  2. extra info: 'charset' or 'filename'
                //
                pos = item.indexOf('=');
                if (pos >= 0) {
                    assert 0 < pos && pos < item.length() - 1 : "header error: " + item;
                    if (extra == null) {
                        extra = new HashMap<>();
                    }
                    name = item.substring(0, pos);
                    name = name.toLowerCase();
                    value = item.substring(pos + 1);
                    extra.put(name, value);
                    continue;
                }
                //
                //  1. 'mime-type'
                //
                pos = item.indexOf('/');
                if (pos >= 0) {
                    assert 0 < pos && pos < item.length() - 1 : "header error: " + item;
                    assert mimeType == null : "duplicate mime-type: " + uri;
                    mimeType = item;
                    continue;
                }
                //
                //  3. 'encoding'
                //
                assert encoding == null : "duplicate encoding: " + uri;
                encoding = item;
            }
            return new Header(mimeType, encoding, extra);
        }

    }

}
