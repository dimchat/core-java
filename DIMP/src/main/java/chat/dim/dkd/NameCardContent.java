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

import chat.dim.format.PortableNetworkFile;
import chat.dim.protocol.ContentType;
import chat.dim.protocol.ID;
import chat.dim.protocol.NameCard;
import chat.dim.type.Converter;

/**
 *  Name Card: {
 *      type : 0x33,
 *      sn   : 123,
 *
 *      did    : "{ID}",        // contact's ID
 *      name   : "{nickname}",  // contact's name
 *      avatar : "{URL}",       // avatar - PNF(URL)
 *      ...
 *  }
 */
public class NameCardContent extends BaseContent implements NameCard {

    private PortableNetworkFile image;

    public NameCardContent(Map<String, Object> content) {
        super(content);
        // lazy load
        image = null;
    }

    public NameCardContent(ID identifier, String name, PortableNetworkFile avatar) {
        super(ContentType.NAME_CARD);
        // ID
        put("did", identifier.toString());
        // name
        put("name", name);
        // avatar
        if (avatar != null) {
            // encode
            put("avatar", avatar.toObject());
        }
        image = avatar;
    }

    @Override
    public ID getIdentifier() {
        return ID.parse(get("did"));
    }

    @Override
    public String getName() {
        return Converter.getString(get("name"), "");
    }

    @Override
    public PortableNetworkFile getAvatar() {
        PortableNetworkFile img = image;
        if (img == null) {
            Object url = get("avatar");
            img = image = PortableNetworkFile.parse(url);
        }
        return img;
    }
}
