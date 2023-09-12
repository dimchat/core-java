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
import chat.dim.protocol.MetaType;

public interface MetaHelper {

    static boolean check(Meta meta) {
        VerifyKey key = meta.getPublicKey();
        if (key == null) {
            assert false : "meta.key should not be empty";
            return false;
        }
        String seed = meta.getSeed();
        byte[] fingerprint = meta.getFingerprint();
        boolean noSeed = seed == null || seed.isEmpty();
        boolean noSig = fingerprint == null || fingerprint.length == 0;
        // check meta version
        if (!MetaType.hasSeed(meta.getType())) {
            // this meta has no seed, so no signature too
            return noSeed && noSig;
        } else if (noSeed || noSig) {
            // seed and fingerprint should not be empty
            return false;
        }
        // verify fingerprint
        return key.verify(UTF8.encode(seed), fingerprint);
    }

    static boolean matches(ID identifier, Meta meta) {
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
        return old.equals(gen);
    }

    static  boolean matches(VerifyKey pKey, Meta meta) {
        // check whether the public key equals to meta.key
        if (pKey.equals(meta.getPublicKey())) {
            return true;
        }
        // check with seed & fingerprint
        if (MetaType.hasSeed(meta.getType())) {
            // check whether keys equal by verifying signature
            String seed = meta.getSeed();
            byte[] fingerprint = meta.getFingerprint();
            return pKey.verify(UTF8.encode(seed), fingerprint);
        } else {
            // NOTICE: ID with BTC/ETH address has no username, so
            //         just compare the key.data to check matching
            return false;
        }
    }

}
