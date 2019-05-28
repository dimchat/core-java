/* license: https://mit-license.org
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
package chat.dim.core;

import chat.dim.crypto.SymmetricKey;
import chat.dim.dkd.InstantMessage;
import chat.dim.mkm.entity.ID;

public interface TransceiverDelegate {

    /**
     *  Send out a data package onto network
     *
     * @param data - package`
     * @param handler - completion handler
     * @return NO on data/delegate error
     */
    boolean sendPackage(byte[] data, CompletionHandler handler);

    /**
     *  Update/create cipher key for encrypt message content
     *
     * @param sender - user identifier
     * @param receiver - contact/group identifier
     * @param reusedKey - old key (nullable)
     * @return new key
     */
    SymmetricKey reuseCipherKey(ID sender, ID receiver, SymmetricKey reusedKey);

    /**
     *  Upload encrypted data to CDN
     *
     * @param data - encrypted file data
     * @param iMsg - instant message
     * @return download URL
     */
    String uploadFileData(byte[] data, InstantMessage iMsg);

    /**
     *  Download encrypted data from CDN, and decrypt it when finished
     *
     * @param url - download URL
     * @param iMsg - instant message
     * @return encrypted file data
     */
    byte[] downloadFileData(String url, InstantMessage iMsg);
}
