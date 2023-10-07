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

import chat.dim.crypto.DecryptKey;
import chat.dim.format.TransportableData;
import chat.dim.protocol.ContentType;
import chat.dim.protocol.file.ImageContent;

/**
 *  Image message: {
 *      type : 0x12,
 *      sn   : 123,
 *
 *      data     : "...",        // base64_encode(fileContent)
 *      filename : "photo.png",
 *
 *      URL      : "http://...", // download from CDN
 *      // before fileContent uploaded to a public CDN,
 *      // it should be encrypted by a symmetric key
 *      key      : {             // symmetric key to decrypt file content
 *          algorithm : "AES",   // "DES", ...
 *          data      : "{BASE64_ENCODE}",
 *          ...
 *      },
 *      thumbnail : "..."        // base64_encode(smallImage)
 *  }
 */
public class ImageFileContent extends BaseFileContent implements ImageContent {

    // small image
    private TransportableData thumbnail = null;

    public ImageFileContent(Map<String, Object> content) {
        super(content);
    }

    public ImageFileContent(TransportableData data, String filename, URI url, DecryptKey key) {
        super(ContentType.IMAGE, data, filename, url, key);
    }

    @Override
    public void setThumbnail(byte[] imageData) {
        if (imageData == null/* || imageData.length == 0*/) {
            remove("thumbnail");
            thumbnail = null;
        } else {
            TransportableData ted = TransportableData.create(imageData);
            put("thumbnail", ted.toObject());
            thumbnail = ted;
        }
    }

    @Override
    public byte[] getThumbnail() {
        TransportableData ted = thumbnail;
        if (ted == null) {
            Object base64 = get("thumbnail");
            thumbnail = ted = TransportableData.parse(base64);
        }
        return ted == null ? null : ted.getData();
    }
}
