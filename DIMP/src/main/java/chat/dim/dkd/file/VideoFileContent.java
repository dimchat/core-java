/* license: https://mit-license.org
 *
 *  DIMP : Decentralized Instant Messaging Protocol
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
package chat.dim.dkd.file;

import java.net.URI;
import java.util.Map;

import chat.dim.protocol.ContentType;
import chat.dim.protocol.DecryptKey;
import chat.dim.protocol.PortableNetworkFile;
import chat.dim.protocol.TransportableData;
import chat.dim.protocol.VideoContent;

/**
 *  Video File Content
 *
 *  <blockquote><pre>
 *  data format: {
 *      "type" : i2s(0x16),
 *      "sn"   : 123,
 *
 *      "data"     : "...",        // base64_encode(fileContent)
 *      "filename" : "movie.mp4",
 *
 *      "URL"      : "http://...", // download from CDN
 *      // before fileContent uploaded to a public CDN,
 *      // it should be encrypted by a symmetric key
 *      "key"      : {             // symmetric key to decrypt file data
 *          "algorithm" : "AES",   // "DES", ...
 *          "data"      : "{BASE64_ENCODE}",
 *          ...
 *      },
 *      "snapshot" : "data:image/jpeg;base64,..."
 *  }
 *  </pre></blockquote>
 */
public class VideoFileContent extends BaseFileContent implements VideoContent {

    // small image
    private PortableNetworkFile snapshot = null;

    public VideoFileContent(Map<String, Object> content) {
        super(content);
    }

    public VideoFileContent(TransportableData data, String filename, URI url, DecryptKey key) {
        super(ContentType.VIDEO, data, filename, url, key);
    }

    @Override
    public void setSnapshot(PortableNetworkFile img) {
        if (img == null || img.isEmpty()) {
            remove("snapshot");
        } else {
            put("snapshot", img.toObject());
        }
        snapshot = img;
    }

    @Override
    public PortableNetworkFile getSnapshot() {
        PortableNetworkFile img = snapshot;
        if (img == null) {
            Object uri = get("snapshot");
            img = PortableNetworkFile.parse(uri);
            snapshot = img;
        }
        return img;
    }

}
