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
import chat.dim.protocol.VideoContent;

/**
 *  Video message: {
 *      type : 0x16,
 *      sn   : 123,
 *
 *      URL      : "http://", // upload to CDN
 *      data     : "...",     // if (!URL) base64_encode(video)
 *      snapshot : "...",     // base64_encode(smallImage)
 *      filename : "..."
 *  }
 */
public class VideoFileContent extends BaseFileContent implements VideoContent {

    private byte[] snapshot = null;

    public VideoFileContent(Map<String, Object> dictionary) {
        super(dictionary);
    }

    public VideoFileContent(String filename, byte[] data) {
        super(ContentType.VIDEO, filename, data);
    }

    @Override
    public void setSnapshot(byte[] imageData) {
        snapshot = imageData;
        if (imageData == null) {
            remove("snapshot");
        } else {
            put("snapshot", Base64.encode(imageData));
        }
    }

    @Override
    public byte[] getSnapshot() {
        if (snapshot == null) {
            String base64 = (String) get("snapshot");
            if (base64 != null) {
                snapshot = Base64.decode(base64);
            }
        }
        return snapshot;
    }
}
