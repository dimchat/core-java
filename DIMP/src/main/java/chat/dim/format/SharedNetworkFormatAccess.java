/* license: https://mit-license.org
 * ==============================================================================
 * The MIT License (MIT)
 *
 * Copyright (c) 2025 Albert Moky
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

import java.util.HashMap;
import java.util.Map;

import chat.dim.protocol.EncodeAlgorithms;


public final class SharedNetworkFormatAccess {

    // wrapper for PNF
    public static TransportableFileWrapper.Factory pnfWrapperFactory = new TransportableFileWrapper.Factory() {
        @Override
        public TransportableFileWrapper createTransportableFileWrapper(Map<String, Object> content) {
            return new PortableNetworkFileWrapper(content);
        }
    };

    //
    //  Coders for encoding
    //
    private static final Map<String, DataCoder> dataCoders = new HashMap<>();
    public static DataCoder getDataCoder(String encoding) {
        if (encoding == null || encoding.isEmpty()) {
            encoding = "*";
        } else {
            encoding = encoding.toLowerCase();
        }
        return dataCoders.get(encoding);
    }
    public static void setDataCoder(String encoding, DataCoder coder) {
        if (encoding != null/* && !encoding.isEmpty()*/) {
            encoding = encoding.toLowerCase();
        }
        dataCoders.put(encoding, coder);
    }

    static {
        //
        //  Base-64
        //
        setDataCoder(EncodeAlgorithms.BASE_64, new DataCoder() {
            @Override
            public String encode(byte[] data) {
                return Base64.encode(data);
            }
            @Override
            public byte[] decode(String string) {
                return Base64.decode(string);
            }
        });

        /*/
        //
        //  Plain (UTF-8)
        //
        setDataCoder("*", new DataCoder() {
            @Override
            public String encode(byte[] data) {
                // This binary data was encoded from a string using UTF-8 encoding:
                //      data = UTF8.encode(string);
                // we need to reverse it to a string here.
                return UTF8.decode(data);
            }
            @Override
            public byte[] decode(String string) {
                // This string was decoded from a binary data using UTF-8 encoding:
                //      data = UTF8.decode(binary)
                // we need to revers it to a binary data here.
                return UTF8.encode(string);
            }
        });
        /*/
    }

}
