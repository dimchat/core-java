/* license: https://mit-license.org
 * ==============================================================================
 * The MIT License (MIT)
 *
 * Copyright (c) 2025 Albert Moky
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
package chat.dim.format;

import java.net.URI;
import java.util.Map;

import chat.dim.ext.SharedFileFormatExtensions;
import chat.dim.protocol.DecryptKey;
import chat.dim.protocol.TransportableData;


/**
 *  PNF Wrapper.
 *  <p>
 *  A wrapper interface for serializing/deserializing {@link chat.dim.protocol.TransportableFile} data
 *  to/from a Map.
 *  </p>
 *  The serialized Map follows this structure:
 *  <blockquote><pre>
 *  {
 *      "data"     : "&lt;base64-encoded file content&gt;",   // from TransportableData
 *      "filename" : "photo.png",                         // original file name
 *
 *      "URL"      : "http://example.com/photo.png",     // remote CDN URL (alternative to `data`)
 *      "key"      : {                                   // symmetric decryption key (encrypted CDN content)
 *          "algorithm" : "AES",                         // encryption algorithm ("AES", "DES", ...)
 *          "data"      : "&lt;base64-encoded key data&gt;" // key material (base64 encoded)
 *      }
 *  }
 *  </pre></blockquote>
 *  Key notes:
 *  <ul>
 *    <li>{@code data} and {@code URL} are mutually exclusive for large files
 *        (prefer {@code URL} to reduce payload size)</li>
 *    <li>{@code key} is required only if the CDN-hosted content is encrypted</li>
 *  </ul>
 */
public interface TransportableFileWrapper {

    /**
     *  Converts the wrapper's state to a structured Map.
     *  <p>
     *  Serializes the {@link TransportableData} into the "data" field; subclasses may
     *  override this to implement lazy serialization for other properties (e.g. defer
     *  encoding large file data until this method is called).
     *  </p>
     */
    Map<String, Object> toMap();

    /**
     *  Binary file data (encoded as {@link TransportableData}).
     *  <p>
     *  For large files, use {@link #setURL(URI) url} instead to avoid large payloads.
     *  </p>
     */
    TransportableData getData();
    void setData(TransportableData ted);

    /**
     *  Original filename of the file (e.g., "avatar.png").
     */
    String getFilename();
    void setFilename(String name);

    /**
     *  Remote CDN URL to download the file (alternative to {@link #getData() data} for large files).
     */
    URI getURL();
    void setURL(URI remote);

    /**
     *  Symmetric decryption key for encrypted file content from {@link #getURL() url}.
     *  <p>
     *  Aliased as {@code password} for legacy compatibility (actual value is a
     *  {@link DecryptKey}).
     *  </p>
     */
    DecryptKey getPassword();
    void setPassword(DecryptKey password);

    //
    //  Factory
    //

    /**
     *  Create wrapper with the given content
     */
    static TransportableFileWrapper create(Map<String, Object> content) {
        TransportableFileWrapper.Factory factory = SharedFileFormatExtensions.pnfWrapperFactory;
        return factory.createTransportableFileWrapper(content);
    }

    /**
     *  Create wrapper with the given content and overrides
     */
    static TransportableFileWrapper create(Map<String, Object> content,
                                           TransportableData data, String filename, URI url, DecryptKey password) {
        TransportableFileWrapper.Factory factory = SharedFileFormatExtensions.pnfWrapperFactory;
        return factory.createTransportableFileWrapper(content, data, filename, url, password);
    }

    /**
     *  Factory interface for creating {@link TransportableFileWrapper} instances.
     *  <p>
     *  Implement this interface to provide custom wrapper implementations
     *  (e.g., for different serialization formats).
     *  </p>
     */
    interface Factory {

        /**
         *  Create a wrapper with the given content only.
         */
        TransportableFileWrapper createTransportableFileWrapper(Map<String, Object> content);

        /**
         *  Create a wrapper with the given content and overrides.
         *
         *  @param content   base Map to initialize the wrapper
         *  @param data      binary file data (overrides {@code content["data"]})
         *  @param filename  original file name (overrides {@code content["filename"]})
         *  @param url       remote CDN URL (overrides {@code content["URL"]})
         *  @param password  decryption key (overrides {@code content["key"]})
         */
        TransportableFileWrapper createTransportableFileWrapper(Map<String, Object> content,
                                                                TransportableData data, String filename, URI url, DecryptKey password);

    }

}
