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
 */
public abstract class BaseData implements TransportableData {

    protected String string;  // encoded string
    protected byte[] binary;  // decoded bytes

    protected BaseData(String str) {
        super();
        assert str != null : "encoded string should not be null";
        string = str;
        // lazy load
        binary = null;
    }

    protected BaseData(byte[] bytes) {
        super();
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
    public int compareTo(String other) {
        if (other == null || other.isEmpty()) {
            return isEmpty() ? 0 : "s".compareTo("");
        }
        // compare with encoded string
        String str = toString();
        return str.compareTo(other);
    }

    @Override
    public int compareToIgnoreCase(String other) {
        if (other == null || other.isEmpty()) {
            return isEmpty() ? 0 : "s".compareToIgnoreCase("");
        }
        // compare with encoded string
        String str = toString();
        return str.compareToIgnoreCase(other);
    }

    @Override
    public int compareToIgnoreCase(Stringer other) {
        if (other == null || other.isEmpty()) {
            return isEmpty() ? 0 : "s".compareToIgnoreCase("");
        }
        // compare with encoded string
        String str = toString();
        return str.compareToIgnoreCase(other.toString());
    }

    @Override
    public boolean equalsIgnoreCase(String other) {
        if (other == null || other.isEmpty()) {
            return isEmpty();
        }
        // compare with encoded string
        String str = toString();
        return str.equalsIgnoreCase(other);
    }

    @Override
    public boolean equalsIgnoreCase(Stringer other) {
        if (other == null || other.isEmpty()) {
            return isEmpty();
        }
        // compare with encoded string
        String str = toString();
        return str.equalsIgnoreCase(other.toString());
    }

    @Override
    public boolean equals(Object other) {
        if (other == null) {
            return false;
        } else if (other instanceof BaseData) {
            if (this == other) {
                // same object
                return true;
            }
            // compare as base data
            return dataEquals((BaseData) other);
        } else if (other instanceof TransportableData) {
            // compare as ted
            return tedEquals((TransportableData) other);
        } else if (other instanceof Stringer) {
            // compare with inner string
            Stringer otherString = (Stringer) other;
            return otherString.toString().equals(toString());
        } else if (other instanceof String) {
            // compare with inner string
            String otherString = (String) other;
            return otherString.equals(toString());
        }
        assert false : "unknown data: " + other;
        return false;
    }

    //
    //  Compare inner string first, then inner bytes, then decoded bytes.
    //
    protected boolean dataEquals(BaseData other) {
        if (other == null || other.isEmpty()) {
            return isEmpty();
        }
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
    private boolean tedEquals(TransportableData other) {
        if (other == null || other.isEmpty()) {
            return isEmpty();
        }
        // compare with encoded string
        String thisString = string;
        if (thisString != null && !thisString.isEmpty()) {
            return thisString.equals(other.toString());
        }
        // compare with decoded bytes
        return Arrays.equals(binary, other.getBytes());
    }

    //
    //  CharSequence
    //

    @Override
    public char charAt(int index) {
        String str = toString();
        return str.charAt(index);
    }

    @Override
    public CharSequence subSequence(int start, int end) {
        String str = toString();
        return str.subSequence(start, end);
    }

    //
    //  Object
    //

    @Override
    public int hashCode() {
        byte[] bytes = getBytes();
        return Arrays.hashCode(bytes);
    }

}
