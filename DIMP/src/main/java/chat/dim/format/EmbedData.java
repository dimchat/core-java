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
package chat.dim.format;

import chat.dim.protocol.TransportableData;
import chat.dim.rfc.DataURI;
import chat.dim.rfc.MIME;


/**
 *  Data URI for embed image/audio
 */
public class EmbedData extends BaseData {

    private DataURI dataUri;
    private final DataURI.Header dataHead;

    public EmbedData(DataURI uri) {
        super(uri.toString());
        dataUri = uri;
        dataHead = uri.head;
    }

    public EmbedData(byte[] data, DataURI.Header head) {
        super(data);
        assert data.length > 0 : "decoded data should not be empty";
        dataUri = null;
        dataHead = head;
    }

    //
    //  Uri Headers
    //

    // default is "text/plain"
    public String getMimeType() {
        return dataHead.mimeType;
    }

    // default is "us-ascii"
    public String getCharset() {
        return dataHead.getCharset();
    }

    // "avatar.png"
    public String getFilename() {
        return dataHead.getExtraValue("filename");
    }

    public String getHeader(String name) {
        String value = dataHead.getExtraValue(name);
        if (value != null) {
            // charset
            // filename
            return value;
        } else if ("encoding".equalsIgnoreCase(name)) {
            return dataHead.encoding;
        } else if ("mime-type".equalsIgnoreCase(name)) {
            return dataHead.mimeType;
        } else if ("content-type".equalsIgnoreCase(name)) {
            return dataHead.mimeType;
        } else {
            return null;
        }
    }

    // "data:.../...;base64,..."
    public DataURI getDataURI() {
        DataURI uri = dataUri;
        if (uri != null) {
            return uri;
        }
        // check encoded data uri
        String txt = string;
        if (txt == null || txt.isEmpty()) {
            // encode data to build uri
            byte[] bin = binary;
            if (bin == null/* || bin.length == 0*/) {
                return null;
            }
            assert bin.length > 0 : "embed data empty";
            // encode body
            String base64 = Base64.encode(bin);
            assert base64 != null && !base64.isEmpty() : "failed to encode " + bin.length + " byte(s)";
            // build uri with header
            uri = new DataURI(dataHead, base64);
        } else {
            assert txt.startsWith("data:") : "data uri error: " + txt;
            uri = DataURI.parse(txt);
        }
        dataUri = uri;
        return uri;
    }

    //
    //  TransportableData
    //

    @Override
    public String getEncoding() {
        return dataHead.encoding;  // "base64"
    }

    @Override
    public byte[] getBytes() {
        byte[] bin = binary;
        if (bin == null) {
            DataURI uri = dataUri;
            if (uri != null/* && !uri.isEmpty()*/) {
                String base64 = uri.body;
                if (base64 != null && !base64.isEmpty()) {
                    bin = Base64.decode(base64);
                }
            }
            assert uri != null && !uri.isEmpty() : "failed to decode data uri";
            binary = bin;
        }
        return bin;
    }

    @Override
    public String toString() {
        String text = string;
        if (text == null) {
            DataURI uri = getDataURI();
            if (uri != null/* && !uri.isEmpty()*/) {
                text = uri.toString();
            } else {
                text = "";
                assert false : "failed to encode data uri";
            }
            string = text;
        }
        return text;
    }

    //
    //  Factory methods:
    //
    //      Image URI: "data:image/jpg;base64,{BASE64_ENCODE}"
    //      Audio URI: "data:audio/mp4;base64,{BASE64_ENCODE}"
    //

    public static TransportableData createImage(byte[] jpeg) {
        return create(jpeg, MIME.ContentType.IMAGE_JPG);
    }

    public static TransportableData createAudio(byte[] mp4) {
        return create(mp4, MIME.ContentType.AUDIO_MP4);
    }

    // create with bytes
    public static TransportableData create(byte[] data, String mimeType) {
        DataURI.Header head = new DataURI.Header(mimeType, BASE_64, null);
        return new EmbedData(data, head);
    }

    // create with string
    public static TransportableData create(String dataUri) {
        DataURI uri = DataURI.parse(dataUri);
        if (uri == null) {
            assert false : "data uri error: " + dataUri;
            return null;
        }
        return new EmbedData(uri);
    }

    // create with uri
    public static TransportableData create(DataURI uri) {
        return new EmbedData(uri);
    }

}
