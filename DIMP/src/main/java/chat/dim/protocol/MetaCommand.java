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

import chat.dim.dkd.BaseCommand;

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
public class MetaCommand extends BaseCommand {

    private ID identifier;
    private Meta meta;

    public MetaCommand(Map<String, Object> dictionary) {
        super(dictionary);
        identifier = null;
        meta = null;
    }

    public MetaCommand(String command, ID identifier, Meta meta) {
        super(command);
        // ID
        assert identifier != null : "ID cannot be empty for meta command";
        put("ID", identifier.toString());
        this.identifier = identifier;
        // meta
        if (meta != null) {
            put("meta", meta.getMap());
        }
        this.meta = meta;
    }

    /**
     *  Response Meta
     *
     * @param identifier - entity ID
     * @param meta - entity Meta
     */
    public MetaCommand(ID identifier, Meta meta) {
        this(META, identifier, meta);
    }

    /**
     *  Query Meta
     *
     * @param identifier - entity ID
     */
    public MetaCommand(ID identifier) {
        this(identifier, null);
    }

    /*
     *  Entity ID
     *
     */
    public ID getIdentifier() {
        if (identifier == null) {
            identifier = ID.parse(get("ID"));
        }
        return identifier;
    }

    /*
     *  Entity Meta
     *
     */
    @SuppressWarnings("unchecked")
    public Meta getMeta() {
        if (meta == null) {
            Object info = get("meta");
            if (info instanceof Map) {
                meta = Meta.parse((Map<String, Object>) info);
            }
        }
        return meta;
    }

    //
    //  Factories
    //

    public static MetaCommand query(ID identifier) {
        return new MetaCommand(identifier);
    }

    public static MetaCommand response(ID identifier, Meta meta) {
        return new MetaCommand(identifier, meta);
    }
}
