/* license: https://mit-license.org
 *
 *  Ming-Ke-Ming : Decentralized User Identity Authentication
 *
 *                                Written in 2020 by Moky <albert.moky@gmail.com>
 *
 * ==============================================================================
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 Albert Moky
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
package chat.dim.mkm;

import java.util.Map;

import chat.dim.crypto.EncryptKey;
import chat.dim.crypto.PublicKey;
import chat.dim.protocol.ID;
import chat.dim.protocol.Visa;

public class BaseVisa extends BaseDocument implements Visa {

    private EncryptKey key = null;

    public BaseVisa(Map<String, Object> dictionary) {
        super(dictionary);
    }

    public BaseVisa(ID identifier, String data, String signature) {
        super(identifier, data, signature);
    }

    public BaseVisa(ID identifier) {
        super(identifier, VISA);
    }

    /**
     *  Public key (used for encryption, can be same with meta.key)
     *
     *      RSA
     */
    @Override
    public EncryptKey getKey() {
        if (key == null) {
            Object info = getProperty("key");
            PublicKey pKey = PublicKey.parse(info);
            if (pKey instanceof EncryptKey) {
                key = (EncryptKey) pKey;
            }
        }
        return key;
    }

    @Override
    public void setKey(EncryptKey publicKey) {
        setProperty("key", publicKey.toMap());
        key = publicKey;
    }

    @Override
    public String getAvatar() {
        return (String) getProperty("avatar");
    }

    @Override
    public void setAvatar(String url) {
        setProperty("avatar", url);
    }
}
