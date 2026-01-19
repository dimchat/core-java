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


/**
 *  UTF-8 encoding
 */
public class PlainData extends BaseData {

    public PlainData(String str) {
        super(str);
    }

    public PlainData(byte[] bytes) {
        super(bytes);
    }

    //
    //  TransportableData
    //

    @Override
    public String getEncoding() {
        return null;
    }

    @Override
    public byte[] getBytes() {
        byte[] bytes = binary;
        if (bytes == null) {
            assert string != null : "base64 data error";
            bytes = UTF8.encode(string);
            binary = bytes;
        }
        return bytes;
    }

    @Override
    public String toString() {
        String base64 = string;
        if (base64 == null) {
            assert binary != null : "base64 data error";
            base64 = UTF8.decode(binary);
            string = base64;
        }
        return base64;
    }

    //
    //  Factory methods
    //

    public static PlainData create(byte[] data) {
        return new PlainData(data);
    }

    public static PlainData create(String text) {
        return new PlainData(text);
    }

}
