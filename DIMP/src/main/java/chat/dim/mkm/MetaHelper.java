/* license: https://mit-license.org
 *
 *  Ming-Ke-Ming : Decentralized User Identity Authentication
 *
 *                                Written in 2023 by Moky <albert.moky@gmail.com>
 *
 * ==============================================================================
 * The MIT License (MIT)
 *
 * Copyright (c) 2023 Albert Moky
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

import chat.dim.crypto.VerifyKey;
import chat.dim.format.UTF8;
import chat.dim.protocol.Address;
import chat.dim.protocol.ID;
import chat.dim.protocol.Meta;

public interface MetaHelper {

    static boolean check(Meta meta) {
        VerifyKey key = meta.getPublicKey();
        if (key == null) {
            assert false : "meta.key should not be empty";
            return false;
        }
        String seed = meta.getSeed();
        byte[] fingerprint = meta.getFingerprint();
        // check meta seed & signature
        if (seed == null || seed.length() == 0) {
            // this meta has no seed, so no signature too
            return fingerprint == null || fingerprint.length == 0;
        } else if (fingerprint == null || fingerprint.length == 0) {
            // fingerprint should not be empty here
            return false;
        }
        // verify fingerprint
        byte[] data = UTF8.encode(seed);
        return key.verify(data, fingerprint);
    }

    static boolean matches(ID identifier, Meta meta) {
        assert meta.isValid() : "meta not valid: " + meta;
        // check ID.name
        String seed = meta.getSeed();
        String name = identifier.getName();
        if (name == null || name.isEmpty()) {
            if (seed != null && seed.length() > 0) {
                return false;
            }
        } else if (!name.equals(seed)) {
            return false;
        }
        // check ID.address
        Address old = identifier.getAddress();
        //assert old != null : "ID error: " + identifier;
        Address gen = Address.generate(meta, old.getType());
        assert gen != null : "failed to generate address: " + old;
        return old.equals(gen);
    }

    static  boolean matches(VerifyKey pKey, Meta meta) {
        assert meta.isValid() : "meta not valid: " + meta;
        // check whether the public key equals to meta.key
        if (pKey.equals(meta.getPublicKey())) {
            return true;
        }
        // check with seed & fingerprint
        String seed = meta.getSeed();
        if (seed != null && seed.length() > 0) {
            // check whether keys equal by verifying signature
            byte[] data = UTF8.encode(seed);
            byte[] fingerprint = meta.getFingerprint();
            return pKey.verify(data, fingerprint);
        } else {
            // NOTICE: ID with BTC/ETH address has no username, so
            //         just compare the key.data to check matching
            return false;
        }
    }

}
