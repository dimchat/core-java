/* license: https://mit-license.org
 *
 *  DIMP : Decentralized Instant Messaging Protocol
 *
 *                                Written in 2023 by Moky <albert.moky@gmail.com>
 *
 * ==============================================================================
 * The MIT License (MIT)
 *
 * Copyright (c) 2023 Albert Moky
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

import chat.dim.protocol.ContentType;
import chat.dim.protocol.ID;
import chat.dim.protocol.NameCard;
import chat.dim.protocol.TransportableFile;

/**
 *  Name Card
 *
 *  <blockquote><pre>
 *  data format: {
 *      "type" : i2s(0x33),
 *      "sn"   : 123,
 *
 *      "did"    : "{ID}",        // contact's ID
 *      "name"   : "{nickname}",  // contact's name
 *      "avatar" : "{URL}",       // avatar - PNF(URL)
 *      ...
 *  }
 *  </pre></blockquote>
 */
public class NameCardContent extends BaseContent implements NameCard {

    private TransportableFile image;

    public NameCardContent(Map<String, Object> content) {
        super(content);
        // lazy load
        image = null;
    }

    public NameCardContent(ID did, String name, TransportableFile avatar) {
        super(ContentType.NAME_CARD);
        // ID
        put("did", did.toString());
        // name
        put("name", name);
        // avatar
        image = avatar;  // lazy serialize
    }

    @Override
    public Map<String, Object> toMap() {
        // serialize 'avatar'
        TransportableFile img = image;
        if (img != null && get("avatar") == null) {
            put("avatar", img.serialize());
        }
        // OK
        return super.toMap();
    }

    @Override
    public ID getIdentifier() {
        return ID.parse(get("did"));
    }

    @Override
    public String getName() {
        return getString("name", "");
    }

    @Override
    public TransportableFile getAvatar() {
        TransportableFile img = image;
        if (img == null) {
            Object uri = get("avatar");
            img = TransportableFile.parse(uri);
            image = img;
        }
        return img;
    }

}
