/* license: https://mit-license.org
 *
 *  Ming-Ke-Ming : Decentralized User Identity Authentication
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

import java.util.Map;

import chat.dim.crypto.PublicKey;
import chat.dim.crypto.VerifyKey;
import chat.dim.format.Base64;
import chat.dim.protocol.Meta;
import chat.dim.protocol.MetaType;
import chat.dim.type.Dictionary;

/**
 *  User/Group Meta data
 *  ~~~~~~~~~~~~~~~~~~~~
 *  This class is used to generate entity ID
 *
 *      data format: {
 *          type: 1,             // algorithm version
 *          seed: "moKy",        // user/group name
 *          key: "{public key}", // PK = secp256k1(SK);
 *          fingerprint: "..."   // CT = sign(seed, SK);
 *      }
 *
 *      algorithm:
 *          fingerprint = sign(seed, SK);
 */
public abstract class BaseMeta extends Dictionary implements Meta {

    /**
     *  Meta algorithm version
     *
     *      0x01 - username@address
     *      0x02 - btc_address
     *      0x03 - username@btc_address
     */
    private int type = 0;

    /**
     *  Public key (used for signature)
     *
     *      RSA / ECC
     */
    private VerifyKey key = null;

    /**
     *  Seed to generate fingerprint
     *
     *      Username / Group-X
     */
    private String seed = null;

    /**
     *  Fingerprint to verify ID and public key
     *
     *      Build: fingerprint = sign(seed, privateKey)
     *      Check: verify(seed, fingerprint, publicKey)
     */
    private byte[] fingerprint = null;

    protected BaseMeta(Map<String, Object> dictionary) {
        super(dictionary);
    }

    protected BaseMeta(int version, VerifyKey key, String seed, byte[] fingerprint) {
        super();

        // meta type
        put("type", version);
        this.type = version;

        // public key
        put("key", key.toMap());
        this.key = key;

        if (seed != null) {
            put("seed", seed);
            this.seed = seed;
        }

        if (fingerprint != null) {
            put("fingerprint", Base64.encode(fingerprint));
            this.fingerprint = fingerprint;
        }
    }

    @Override
    public int getType() {
        if (type == 0) {
            type = getInt("type");
        }
        return type;
    }

    @Override
    public VerifyKey getKey() {
        if (key == null) {
            Object info = get("key");
            assert info instanceof Map : "meta key not found: " + toMap();
            key = PublicKey.parse(info);
        }
        return key;
    }

    @Override
    public String getSeed() {
        if (seed == null && MetaType.hasSeed(getType())) {
            seed = getString("seed");
            assert seed != null && seed.length() > 0 : "meta.seed should not be empty: " + toMap();
        }
        return seed;
    }

    @Override
    public byte[] getFingerprint() {
        if (fingerprint == null && MetaType.hasSeed(getType())) {
            String base64 = getString("fingerprint");
            assert base64 != null : "meta.fingerprint should not be empty: " + toMap();
            fingerprint = Base64.decode(base64);
            assert fingerprint != null : "meta.fingerprint error: " + toMap();
        }
        return fingerprint;
    }
}
