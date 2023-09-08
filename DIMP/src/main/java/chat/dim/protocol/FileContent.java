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
import chat.dim.dkd.AudioFileContent;
import chat.dim.dkd.BaseFileContent;
import chat.dim.dkd.ImageFileContent;
import chat.dim.dkd.VideoFileContent;

/**
 *  File message: {
 *      type : 0x10,
 *      sn   : 123,
 *
 *      URL      : "http://...", // download from CDN
 *      data     : "...",        // base64_encode(fileContent)
 *      filename : "photo.png"
 *      key      : {             // symmetric key to decrypt file content
 *          algorithm : "AES",   // "DES", ...
 *          data      : "{BASE64_ENCODE}",
 *          ...
 *      }
 *  }
 */
public interface FileContent extends Content {

    void setURL(URI url);
    URI getURL();

    void setData(byte[] binary);
    byte[] getData();

    void setFilename(String name);
    String getFilename();

    // symmetric key to decrypt the encrypted data from URL
    void setPassword(DecryptKey key);
    DecryptKey getPassword();

    //
    //  Factories
    //

    static FileContent create(ContentType type, String filename, byte[] binary) {
        return new BaseFileContent(type, filename, binary);
    }
    static FileContent create(int type, String filename, byte[] binary) {
        return new BaseFileContent(type, filename, binary);
    }

    static FileContent file(String filename, byte[] binary) {
        return new BaseFileContent(filename, binary);
    }

    static ImageContent image(String filename, byte[] binary) {
        return new ImageFileContent(filename, binary);
    }

    static AudioContent audio(String filename, byte[] binary) {
        return new AudioFileContent(filename, binary);
    }

    static VideoContent video(String filename, byte[] binary) {
        return new VideoFileContent(filename, binary);
    }
}
