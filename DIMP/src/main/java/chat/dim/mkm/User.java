/* license: https://mit-license.org
 *
 *  DIMP : Decentralized Instant Messaging Protocol
 *
 *                                Written in 2019 by Moky <albert.moky@gmail.com>
 *
 * ==============================================================================
 * The MIT License (MIT)
 *
 * Copyright (c) 2019 Albert Moky
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

import java.security.InvalidParameterException;
import java.util.List;

import chat.dim.crypto.DecryptKey;
import chat.dim.crypto.EncryptKey;
import chat.dim.crypto.SignKey;
import chat.dim.crypto.VerifyKey;
import chat.dim.protocol.Document;
import chat.dim.protocol.ID;
import chat.dim.protocol.Visa;

/**
 *  User account for communication
 *  ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 *  This class is for creating user account
 *
 *  functions:
 *      (User)
 *      1. verify(data, signature) - verify (encrypted content) data and signature
 *      2. encrypt(data)           - encrypt (symmetric key) data
 *      (LocalUser)
 *      3. sign(data)    - calculate signature of (encrypted content) data
 *      4. decrypt(data) - decrypt (symmetric key) data
 */
public class User extends Entity {

    public User(ID identifier) {
        super(identifier);
    }

    @Override
    public DataSource getDataSource() {
        return (DataSource) super.getDataSource();
    }

    public Visa getVisa() {
        Document doc = getDocument(Document.VISA);
        if (doc instanceof Visa) {
            return (Visa) doc;
        }
        return null;
    }

    /**
     *  Get all contacts of the user
     *
     * @return contact list
     */
    public List<ID> getContacts() {
        DataSource delegate = getDataSource();
        assert delegate != null : "user delegate not set yet";
        return delegate.getContacts(identifier);
    }

    /**
     *  Verify data and signature with user's public keys
     *
     * @param data - message data
     * @param signature - message signature
     * @return true on correct
     */
    public boolean verify(byte[] data, byte[] signature) {
        DataSource delegate = getDataSource();
        assert delegate != null : "user delegate not set yet";
        // NOTICE: I suggest using the private key paired with meta.key to sign message
        //         so here should return the meta.key
        List<VerifyKey> keys = delegate.getPublicKeysForVerification(identifier);
        for (VerifyKey key : keys) {
            if (key.verify(data, signature)) {
                // matched!
                return true;
            }
        }
        return false;
    }

    /**
     *  Encrypt data, try visa.key first, if not found, use meta.key
     *
     * @param plaintext - message data
     * @return encrypted data
     */
    public byte[] encrypt(byte[] plaintext) {
        DataSource delegate = getDataSource();
        assert delegate != null : "user delegate not set yet";
        // NOTICE: meta.key will never changed, so use visa.key to encrypt message
        //         is a better way
        EncryptKey key = delegate.getPublicKeyForEncryption(identifier);
        assert key != null : "failed to get encrypt key for user: " + identifier;
        return key.encrypt(plaintext);
    }

    //
    //  Interfaces for Local User
    //

    /**
     *  Sign data with user's private key
     *
     * @param data - message data
     * @return signature
     */
    public byte[] sign(byte[] data) {
        DataSource delegate = getDataSource();
        assert delegate != null : "user delegate not set yet";
        // NOTICE: I suggest use the private key which paired to visa.key
        //         to sign message
        SignKey key = delegate.getPrivateKeyForSignature(identifier);
        assert key != null : "failed to get sign key for user: " + identifier;
        return key.sign(data);
    }

    /**
     *  Decrypt data with user's private key(s)
     *
     * @param ciphertext - encrypted data
     * @return plain text
     */
    public byte[] decrypt(byte[] ciphertext) {
        DataSource delegate = getDataSource();
        assert delegate != null : "user delegate not set yet";
        // NOTICE: if you provide a public key in visa for encryption,
        //         here you should return the private key paired with visa.key
        List<DecryptKey> keys = delegate.getPrivateKeysForDecryption(identifier);
        assert keys != null && keys.size() > 0 : "failed to get decrypt keys for user: " + identifier;
        byte[] plaintext;
        for (DecryptKey key : keys) {
            // try decrypting it with each private key
            try {
                plaintext = key.decrypt(ciphertext);
                if (plaintext != null) {
                    // OK!
                    return plaintext;
                }
            } catch (InvalidParameterException e) {
                // this key not match, try next one
                //e.printStackTrace();
            }
        }
        // decryption failed
        return null;
    }

    //
    //  Interfaces for Visa
    //
    public Visa sign(Visa doc) {
        assert doc.getIdentifier().equals(identifier) : "visa ID not match: " + identifier + ", " + doc.getIdentifier();
        DataSource delegate = getDataSource();
        assert delegate != null : "user delegate not set yet";
        // NOTICE: only sign visa with the private key paired with your meta.key
        SignKey key = delegate.getPrivateKeyForVisaSignature(identifier);
        assert key != null : "failed to get sign key for visa: " + identifier;
        doc.sign(key);
        return doc;
    }

    public boolean verify(Visa doc) {
        // NOTICE: only verify visa with meta.key
        if (!identifier.equals(doc.getIdentifier())) {
            // visa ID not match
            return false;
        }
        // if meta not exists, user won't be created
        VerifyKey key = getMeta().getKey();
        assert key != null : "failed to get verify key for visa: " + identifier;
        return doc.verify(key);
    }

    /**
     *  User Data Source
     *  ~~~~~~~~~~~~~~~~
     *
     *  (Encryption/decryption)
     *  1. public key for encryption
     *     if visa.key not exists, means it is the same key with meta.key
     *  2. private keys for decryption
     *     the private keys paired with [visa.key, meta.key]
     *
     *  (Signature/Verification)
     *  3. private key for signature
     *     the private key paired with visa.key or meta.key
     *  4. public keys for verification
     *     [visa.key, meta.key]
     *
     *  (Visa Document)
     *  5. private key for visa signature
     *     the private key pared with meta.key
     *  6. public key for visa verification
     *     meta.key only
     */
    public interface DataSource extends Entity.DataSource {

        /**
         *  Get contacts list
         *
         * @param user - user ID
         * @return contacts list (ID)
         */
        List<ID> getContacts(ID user);

        /**
         *  Get user's public key for encryption
         *  (visa.key or meta.key)
         *
         * @param user - user ID
         * @return visa.key or meta.key
         */
        EncryptKey getPublicKeyForEncryption(ID user);

        /**
         *  Get user's public keys for verification
         *  [visa.key, meta.key]
         *
         * @param user - user ID
         * @return public keys
         */
        List<VerifyKey> getPublicKeysForVerification(ID user);

        /**
         *  Get user's private keys for decryption
         *  (which paired with [visa.key, meta.key])
         *
         * @param user - user ID
         * @return private keys
         */
        List<DecryptKey> getPrivateKeysForDecryption(ID user);

        /**
         *  Get user's private key for signature
         *  (which paired with visa.key or meta.key)
         *
         * @param user - user ID
         * @return private key
         */
        SignKey getPrivateKeyForSignature(ID user);

        /**
         *  Get user's private key for signing visa
         *
         * @param user - user ID
         * @return private key
         */
        SignKey getPrivateKeyForVisaSignature(ID user);
    }
}
