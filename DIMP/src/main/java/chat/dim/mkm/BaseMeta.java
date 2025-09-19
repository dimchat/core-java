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

import chat.dim.ext.SharedAccountExtensions;
import chat.dim.format.UTF8;
import chat.dim.protocol.Meta;
import chat.dim.protocol.PublicKey;
import chat.dim.protocol.TransportableData;
import chat.dim.protocol.VerifyKey;
import chat.dim.type.Dictionary;

/**
 *  User/Group Meta data
 *  <p>
 *      This class is used to generate entity ID
 *  </p>
 *
 *  <blockquote><pre>
 *  data format: {
 *      type       : 1,              // algorithm version
 *      seed       : "moKy",         // user/group name
 *      key        : "{public key}", // PK = secp256k1(SK);
 *      fingerprint: "..."           // CT = sign(seed, SK);
 *  }
 *
 *  algorithm:
 *      fingerprint = sign(seed, SK);
 *
 *  abstract method:
 *      - Address generateAddress(int network);
 *  </pre></blockquote>
 */
public abstract class BaseMeta extends Dictionary implements Meta {

    /**
     *  Meta algorithm version
     *
     *  <pre>
     *  1 = MKM : username@address (default)
     *  2 = BTC : btc_address
     *  4 = ETH : eth_address
     *      ...
     *  </pre>
     */
    private String type = null;

    /**
     *  Public key (used for signature)
     *  <p>
     *      RSA / ECC
     *  </p>
     */
    private VerifyKey key = null;

    /**
     *  Seed to generate fingerprint
     *  <p>
     *      Username / Group-X
     *  </p>
     */
    private String seed = null;

    /**
     *  Fingerprint to verify ID and public key
     *
     *  <pre>
     *  Build: fingerprint = sign(seed, privateKey)
     *  Check: verify(seed, fingerprint, publicKey)
     *  </pre>
     */
    private TransportableData fingerprint = null;

    private int status;        // 1 for valid, -1 for invalid

    protected BaseMeta(Map<String, Object> dictionary) {
        super(dictionary);
        // meta info from network, waiting to verify.
        this.status = 0;
    }

    protected BaseMeta(String type, VerifyKey key, String seed, TransportableData fingerprint) {
        super();

        // meta type
        put("type", type);
        this.type = type;

        // public key
        put("key", key.toMap());
        this.key = key;

        if (seed != null) {
            put("seed", seed);
            this.seed = seed;
        }

        if (fingerprint != null) {
            put("fingerprint", fingerprint.toObject());
            this.fingerprint = fingerprint;
        }

        // generated meta, or loaded from local storage,
        // no need to verify again.
        this.status = 1;
    }

    @Override
    public String getType() {
        if (type == null) {
            type = SharedAccountExtensions.helper.getMetaType(toMap(), "");
            // type = getInt("type", 0);
            assert type != null : "meta.type not found: " + toMap();
        }
        return type;
    }

    @Override
    public VerifyKey getPublicKey() {
        if (key == null) {
            Object info = get("key");
            assert info != null : "meta.key should not be empty: " + toMap();
            key = PublicKey.parse(info);
            assert key != null : "meta.key error: " + info;
        }
        return key;
    }

    /*/
    protected boolean hasSeed() {
        String algorithm = getType();
        return "MKM".equals(algorithm) || "1".equals(algorithm);
    }
    /*/
    protected abstract boolean hasSeed();

    @Override
    public String getSeed() {
        if (seed == null && hasSeed()) {
            seed = getString("seed", "");
            assert seed != null : "meta.seed not found";
        }
        return seed;
    }

    @Override
    public byte[] getFingerprint() {
        TransportableData ted = fingerprint;
        if (ted == null && hasSeed()) {
            Object base64 = get("fingerprint");
            assert base64 != null : "meta.fingerprint should not be empty: " + toMap();
            fingerprint = ted = TransportableData.parse(base64);
            assert ted != null : "meta.fingerprint error: " + base64;
        }
        return ted == null ? null : ted.getData();
    }

    //
    //  Validation
    //

    @Override
    public boolean isValid() {
        if (status == 0) {
            // meta from network, try to verify
            if (checkValid()) {
                // correct
                status = 1;
            } else {
                // error
                status = -1;
            }
        }
        return status > 0;
    }

    private boolean checkValid() {
        VerifyKey key = getPublicKey();
        if (key == null) {
            assert false : "meta.key should not be empty";
            return false;
        } else if (hasSeed()) {
            assert containsKey("seed") && containsKey("fingerprint") : "meta error: " + toMap();
        } else {
            // this meta has no seed, so
            // it should not contains 'seed' or 'fingerprint'
            return !(containsKey("seed") || containsKey("fingerprint"));
        }
        String seed = getSeed();
        byte[] fingerprint = getFingerprint();
        // check meta seed & signature
        if (fingerprint == null || fingerprint.length == 0 ||
                seed == null || seed.isEmpty()) {
            assert false : "meta error: " + toMap();
            return false;
        }
        // verify fingerprint
        byte[] data = UTF8.encode(seed);
        return key.verify(data, fingerprint);
    }

}
