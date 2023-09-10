/* license: https://mit-license.org
 *
 *  DIMP : Decentralized Instant Messaging Protocol
 *
 *                                Written in 2022 by Moky <albert.moky@gmail.com>
 *
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
package chat.dim.mkm;

import java.util.List;

import chat.dim.crypto.DecryptKey;
import chat.dim.crypto.EncryptKey;
import chat.dim.crypto.SignKey;
import chat.dim.crypto.VerifyKey;
import chat.dim.protocol.Document;
import chat.dim.protocol.ID;
import chat.dim.protocol.Visa;

public class BaseUser extends BaseEntity implements User {

    public BaseUser(ID identifier) {
        super(identifier);
    }

    @Override
    public User.DataSource getDataSource() {
        return (User.DataSource) super.getDataSource();
    }

    @Override
    public Visa getVisa() {
        Document doc = getDocument(Document.VISA);
        if (doc instanceof Visa/* && doc.isValid()*/) {
            return (Visa) doc;
        }
        return null;
    }

    @Override
    public List<ID> getContacts() {
        User.DataSource barrack = getDataSource();
        assert barrack != null : "user delegate not set yet";
        return barrack.getContacts(identifier);
    }

    @Override
    public boolean verify(byte[] data, byte[] signature) {
        User.DataSource barrack = getDataSource();
        assert barrack != null : "user delegate not set yet";
        // NOTICE: I suggest using the private key paired with meta.key to sign message
        //         so here should return the meta.key
        List<VerifyKey> keys = barrack.getPublicKeysForVerification(identifier);
        assert keys.size() > 0 : "failed to get verify keys: " + identifier;
        for (VerifyKey key : keys) {
            if (key.verify(data, signature)) {
                // matched!
                return true;
            }
        }
        // signature not match
        // TODO: check whether visa is expired, query new document for this contact
        return false;
    }

    @Override
    public byte[] encrypt(byte[] plaintext) {
        User.DataSource barrack = getDataSource();
        assert barrack != null : "user delegate not set yet";
        // NOTICE: meta.key will never changed, so use visa.key to encrypt message
        //         is a better way
        EncryptKey pKey = barrack.getPublicKeyForEncryption(identifier);
        assert pKey != null : "failed to get encrypt key for user: " + identifier;
        return pKey.encrypt(plaintext, null);
    }

    //
    //  Interfaces for Local User
    //

    @Override
    public byte[] sign(byte[] data) {
        User.DataSource barrack = getDataSource();
        assert barrack != null : "user delegate not set yet";
        // NOTICE: I suggest use the private key which paired to visa.key
        //         to sign message
        SignKey sKey = barrack.getPrivateKeyForSignature(identifier);
        assert sKey != null : "failed to get sign key for user: " + identifier;
        return sKey.sign(data);
    }

    @Override
    public byte[] decrypt(byte[] ciphertext) {
        User.DataSource barrack = getDataSource();
        assert barrack != null : "user delegate not set yet";
        // NOTICE: if you provide a public key in visa for encryption,
        //         here you should return the private key paired with visa.key
        List<DecryptKey> keys = barrack.getPrivateKeysForDecryption(identifier);
        assert keys.size() > 0 : "failed to get decrypt keys for user: " + identifier;
        byte[] plaintext;
        for (DecryptKey key : keys) {
            // try decrypting it with each private key
            plaintext = key.decrypt(ciphertext, null);
            if (plaintext != null) {
                // OK!
                return plaintext;
            }
        }
        // decryption failed
        // TODO: check whether my visa key is changed, push new visa to this contact
        return null;
    }

    @Override
    public Visa sign(Visa doc) {
        assert doc.getIdentifier().equals(identifier) : "visa ID not match: " + identifier + ", " + doc.getIdentifier();
        User.DataSource barrack = getDataSource();
        assert barrack != null : "user delegate not set yet";
        // NOTICE: only sign visa with the private key paired with your meta.key
        SignKey sKey = barrack.getPrivateKeyForVisaSignature(identifier);
        assert sKey != null : "failed to get sign key for visa: " + identifier;
        if (doc.sign(sKey) == null) {
            assert false : "failed to sign visa: " + identifier + ", " + doc;
            return null;
        }
        return doc;
    }

    @Override
    public boolean verify(Visa doc) {
        // NOTICE: only verify visa with meta.key
        //         (if meta not exists, user won't be created)
        if (!identifier.equals(doc.getIdentifier())) {
            // visa ID not match
            return false;
        }
        VerifyKey pKey = getMeta().getPublicKey();
        assert pKey != null : "failed to get verify key for visa: " + identifier;
        return doc.verify(pKey);
    }
}
