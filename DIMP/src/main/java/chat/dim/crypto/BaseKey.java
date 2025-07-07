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

import chat.dim.plugins.GeneralCryptoHelper;
import chat.dim.plugins.SharedCryptoExtensions;
import chat.dim.type.Dictionary;

public abstract class BaseKey extends Dictionary implements CryptographyKey {

    protected BaseKey(Map<String, Object> dictionary) {
        super(dictionary);
    }

    @Override
    public String getAlgorithm() {
        return getKeyAlgorithm(toMap());
    }

    //
    //  Conveniences
    //

    public static String getKeyAlgorithm(Map<?, ?> key) {
        String algorithm = SharedCryptoExtensions.helper.getKeyAlgorithm(key, null);
        return algorithm == null ? "" : algorithm;
    }

    public static boolean matchEncryptKey(EncryptKey pKey, DecryptKey sKey) {
        return GeneralCryptoHelper.matchSymmetricKeys(pKey, sKey);
    }

    public static boolean matchSignKey(SignKey sKey, VerifyKey pKey) {
        return GeneralCryptoHelper.matchAsymmetricKeys(sKey, pKey);
    }

    public static boolean symmetricKeyEquals(SymmetricKey a, SymmetricKey b) {
        if (a == b) {
            // same object
            return true;
        }
        // compare by encryption
        return matchEncryptKey(a, b);
    }

    public static boolean privateKeyEquals(PrivateKey a, PrivateKey b) {
        if (a == b) {
            // same object
            return true;
        }
        // compare by signature
        return matchSignKey(a, b.getPublicKey());
    }

}
