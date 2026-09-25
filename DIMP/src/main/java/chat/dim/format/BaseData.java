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

import java.util.Arrays;

import chat.dim.protocol.TransportableData;
import chat.dim.type.Stringer;


/**
 *  Base Transportable Data
 *  <p>
 *  Base class for transportable data.
 *  </p>
 *  <p>
 *  Holds both an encoded string representation (e.g. base64 string)
 *  and the decoded binary bytes; the missing side is lazy loaded.
 *  </p>
 *  <p>
 *  The {@link Stringer} / {@link CharSequence} delegation lives in the
 *  superclass {@link BaseString}.
 *  </p>
 *  <p>
 *  Semantics: the encoded string is a lossless encoding of the decoded
 *  bytes, so they are one-to-one — equal (non-empty) strings imply equal
 *  bytes, and vice versa.
 *  </p>
 */
public abstract class BaseData extends BaseString implements TransportableData {

    // decoded bytes
    protected byte[] binary;

    /**
     *  Create data with encoded string (decoded bytes lazy loaded).
     *
     *  @param str  encoded string
     */
    protected BaseData(String str) {
        super(str);
        assert str != null : "encoded string should not be null";
        // lazy load
        binary = null;
    }

    /**
     *  Create data with decoded bytes (encoded string lazy loaded).
     *
     *  @param bytes  decoded bytes
     */
    protected BaseData(byte[] bytes) {
        super(null);
        assert bytes != null : "decoded data should not be null";
        binary = bytes;
        // lazy load
        string = null;
    }

    //
    //  TransportableResource
    //

    @Override
    public Object serialize() {
        return toString();
    }

    //
    //  Stringer
    //

    @Override
    public int length() {
        /*/
        String str = toString();
        assert !str.isEmpty() : "transportable data error";
        return str.length();
        /*/
        byte[] bytes = getBytes();
        if (bytes == null) {
            assert false : "transportable data error";
            return 0;
        }
        assert bytes.length > 0 : "transportable data empty";
        return bytes.length;
    }

    @Override
    public boolean isEmpty() {
        // 1. check inner bytes
        byte[] bytes = binary;
        if (bytes != null && bytes.length > 0) {
            return false;
        }
        // 2. check inner string
        String str = string;
        return str == null || str.isEmpty();
    }

    @Override
    public boolean equals(Object other) {
        if (other == null) {
            //return isEmpty();
            return false;
        } else if (this == other) {
            // same object
            return true;
        } else if (other instanceof BaseData) {
            BaseData o = (BaseData) other;
            if (o.isEmpty()) {
                return isEmpty();
            }
            // compare as base data
            return dataEquals(o);
        } else if (other instanceof TransportableData) {
            TransportableData o = (TransportableData) other;
            if (o.isEmpty()) {
                return isEmpty();
            }
            // compare as ted
            return tedEquals(o);
        } else if (other instanceof Stringer) {
            Stringer o = (Stringer) other;
            if (o.isEmpty()) {
                return isEmpty();
            }
            // compare with encoded string
            return toString().equals(o.toString());
        } else if (other instanceof String) {
            String s = (String) other;
            if (s.isEmpty()) {
                return isEmpty();
            }
            // compare with encoded string
            return toString().equals(s);
        }
        assert false : "unknown data: " + other;
        return false;
    }

    //
    //  Compare inner string first, then inner bytes, then decoded bytes.
    //
    //  The encoded string and the decoded bytes are one-to-one: the string
    //  is a lossless encoding of the bytes, so when both inner strings are
    //  non-empty, string equality implies byte equality (and vice versa);
    //  this also keeps the hashCode()/equals() contract valid.
    //
    protected boolean dataEquals(BaseData other) {
        assert !other.isEmpty() : "base data error: " + other;
        // compare with inner string
        String thisString = string;
        String thatString = other.string;
        if (thisString != null && !thisString.isEmpty()
                && thatString != null && !thatString.isEmpty()) {
            return thisString.equals(thatString);
        }
        // compare with inner bytes
        byte[] thisBytes = binary;
        byte[] thatBytes = other.binary;
        if (thisBytes != null && thatBytes != null) {
            return Arrays.equals(thisBytes, thatBytes);
        }
        // compare with decoded bytes
        thisBytes = getBytes();
        thatBytes = other.getBytes();
        return Arrays.equals(thisBytes, thatBytes);
    }

    //
    //  Compare encoded string first, otherwise decoded bytes.
    //
    protected boolean tedEquals(TransportableData other) {
        assert !other.isEmpty() : "base data error: " + other;
        // compare with encoded string
        String thisString = string;
        if (thisString != null && !thisString.isEmpty()) {
            return thisString.equals(other.toString());
        }
        // compare with decoded bytes
        return Arrays.equals(binary, other.getBytes());
    }

    //
    //  Object
    //

    @Override
    public int hashCode() {
        byte[] bytes = getBytes();
        return Arrays.hashCode(bytes);
    }

    /**
     *  Subclasses must implement {@code toString()} to return the encoded
     *  string representation (same as Dart's {@code UnimplementedError}).
     */
    @Override
    public String toString() {
        throw new UnsupportedOperationException("BaseData subclass must implement toString()");
    }

}
