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
package chat.dim.ext;

import java.net.URI;

import chat.dim.protocol.DecryptKey;
import chat.dim.protocol.TransportableData;
import chat.dim.protocol.TransportableFile;

/**
 *  PNF Helper.
 *  <p>
 *  Helper interface for creating/parsing {@link TransportableFile} instances.
 *  </p>
 *  Provides factory methods to abstract the creation logic of
 *  {@link TransportableFile} implementations.
 */
public interface TransportableFileHelper {

    /**
     *  Set transportable file factory.
     */
    void setTransportableFileFactory(TransportableFile.Factory factory);

    /**
     *  Get transportable file factory.
     */
    TransportableFile.Factory getTransportableFileFactory();

    /**
     *  Parses a raw object into a {@link TransportableFile} instance.
     *  <p>
     *  Converts arbitrary raw data (e.g., string, map) into a standardized
 *  TransportableFile object.
     *  </p>
     *
     *  @param pnf  raw data object to parse
     *  @return parsed {@link TransportableFile} instance (null if parsing fails)
     */
    TransportableFile parseTransportableFile(Object pnf);

    /**
     *  Creates a {@link TransportableFile} instance with the given metadata.
     *
     *  @param data      binary file data (encoded as {@link TransportableData})
     *  @param filename  original file name (e.g., "document.pdf")
     *  @param url       remote CDN URL (alternative to {@code data} for large files)
     *  @param password  decryption key for encrypted CDN content
     *  @return an initialized {@link TransportableFile} instance
     */
    TransportableFile createTransportableFile(TransportableData data, String filename,
                                              URI url, DecryptKey password);

}
