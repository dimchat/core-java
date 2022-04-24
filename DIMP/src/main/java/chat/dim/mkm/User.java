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

import java.util.List;

import chat.dim.crypto.DecryptKey;
import chat.dim.crypto.EncryptKey;
import chat.dim.crypto.SignKey;
import chat.dim.crypto.VerifyKey;
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
public interface User extends Entity {

    Visa getVisa();

    /**
     *  Get all contacts of the user
     *
     * @return contact list
     */
    List<ID> getContacts();

    /**
     *  Verify data and signature with user's public keys
     *
     * @param data - message data
     * @param signature - message signature
     * @return true on correct
     */
    boolean verify(byte[] data, byte[] signature);

    /**
     *  Encrypt data, try visa.key first, if not found, use meta.key
     *
     * @param plaintext - message data
     * @return encrypted data
     */
    byte[] encrypt(byte[] plaintext);

    //
    //  Interfaces for Local User
    //

    /**
     *  Sign data with user's private key
     *
     * @param data - message data
     * @return signature
     */
    byte[] sign(byte[] data);

    /**
     *  Decrypt data with user's private key(s)
     *
     * @param ciphertext - encrypted data
     * @return plain text
     */
    byte[] decrypt(byte[] ciphertext);

    //
    //  Interfaces for Visa
    //
    Visa sign(Visa doc);

    boolean verify(Visa doc);

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
    interface DataSource extends Entity.DataSource {

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
