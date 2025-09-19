/* license: https://mit-license.org
 * ==============================================================================
 * The MIT License (MIT)
 *
 * Copyright (c) 2022 Albert Moky
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
package chat.dim.crypto;

import java.util.Map;

import chat.dim.protocol.EncryptKey;
import chat.dim.protocol.SymmetricKey;
import chat.dim.type.Comparator;
import chat.dim.type.Dictionary;
import chat.dim.type.Mapper;

public abstract class BaseSymmetricKey extends Dictionary implements SymmetricKey {

    protected BaseSymmetricKey(Map<String, Object> dictionary) {
        super(dictionary);
    }

    @Override
    public boolean equals(Object other) {
        if (other == null) {
            return isEmpty();
        } else if (other instanceof Mapper) {
            if (this == other) {
                // same object
                return true;
            } else if (other instanceof SymmetricKey) {
                return BaseKey.symmetricKeyEquals((SymmetricKey) other, this);
            }
            // compare inner map
            other = ((Mapper) other).toMap();
        }
        return other instanceof Map && Comparator.mapEquals(toMap(), (Map<?, ?>) other);
    }

    @Override
    public String getAlgorithm() {
        return BaseKey.getKeyAlgorithm(toMap());
    }

    @Override
    public boolean matchEncryptKey(EncryptKey pKey) {
        return BaseKey.matchEncryptKey(pKey, this);
    }

}
