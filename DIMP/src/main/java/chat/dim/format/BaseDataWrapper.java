/* license: https://mit-license.org
 * ==============================================================================
 * The MIT License (MIT)
 *
 * Copyright (c) 2023 Albert Moky
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

import java.util.Map;

import chat.dim.protocol.EncodeAlgorithms;

/**
 *  Transportable Data MixIn
 *
 *  <blockquote><pre>
 *  {
 *      "algorithm" : "base64",
 *      "data"      : "...",     // base64_encode(data)
 *      ...
 *  }
 *
 *  data format:
 *      0. "{BASE64_ENCODE}"
 *      1. "base64,{BASE64_ENCODE}"
 *      2. "data:image/png;base64,{BASE64_ENCODE}"
 *  </pre></blockquote>
 */
class BaseDataWrapper extends BaseNetworkFormatWrapper implements TransportableDataWrapper {

    // binary data
    private byte[] data;

    public BaseDataWrapper(Map<String, Object> map) {
        super(map);
        // lazy load
        data = null;
    }

    @Override
    public boolean isEmpty() {
        byte[] binary = data;
        if (binary != null && binary.length > 0) {
            return false;
        }
        String text = getString("data");
        return text == null || text.isEmpty();
    }

    @Override
    public String toString() {
        // serialize data
        return encode();
    }

    @Override
    public String encode() {
        // get encoded data
        String text = getEncodedData();
        if (text == null || text.isEmpty()) {
            return "";
        }
        String algorithm = getString("algorithm");
        if (algorithm == null || algorithm.equals(EncodeAlgorithms.DEFAULT)) {
            algorithm = "";
        }
        if (algorithm.isEmpty()) {
            // 0. "{BASE64_ENCODE}"
            return text;
        }
        String mimeType = getString("mime-type");
        if (mimeType == null || mimeType.isEmpty()) {
            // 1. "base64,{BASE64_ENCODE}"
            return algorithm + "," + text;
        }
        // 2. "data:image/png;base64,{BASE64_ENCODE}"
        return "data:" + mimeType + ";" + algorithm + "," + text;
    }

    @Override
    public String encode(String mimeType) {
        assert !mimeType.contains(" ") : "mime-type error: " + mimeType;
        // get encoded data
        String text = getEncodedData();
        if (text == null || text.isEmpty()) {
            return "";
        }
        String algorithm = getAlgorithm();
        // 2. "data:image/png;base64,{BASE64_ENCODE}"
        return "data:" + mimeType + ";" + algorithm + "," + text;
    }

    @Override
    public String getAlgorithm() {
        String algorithm = getString("algorithm");
        if (algorithm == null || algorithm.isEmpty()) {
            algorithm = EncodeAlgorithms.DEFAULT;
        }
        return algorithm;
    }

    @Override
    public void setAlgorithm(String algorithm) {
        if (algorithm == null/* || algorithm.equals(EncodeAlgorithms.DEFAULT)*/) {
            remove("algorithm");
        } else {
            put("algorithm", algorithm);
        }
    }

    @Override
    public byte[] getData() {
        byte[] binary = data;
        if (binary == null) {
            String text = getString("data");
            if (text == null || text.isEmpty()) {
                assert false : "TED data empty: " + toMap();
                return null;
            }
            binary = decodeData(text, getAlgorithm());
            data = binary;
        }
        return binary;
    }

    @Override
    public void setData(byte[] binary) {
        remove("data");
        /*/
        if (binary != null && binary.length > 0) {
            String text = encodeData(binary, getAlgorithm());
            put("data", text);
        }
        /*/
        data = binary;
    }

    // decode data
    protected byte[] decodeData(String text, String algorithm) {
        switch (algorithm) {

            case EncodeAlgorithms.BASE_64:
                return Base64.decode(text);

            case EncodeAlgorithms.BASE_58:
                return Base58.decode(text);

            case EncodeAlgorithms.HEX:
                return Hex.decode(text);

            default:
                assert false : "data algorithm not support: " + algorithm;
        }
        return null;
    }

    // encode data
    protected String encodeData(byte[] binary, String algorithm) {
        switch (algorithm) {

            case EncodeAlgorithms.BASE_64:
                return Base64.encode(binary);

            case EncodeAlgorithms.BASE_58:
                return Base58.encode(binary);

            case EncodeAlgorithms.HEX:
                return Hex.encode(binary);
            default:
                throw new ArithmeticException("data algorithm not support: " + algorithm);
                //assert false : "data algorithm not support: " + algorithm;
        }
        //return null;
    }

    // get encoded data
    protected String getEncodedData() {
        String text = getString("data");
        if (text == null || text.isEmpty()) {
            byte[] binary = data;
            if (binary == null || binary.length == 0) {
                return null;
            }
            text = encodeData(binary, getAlgorithm());
            assert text != null : "failed to encode data: " + binary.length;
            put("data", text);
        }
        return text;
    }

}
