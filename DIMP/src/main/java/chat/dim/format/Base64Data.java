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

import chat.dim.protocol.EncodeAlgorithms;


/**
 *  Base-64 encoding
 */
public class Base64Data extends BaseData {

    public Base64Data(String base64) {
        super(base64);
        assert !base64.isEmpty() : "base64 string should not be empty";
    }

    public Base64Data(byte[] bytes) {
        super(bytes);
        assert bytes.length > 0 : "decoded data should not be empty";
    }

    //
    //  TransportableData
    //

    @Override
    public String getEncoding() {
        return EncodeAlgorithms.BASE_64;
    }

    @Override
    public byte[] getBytes() {
        byte[] bytes = binary;
        if (bytes == null) {
            assert string != null : "base64 data error";
            bytes = Base64.decode(string);
            binary = bytes;
        }
        return bytes;
    }

    @Override
    public String toString() {
        String base64 = string;
        if (base64 == null) {
            assert binary != null : "base64 data error";
            base64 = Base64.encode(binary);
            string = base64;
        }
        return base64;
    }

    //
    //  Factory methods
    //

    public static Base64Data create(byte[] data) {
        return new Base64Data(data);
    }

    public static Base64Data create(String text) {
        return new Base64Data(text);
    }

}
