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

import chat.dim.data.Converter;
import chat.dim.protocol.DocumentType;
import chat.dim.protocol.EncryptKey;
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
    private PortableNetworkFile image = null;

    public BaseVisa(Map<String, Object> dictionary) {
        super(dictionary);
    }

    public BaseVisa(String data, TransportableData signature) {
        super(DocumentType.VISA, data, signature);
    }

    public BaseVisa() {
        super(DocumentType.VISA);
    }

    @Override
    public String getName() {
        return Converter.getString(getProperty("name"));
    }

    @Override
    public void setName(String nickname) {
        setProperty("name", nickname);
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
        PortableNetworkFile img = image;
        if (img == null) {
            Object uri = getProperty("avatar");
            img = PortableNetworkFile.parse(uri);
            image = img;
        }
        return img;
    }

    @Override
    public void setAvatar(PortableNetworkFile img) {
        if (img == null || img.isEmpty()) {
            setProperty("avatar", null);
        } else {
            setProperty("avatar", img.toObject());
        }
        image = img;
    }

}
