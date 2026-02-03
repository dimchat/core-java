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
public abstract class EmbedData extends BaseData {

    private DataURI dataUri;
    private final DataURI.Header dataHead;

    protected EmbedData(DataURI uri) {
        super(uri.toString());
        dataUri = uri;
        dataHead = uri.head;
    }

    protected EmbedData(DataURI.Header head, byte[] data) {
        super(data);
        assert data.length > 0 : "decoded data should not be empty";
        dataUri = null;
        dataHead = head;
    }

    protected abstract DataCoder getDataCoder();  // Base64.coder

    // encode
    protected DataURI getDataURI() {
        DataURI uri = dataUri;
        if (uri == null) {
            DataCoder coder = getDataCoder();
            byte[] data = binary;
            if (coder == null || data == null || data.length == 0) {
                assert false : "cannot encode data: " + getEncoding();
                return null;
            }
            String base64 = coder.encode(data);
            assert base64 != null && !base64.isEmpty() : "failed to encode " + data.length + " byte(s)";
            uri = new DataURI(dataHead, base64);
            dataUri = uri;
        }
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
        byte[] data = binary;
        if (data == null) {
            DataURI uri = dataUri;
            if (uri == null || uri.isEmpty()) {
                assert false : "data uri error: " + uri;
                return null;
            }
            DataCoder coder = getDataCoder();
            String base64 = uri.body;
            if (coder == null || base64 == null || base64.isEmpty()) {
                assert false : "cannot decode data: " + getEncoding();
                return null;
            }
            data = coder.decode(base64);
            assert data != null && data.length > 0 : "failed to decode " + base64.length() + " char(s)";
            binary = data;
        }
        return data;
    }

    @Override
    public String toString() {
        String text = string;
        if (text == null) {
            DataURI uri = getDataURI();
            text = uri == null ? "" : uri.toString();
            string = text;
        }
        return text;
    }

    //
    //  factories:
    //
    //      "data:image/jpg;base64,{BASE64_ENCODE}"
    //      "data:audio/mp4;base64,{BASE64_ENCODE}"
    //

    public static TransportableData createImage(byte[] jpeg) {
        return create(MIME.ContentType.IMAGE_JPG, jpeg);
    }

    public static TransportableData createAudio(byte[] mp4) {
        return create(MIME.ContentType.AUDIO_MP4, mp4);
    }

    public static TransportableData create(String mimeType, byte[] data) {
        DataURI.Header head = new DataURI.Header(mimeType, BASE_64, null);
        return new EmbedData(head, data) {
            @Override
            protected DataCoder getDataCoder() {
                return Base64.coder;
            }
        };
    }

}
