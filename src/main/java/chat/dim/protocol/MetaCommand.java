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

import java.util.Map;

import chat.dim.Meta;

/**
 *  Command message: {
 *      type : 0x88,
 *      sn   : 123,
 *
 *      command : "meta", // command name
 *      ID      : "{ID}", // contact's ID
 *      meta    : {...}   // when meta is empty, means query meta for ID
 *  }
 */
public class MetaCommand extends Command {

    public MetaCommand(Map<String, Object> dictionary) {
        super(dictionary);
    }

    MetaCommand(String command, Object identifier, Meta meta) {
        super(command);
        setIdentifier(identifier);
        setMeta(meta);
    }

    /**
     *  Response Meta
     *
     * @param identifier - entity ID
     * @param meta - entity Meta
     */
    public MetaCommand(Object identifier, Meta meta) {
        this(META, identifier, meta);
    }

    /**
     *  Query Meta
     *
     * @param identifier - entity ID
     */
    public MetaCommand(Object identifier) {
        this(identifier, null);
    }

    /*
     *  Entity ID (or String)
     *
     */
    public Object getIdentifier() {
        return dictionary.get("ID");
    }

    public void setIdentifier(Object identifier) {
        assert identifier != null;
        dictionary.put("ID", identifier);
    }

    /*
     *  Entity Meta
     *
     */
    public Meta getMeta() throws ClassNotFoundException {
        Meta meta = null;
        Object data = dictionary.get("meta");
        if (data != null) {
            meta = Meta.getInstance(data);
            if (data != meta) {
                // put back the Meta object for next access
                dictionary.put("meta", meta);
            }
        }
        return meta;
    }

    public void setMeta(Meta meta) {
        if (meta == null) {
            dictionary.remove("meta");
        } else {
            dictionary.put("meta", meta);
        }
    }
}
