/* license: https://mit-license.org
 * ==============================================================================
 * The MIT License (MIT)
 *
 * Copyright (c) 2026 Albert Moky
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

import chat.dim.protocol.DecryptKey;
import chat.dim.protocol.TransportableData;
import chat.dim.protocol.TransportableFile;
import chat.dim.type.Dictionary;


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
public class PortableNetworkFile extends Dictionary implements TransportableFile {

    private final PortableNetworkFileWrapper wrapper;

    public PortableNetworkFile(Map<String, Object> content) {
        super(content);
        wrapper = createWrapper();
    }

    public PortableNetworkFile(TransportableData data, String filename, URI url, DecryptKey key) {
        super();
        wrapper = createWrapper();
        // file data
        if (data != null) {
            wrapper.setData(data);
        }
        // file name
        if (filename != null) {
            wrapper.setFilename(filename);
        }
        // download URL
        if (url != null) {
            wrapper.setURL(url);
        }
        // decrypt key
        if (key != null) {
            wrapper.setPassword(key);
        }
    }

    protected PortableNetworkFileWrapper createWrapper() {
        PortableNetworkFileWrapper.Factory factory = SharedNetworkFormatAccess.pnfWrapperFactory;
        return factory.createPortableNetworkFileWrapper(super.toMap());
    }

    protected String getURIString() {
        // serialize
        Map<String, Object> map = wrapper.toMap();
        // check 'URL'
        URI url = getURL();
        if (url != null) {
            int count = map.size();
            if (map.containsKey("filename")) {
                count -= 1;
            }
            if (count != 1) {
                // this PNF info contains other params,
                // cannot serialize it as a string.
                return null;
            }
            // this PNF info contains 'URL' only (the field 'filename' can be ignored)
            // so serialize it as a string here.
            return url.toString();
        }
        // check 'data'
        String text = getString("data");
        if (text != null && text.startsWith("data:")) {
            int count = map.size();
            if (map.containsKey("filename")) {
                count -= 1;
            }
            if (count != 1) {
                // this PNF info contains other params,
                // cannot serialize it as a string.
                return null;
            }
            // this PNF info contains 'data' only (the field 'filename' can be ignored)
            // so serialize it as a string here.
            return text;
        }
        // cannot build URI string
        assert map.containsKey("filename") : "PNF info error: " + map;
        return null;
    }

    @Override
    public String toString() {
        String uri = getURIString();
        if (uri != null) {
            // this PNF can be simplified to a URI string
            return uri;
        }
        // return JSON string
        return JSONMap.encode(wrapper.toMap());
    }

    @Override
    public Map<String, Object> toMap() {
        // call wrapper to serialize 'data' & 'key"
        return wrapper.toMap();
    }

    @Override
    public Object serialize() {
        String uri = getURIString();
        if (uri != null) {
            // this PNF can be simplified to a URI string
            return uri;
        }
        // return inner map
        return wrapper.toMap();
    }

    /**
     *  file data
     */

    @Override
    public TransportableData getData() {
        return wrapper.getData();
    }

    @Override
    public void setData(TransportableData data) {
        wrapper.setData(data);
    }

    /**
     *  file name
     */

    @Override
    public String getFilename() {
        return wrapper.getFilename();
    }

    @Override
    public void setFilename(String name) {
        wrapper.setFilename(name);
    }

    /**
     *  download URL
     */

    @Override
    public URI getURL() {
        return wrapper.getURL();
    }

    @Override
    public void setURL(URI url) {
        wrapper.setURL(url);
    }

    /**
     *  decrypt key
     */

    @Override
    public DecryptKey getPassword() {
        return wrapper.getPassword();
    }

    @Override
    public void setPassword(DecryptKey key) {
        wrapper.setPassword(key);
    }

}
