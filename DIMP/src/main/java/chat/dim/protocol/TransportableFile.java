/* license: https://mit-license.org
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
package chat.dim.protocol;

import java.net.URI;
import java.util.Map;

import chat.dim.ext.SharedFileFormatExtensions;
import chat.dim.type.Mapper;

/**
 *  Transportable File
 *  <p>
 *      PNF - Portable Network File
 *  </p>
 *
 *  <blockquote><pre>
 *  0.  "{URL}"
 *  1. {
 *         "data"     : "...",        // base64_encode(fileContent)
 *         "filename" : "avatar.png",
 *
 *         "URL"      : "http://...", // download from CDN
 *         // before fileContent uploaded to a public CDN,
 *         // it can be encrypted by a symmetric key
 *         "key"      : {             // symmetric key to decrypt file data
 *             "algorithm" : "AES",   // "DES", ...
 *             "data"      : "{BASE64_ENCODE}",
 *             ...
 *         }
 *      }
 *  </pre></blockquote>
 */
public interface TransportableFile extends TransportableResource, Mapper<String, Object> {

    /** When file data is too big, don't set it in this dictionary,
     *  but upload it to a CDN and set the download URL instead.
     */
    void setData(TransportableData data);
    TransportableData getData();

    void setFilename(String filename);
    String getFilename();

    /** Download URL
     */
    void setURL(URI url);
    URI getURL();

    /** Password for decrypting the downloaded data from CDN,
     *  default is a plain key, which just return the same data when decrypting.
     */
    void setPassword(DecryptKey key);
    DecryptKey getPassword();

    /**
     *  Get encoded string
     *
     * @return "URL", or
     *         "{...}"
     */
    @Override
    String toString();

    /**
     *  Encode data and update inner map
     *
     * @return inner map
     */
    @Override
    Map<String, Object> toMap();

    /**
     *  if contains "URL" and "filename" only,
     *      toString();
     *  else,
     *      toMap();
     */
    @Override
    Object serialize();

    //
    //  Factory methods
    //

    /**
     *  Create from remote URL
     */
    static TransportableFile create(URI url, DecryptKey password) {
        return create(null, null, url, password);
    }
    /**
     *  Create from file data
     */
    static TransportableFile create(TransportableData data, String filename) {
        return create(data, filename, null, null);
    }

    static TransportableFile create(TransportableData data, String filename, URI url, DecryptKey password) {
        return SharedFileFormatExtensions.pnfHelper.createTransportableFile(data, filename, url, password);
    }

    static TransportableFile parse(Object pnf) {
        return SharedFileFormatExtensions.pnfHelper.parseTransportableFile(pnf);
    }

    static void setFactory(Factory factory) {
        SharedFileFormatExtensions.pnfHelper.setTransportableFileFactory(factory);
    }
    static Factory getFactory() {
        return SharedFileFormatExtensions.pnfHelper.getTransportableFileFactory();
    }

    /**
     *  PNF Factory
     */
    interface Factory {

        /**
         *  Create PNF
         *
         * @param data
         *        file content (not encrypted)
         *
         * @param filename
         *        file name
         *
         * @param url
         *        download URL
         *
         * @param password
         *        decrypt key for downloaded data
         *
         * @return PNF object
         */
        TransportableFile createTransportableFile(TransportableData data, String filename,
                                                  URI url, DecryptKey password);

        /**
         *  Parse string/map object to PNF
         *
         * @param pnf
         *        PNF info
         *
         * @return PNF object
         */
        TransportableFile parseTransportableFile(Map<String, Object> pnf);
    }

}
