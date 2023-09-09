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

import java.util.Map;

import chat.dim.format.TransportableData;
import chat.dim.protocol.ContentType;
import chat.dim.protocol.file.VideoContent;

/**
 *  Video message: {
 *      type : 0x16,
 *      sn   : 123,
 *
 *      URL      : "http://...", // download from CDN
 *      data     : "...",        // base64_encode(fileContent)
 *      filename : "movie.mp4",
 *      key      : {             // symmetric key to decrypt file content
 *          algorithm : "AES",   // "DES", ...
 *          data      : "{BASE64_ENCODE}",
 *          ...
 *      },
 *      snapshot : "..."         // base64_encode(smallImage)
 *  }
 */
public class VideoFileContent extends BaseFileContent implements VideoContent {

    private TransportableData snapshot = null;

    public VideoFileContent(Map<String, Object> content) {
        super(content);
    }

    public VideoFileContent(String filename, TransportableData data) {
        super(ContentType.VIDEO, filename, data);
    }

    @Override
    public void setSnapshot(byte[] imageData) {
        if (imageData != null && imageData.length > 0) {
            TransportableData ted = TransportableData.create(imageData);
            put("snapshot", ted.toObject());
            snapshot = ted;
        } else {
            remove("snapshot");
            snapshot = null;
        }
    }

    @Override
    public byte[] getSnapshot() {
        TransportableData ted = snapshot;
        if (ted == null) {
            String base64 = getString("snapshot", null);
            snapshot = ted = TransportableData.parse(base64);
        }
        return ted == null ? null : ted.getData();
    }
}
