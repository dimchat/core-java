/* license: https://mit-license.org
 *
 *  DIMP : Decentralized Instant Messaging Protocol
 *
 *                                Written in 2021 by Moky <albert.moky@gmail.com>
 *
 * ==============================================================================
 * The MIT License (MIT)
 *
 * Copyright (c) 2021 Albert Moky
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
package chat.dim;

import chat.dim.protocol.InstantMessage;
import chat.dim.protocol.ReliableMessage;
import chat.dim.protocol.SecureMessage;

/**
 *  Message Packer
 *  ~~~~~~~~~~~~~~
 */
public interface Packer {

    //
    //  InstantMessage -> SecureMessage -> ReliableMessage -> Data
    //

    /**
     *  Encrypt message content
     *
     * @param iMsg - plain message
     * @return encrypted message
     */
    SecureMessage encryptMessage(InstantMessage iMsg);

    /**
     *  Sign content data
     *
     * @param sMsg - encrypted message
     * @return network message
     */
    ReliableMessage signMessage(SecureMessage sMsg);

    /**
     *  Serialize network message
     *
     * @param rMsg - network message
     * @return data package
     */
    byte[] serializeMessage(ReliableMessage rMsg);

    //
    //  Data -> ReliableMessage -> SecureMessage -> InstantMessage
    //

    /**
     *  Deserialize network message
     *
     * @param data - data package
     * @return network message
     */
    ReliableMessage deserializeMessage(byte[] data);

    /**
     *  Verify encrypted content data
     *
     * @param rMsg - network message
     * @return encrypted message
     */
    SecureMessage verifyMessage(ReliableMessage rMsg);

    /**
     *  Decrypt message content
     *
     * @param sMsg - encrypted message
     * @return plain message
     */
    InstantMessage decryptMessage(SecureMessage sMsg);
}
