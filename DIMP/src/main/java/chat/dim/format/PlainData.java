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
import chat.dim.type.ConstantString;
import chat.dim.type.Stringer;


public class PlainData extends ConstantString implements TransportableData {

    private byte[] data;

    public PlainData(String str) {
        super(str);
        // lazy load
        data = null;
    }

    public PlainData(Stringer str) {
        super(str);
        // lazy load
        data = null;
    }

    @Override
    public String getEncoding() {
        return null;
    }

    @Override
    public byte[] getBytes() {
        byte[] binary = data;
        if (binary == null) {
            binary = UTF8.encode(super.toString());
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

    public static PlainData create(byte[] data) {
        String text = UTF8.decode(data);
        PlainData ted = new PlainData(text);
        ted.data = data;
        return ted;
    }

    public static PlainData parse(String text) {
        return new PlainData(text);
    }

}
