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
import chat.dim.protocol.TransportableData;
import chat.dim.type.ConstantString;
import chat.dim.type.Stringer;


public class Base64Data extends ConstantString implements TransportableData {

    private byte[] data;

    public Base64Data(String str) {
        super(str);
        // lazy load
        data = null;
    }

    public Base64Data(Stringer str) {
        super(str);
        // lazy load
        data = null;
    }

    @Override
    public String getEncoding() {
        return EncodeAlgorithms.BASE_64;
    }

    @Override
    public byte[] getBytes() {
        byte[] binary = data;
        if (binary == null) {
            binary = Base64.decode(super.toString());
            data = binary;
        }
        return binary;
    }

    @Override
    public Object serialize() {
        return toString();
    }

    //
    //  Factory methods
    //

    public static Base64Data create(byte[] data) {
        String base64 = Base64.encode(data);
        Base64Data ted = new Base64Data(base64);
        ted.data = data;
        return ted;
    }

    public static Base64Data parse(String text) {
        return new Base64Data(text);
    }

}
