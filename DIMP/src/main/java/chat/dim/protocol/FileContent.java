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
package chat.dim.protocol;

import java.net.URI;

import chat.dim.crypto.DecryptKey;
import chat.dim.dkd.file.AudioFileContent;
import chat.dim.dkd.file.BaseFileContent;
import chat.dim.dkd.file.ImageFileContent;
import chat.dim.dkd.file.VideoFileContent;
import chat.dim.format.TransportableData;
import chat.dim.protocol.file.AudioContent;
import chat.dim.protocol.file.ImageContent;
import chat.dim.protocol.file.VideoContent;

/**
 *  File message: {
 *      type : 0x10,
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
 *      }
 *  }
 */
public interface FileContent extends Content {

    void setData(byte[] binary);
    byte[] getData();

    void setFilename(String name);
    String getFilename();

    /**
     *  URL for download the file data from CDN
     */
    void setURL(URI url);
    URI getURL();

    /**
     *  Symmetric key to decrypt the downloaded data from URL
     */
    void setPassword(DecryptKey key);
    DecryptKey getPassword();

    //
    //  Factories
    //

    static FileContent create(String type, TransportableData data, String filename, URI url, DecryptKey key) {
        if (ContentType.IMAGE.equals(type)) {
            return new ImageFileContent(data, filename, url, key);
        } else if (ContentType.AUDIO.equals(type)) {
            return new AudioFileContent(data, filename, url, key);
        } else if (ContentType.VIDEO.equals(type)) {
            return new VideoFileContent(data, filename, url, key);
        }
        return new BaseFileContent(type, data, filename, url, key);
    }

    static FileContent file(TransportableData data, String filename, URI url, DecryptKey key) {
        return new BaseFileContent(ContentType.FILE, data, filename, url, key);
    }

    static ImageContent image(TransportableData data, String filename, URI url, DecryptKey key) {
        return new ImageFileContent(data, filename, url, key);
    }

    static AudioContent audio(TransportableData data, String filename, URI url, DecryptKey key) {
        return new AudioFileContent(data, filename, url, key);
    }

    static VideoContent video(TransportableData data, String filename, URI url, DecryptKey key) {
        return new VideoFileContent(data, filename, url, key);
    }

}
