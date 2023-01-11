/* license: https://mit-license.org
 *
 *  Ming-Ke-Ming : Decentralized User Identity Authentication
 *
 *                                Written in 2020 by Moky <albert.moky@gmail.com>
 *
 * ==============================================================================
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 Albert Moky
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
package chat.dim.mkm;

import java.util.HashMap;
import java.util.Map;

import chat.dim.protocol.Address;
import chat.dim.protocol.ID;
import chat.dim.protocol.Meta;

public class IDFactory implements ID.Factory {

    private final Map<String, ID> identifiers = new HashMap<>();

    /**
     * Call it when received 'UIApplicationDidReceiveMemoryWarningNotification',
     * this will remove 50% of cached objects
     *
     * @return number of survivors
     */
    public int reduceMemory() {
        int finger = 0;
        finger = AddressFactory.thanos(identifiers, finger);
        return finger >> 1;
    }

    @Override
    public ID generateID(Meta meta, int network, String terminal) {
        Address address = Address.generate(meta, network);
        assert address != null : "failed to generate ID with meta: " + meta.toMap();
        return ID.create(meta.getSeed(), address, terminal);
    }

    @Override
    public ID createID(String name, Address address, String terminal) {
        String identifier = concat(name, address, terminal);
        ID id = identifiers.get(identifier);
        if (id == null) {
            id = newID(identifier, name, address, terminal);
            identifiers.put(identifier, id);
        }
        return id;
    }

    @Override
    public ID parseID(String identifier) {
        ID id = identifiers.get(identifier);
        if (id == null) {
            id = parse(identifier);
            if (id != null) {
                identifiers.put(identifier, id);
            }
        }
        return id;
    }

    // override for customized ID
    protected ID newID(String identifier, String name, Address address, String terminal) {
        return new Identifier(identifier, name, address, terminal);
    }

    private String concat(String name, Address address, String terminal) {
        String string = address.toString();
        if (name != null && name.length() > 0) {
            string = name + "@" + string;
        }
        if (terminal != null && terminal.length() > 0) {
            string = string + "/" + terminal;
        }
        return string;
    }

    private ID parse(final String string) {
        String name;
        Address address;
        String terminal;
        // split ID string
        String[] pair = string.split("/");
        // terminal
        if (pair.length == 1) {
            // no terminal
            terminal = null;
        } else {
            // got terminal
            assert pair.length == 2 : "ID error: " + string;
            assert pair[1].length() > 0 : "ID.terminal error: " + string;
            terminal = pair[1];
        }
        // name @ address
        assert pair[0].length() > 0 : "ID error: " + string;
        pair = pair[0].split("@");
        assert pair[0].length() > 0 : "ID error: " + string;
        if (pair.length == 1) {
            // got address without name
            name = null;
            address = Address.parse(pair[0]);
        } else {
            // got name & address
            assert pair.length == 2 : "ID error: " + string;
            assert pair[1].length() > 0 : "ID.address error: " + string;
            name = pair[0];
            address = Address.parse(pair[1]);
        }
        if (address == null) {
            return null;
        }
        return newID(string, name, address, terminal);
    }
}
