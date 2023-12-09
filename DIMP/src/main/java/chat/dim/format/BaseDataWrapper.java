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

import chat.dim.type.Dictionary;

/**
 *  Transportable Data MixIn: {
 *
 *      algorithm : "base64",
 *      data      : "...",     // base64_encode(data)
 *      ...
 *  }
 *
 *  data format:
 *      0. "{BASE64_ENCODE}"
 *      1. "base64,{BASE64_ENCODE}"
 *      2. "data:image/png;base64,{BASE64_ENCODE}"
 */
public class BaseDataWrapper extends Dictionary {

    // binary data
    private byte[] data;

    public BaseDataWrapper(Map<String, Object> content) {
        super(content);
        // lazy load
        data = null;
    }

    @Override
    public boolean isEmpty() {
        if (super.isEmpty()) {
            return true;
        }
        byte[] binary = getData();
        return binary == null || binary.length == 0;
    }

    @Override
    public String toString() {
        String encoded = getString("data", "");
        if (encoded.isEmpty()) {
            return encoded;
        }
        String algorithm = getString("algorithm", "");
        if (algorithm.equals(TransportableData.DEFAULT)) {
            algorithm = "";
        }
        if (algorithm.isEmpty()) {
            // 0. "{BASE64_ENCODE}"
            return encoded;
        } else {
            // 1. "base64,{BASE64_ENCODE}"
            return algorithm + "," + encoded;
        }
    }

    /**
     *  Encode with 'Content-Type'
     */
    public String toString(String mimeType) {
        assert !mimeType.contains(" ") : "content-type error: " + mimeType;
        // get encoded data
        String encoded = getString("data", "");
        if (encoded.isEmpty()) {
            return encoded;
        }
        String algorithm = getAlgorithm();
        // 2. "data:image/png;base64,{BASE64_ENCODE}"
        return "data:" + mimeType + ";" + algorithm + "," + encoded;
    }

    /**
     *  encode algorithm
     */

    public String getAlgorithm() {
        String algorithm = getString("algorithm", "");
        if (algorithm.isEmpty()) {
            algorithm = TransportableData.DEFAULT;
        }
        return algorithm;
    }

    public void setAlgorithm(String algorithm) {
        if (algorithm == null/* || algorithm.equals(TransportableData.DEFAULT)*/) {
            remove("algorithm");
        } else {
            put("algorithm", algorithm);
        }
    }

    /**
     *  binary data
     */

    public byte[] getData() {
        byte[] bin = data;
        if (bin == null) {
            String encoded = getString("data", "");
            if (!encoded.isEmpty()) {
                String algorithm = getAlgorithm();
                switch (algorithm) {
                    case TransportableData.BASE_64:
                        data = bin = Base64.decode(encoded);
                        break;
                    case TransportableData.BASE_58:
                        data = bin = Base58.decode(encoded);
                        break;
                    case TransportableData.HEX:
                        data = bin = Hex.decode(encoded);
                        break;
                    default:
                        assert false : "data algorithm not support: " + algorithm;
                        break;
                }
            }
        }
        return bin;
    }

    public void setData(byte[] binary) {
        if (binary == null || binary.length == 0) {
            remove("data");
        } else {
            String encoded = "";
            String algorithm = getAlgorithm();
            switch (algorithm) {
                case TransportableData.BASE_64:
                    encoded = Base64.encode(binary);
                    break;
                case TransportableData.BASE_58:
                    encoded = Base58.encode(binary);
                    break;
                case TransportableData.HEX:
                    encoded = Hex.encode(binary);
                    break;
                default:
                    assert false : "data algorithm not support: " + algorithm;
                    break;
            }
            put("data", encoded);
        }
        data = binary;
    }

}
