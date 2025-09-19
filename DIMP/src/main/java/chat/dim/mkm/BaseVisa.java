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

import chat.dim.protocol.DocumentType;
import chat.dim.protocol.EncryptKey;
import chat.dim.protocol.ID;
import chat.dim.protocol.PortableNetworkFile;
import chat.dim.protocol.PublicKey;
import chat.dim.protocol.TransportableData;
import chat.dim.protocol.Visa;

/**
 *  Base Document for User
 */
public class BaseVisa extends BaseDocument implements Visa {

    // Public Key for encryption
    // ~~~~~~~~~~~~~~~~~~~~~~~~~
    // For safety considerations, the visa.key which used to encrypt message data
    // should be different with meta.key
    private EncryptKey key = null;

    // Avatar URL
    private PortableNetworkFile avatar = null;

    public BaseVisa(Map<String, Object> dictionary) {
        super(dictionary);
    }

    public BaseVisa(ID identifier, String data, TransportableData signature) {
        super(identifier, DocumentType.VISA, data, signature);
    }

    public BaseVisa(ID identifier) {
        super(identifier, DocumentType.VISA);
    }

    /**
     *  Public key (used for encryption, can be same with meta.key)
     *  <p>
     *      RSA
     *  </p>
     */
    @Override
    public EncryptKey getPublicKey() {
        EncryptKey visaKey = key;
        if (visaKey == null) {
            Object info = getProperty("key");
            // assert info != null : "visa key not found: " + toMap();
            PublicKey pKey = PublicKey.parse(info);
            if (pKey instanceof EncryptKey) {
                visaKey = (EncryptKey) pKey;
                key = visaKey;
            } else {
                assert info == null : "visa key error: " + info;
            }
        }
        return visaKey;
    }

    @Override
    public void setPublicKey(EncryptKey publicKey) {
        setProperty("key", publicKey == null ? null : publicKey.toMap());
        key = publicKey;
    }

    @Override
    public PortableNetworkFile getAvatar() {
        PortableNetworkFile img = avatar;
        if (img == null) {
            Object url = getProperty("avatar");
            boolean empty = url instanceof String && ((String) url).length() == 0;
            if (!empty) {
                img = PortableNetworkFile.parse(url);
                avatar = img;
            }
        }
        return img;
    }

    @Override
    public void setAvatar(PortableNetworkFile img) {
        setProperty("avatar", img == null ? null : img.toObject());
        avatar = img;
    }

}
