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
import chat.dim.type.Dictionary;

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
public class BaseDataWrapper extends Dictionary {

    // binary data
    private byte[] data;

    public BaseDataWrapper(Map<String, Object> content) {
        super(content);
        // lazy load
        data = null;
    }

    /*/
    @Override
    public boolean isEmpty() {
        if (super.isEmpty()) {
            return true;
        }
        byte[] binary = getData();
        return binary == null || binary.length == 0;
    }
    /*/

    @Override
    public String toString() {
        String text = getString("data");
        if (text == null/* || text.isEmpty()*/) {
            return "";
        }
        String algorithm = getString("algorithm");
        if (algorithm == null || algorithm.equals(EncodeAlgorithms.DEFAULT)) {
            algorithm = "";
        }
        if (algorithm.isEmpty()) {
            // 0. "{BASE64_ENCODE}"
            return text;
        } else {
            // 1. "base64,{BASE64_ENCODE}"
            return algorithm + "," + text;
        }
    }

    /**
     *  Encode with 'Content-Type'
     *  <p>
     *      toString(mimeType)
     *  </p>
     */
    public String encode(String mimeType) {
        assert !mimeType.contains(" ") : "content-type error: " + mimeType;
        // get encoded data
        String text = getString("data");
        if (text == null/* || text.isEmpty()*/) {
            return "";
        }
        String algorithm = getAlgorithm();
        // 2. "data:image/png;base64,{BASE64_ENCODE}"
        return "data:" + mimeType + ";" + algorithm + "," + text;
    }

    /**
     *  Encode Algorithm
     */
    public String getAlgorithm() {
        String algorithm = getString("algorithm");
        if (algorithm == null || algorithm.isEmpty()) {
            algorithm = EncodeAlgorithms.DEFAULT;
        }
        return algorithm;
    }

    public void setAlgorithm(String algorithm) {
        if (algorithm == null/* || algorithm.equals(EncodeAlgorithms.DEFAULT)*/) {
            remove("algorithm");
        } else {
            put("algorithm", algorithm);
        }
    }

    /**
     *  Binary Data
     */
    public byte[] getData() {
        byte[] binary = data;
        if (binary == null) {
            String text = getString("data");
            if (text == null || text.isEmpty()) {
                assert false : "TED data empty: " + this.toMap();
                return null;
            } else {
                String algorithm = getAlgorithm();
                switch (algorithm) {
                    case EncodeAlgorithms.BASE_64:
                        binary = Base64.decode(text);
                        break;
                    case EncodeAlgorithms.BASE_58:
                        binary = Base58.decode(text);
                        break;
                    case EncodeAlgorithms.HEX:
                        binary = Hex.decode(text);
                        break;
                    default:
                        assert false : "data algorithm not support: " + algorithm;
                        return null;
                }
            }
            data = binary;
        }
        return binary;
    }

    public void setData(byte[] binary) {
        if (binary == null || binary.length == 0) {
            remove("data");
        } else {
            String text;
            String algorithm = getAlgorithm();
            switch (algorithm) {
                case EncodeAlgorithms.BASE_64:
                    text = Base64.encode(binary);
                    break;
                case EncodeAlgorithms.BASE_58:
                    text = Base58.encode(binary);
                    break;
                case EncodeAlgorithms.HEX:
                    text = Hex.encode(binary);
                    break;
                default:
                    throw new ArithmeticException("data algorithm not support: " + algorithm);
                    //assert false : "data algorithm not support: " + algorithm;
            }
            put("data", text);
        }
        data = binary;
    }

}
