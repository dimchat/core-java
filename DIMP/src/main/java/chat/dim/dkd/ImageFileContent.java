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
package chat.dim.dkd;

import java.util.Map;

import chat.dim.format.Base64;
import chat.dim.protocol.ContentType;
import chat.dim.protocol.ImageContent;

/**
 *  Image message: {
 *      type : 0x12,
 *      sn   : 123,
 *
 *      URL       : "http://", // upload to CDN
 *      data      : "...",     // if (!URL) base64_encode(image)
 *      thumbnail : "...",     // base64_encode(smallImage)
 *      filename  : "..."
 *  }
 */
public class ImageFileContent extends BaseFileContent implements ImageContent {

    private byte[] thumbnail = null;

    public ImageFileContent(Map<String, Object> content) {
        super(content);
    }

    public ImageFileContent(String filename, String encoded) {
        super(ContentType.IMAGE, filename, encoded);
    }
    public ImageFileContent(String filename, byte[] binary) {
        super(ContentType.IMAGE, filename, binary);
    }

    @Override
    public void setThumbnail(byte[] imageData) {
        thumbnail = imageData;
        if (imageData == null) {
            remove("thumbnail");
        } else {
            put("thumbnail", Base64.encode(imageData));
        }
    }

    @Override
    public byte[] getThumbnail() {
        if (thumbnail == null) {
            String base64 = getString("thumbnail");
            if (base64 != null) {
                thumbnail = Base64.decode(base64);
            }
        }
        return thumbnail;
    }
}
