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

import chat.dim.type.Stringer;


/**
 *  Base String
 *  <p>
 *  Wrapper class for a plain string, implementing the {@link Stringer} interface.
 *  </p>
 *  <p>
 *  Provides character sequence operations (index, substring, trim, etc.)
 *  by delegating to the inner string value.
 *  </p>
 *  <p>
 *  Contract: if {@code isEmpty()} returns false, {@code toString()} is
 *  guaranteed to return a non-empty string.
 *  </p>
 */
public class BaseString implements Stringer {

    // encoded string
    protected String string;

    /**
     *  Create string wrapper with the encoded string
     *
     *  @param str  encoded string
     */
    public BaseString(String str) {
        super();
        string = str;
    }

    @Override
    public String toString() {
        return string;
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    @Override
    public int length() {
        return toString().length();
    }

    @Override
    public boolean isEmpty() {
        return toString().isEmpty();
    }

    @Override
    public boolean equals(Object other) {
        String s;
        if (other == null) {
            //return isEmpty();
            return false;
        } else if (other instanceof Stringer) {
            if (this == other) {
                // same object
                return true;
            }
            Stringer o = (Stringer) other;
            if (o.isEmpty()) {
                return isEmpty();
            }
            // encoded string
            s = o.toString();
            assert !s.isEmpty() : "base string error: " + other;
        } else if (other instanceof String) {
            s = (String) other;
            if (s.isEmpty()) {
                return isEmpty();
            }
        } else {
            assert false : "unknown string: " + other;
            return false;
        }
        // compare with encoded string
        return toString().equals(s);
    }

    @Override
    public boolean equalsIgnoreCase(Stringer other) {
        if (other == null) {
            //return isEmpty();
            return false;
        } else if (this == other) {
            // same object
            return true;
        } else if (other.isEmpty()) {
            return isEmpty();
        }
        // compare with encoded string
        return toString().equalsIgnoreCase(other.toString());
    }

    @Override
    public boolean equalsIgnoreCase(String s) {
        if (s == null) {
            //return isEmpty();
            return false;
        } else if (s.isEmpty()) {
            return isEmpty();
        }
        // compare with encoded string
        return toString().equalsIgnoreCase(s);
    }

    @Override
    public int compareTo(String s) {
        if (s == null || s.isEmpty()) {
            //return isEmpty() ? 0 : "s".compareTo("");
            return isEmpty() ? 0 : 1;
        //} else if (isEmpty()) {
        //    return "".compareTo(s);
        }
        // compare with encoded string
        return toString().compareTo(s);
    }

    @Override
    public int compareToIgnoreCase(String s) {
        if (s == null || s.isEmpty()) {
            //return isEmpty() ? 0 : "s".compareToIgnoreCase("");
            return isEmpty() ? 0 : 1;
        //} else if (isEmpty()) {
        //    return "".compareToIgnoreCase(s);
        }
        // compare with encoded string
        return toString().compareToIgnoreCase(s);
    }

    @Override
    public int compareToIgnoreCase(Stringer other) {
        if (other == null || other.isEmpty()) {
            //return isEmpty() ? 0 : "s".compareToIgnoreCase("");
            return isEmpty() ? 0 : 1;
        //} else if (isEmpty()) {
        //    return "".compareToIgnoreCase(other.toString());
        } else if (this == other) {
            // same object
            return 0;
        }
        // compare with encoded string
        return toString().compareToIgnoreCase(other.toString());
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

}
