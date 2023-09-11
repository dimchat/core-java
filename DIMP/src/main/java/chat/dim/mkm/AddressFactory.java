/* license: https://mit-license.org
 *
 *  DIMP : Decentralized Instant Messaging Protocol
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
import java.util.Iterator;
import java.util.Map;

import chat.dim.protocol.Address;
import chat.dim.protocol.Meta;

/**
 *  Base Address Factory
 *  ~~~~~~~~~~~~~~~~~~~~
 */
public abstract class AddressFactory implements Address.Factory {

    private final Map<String, Address> addresses = new HashMap<>();

    /**
     * Call it when received 'UIApplicationDidReceiveMemoryWarningNotification',
     * this will remove 50% of cached objects
     *
     * @return number of survivors
     */
    public int reduceMemory() {
        int finger = 0;
        finger = thanos(addresses, finger);
        return finger >> 1;
    }

    /**
     *  Thanos can kill half lives of a world with a snap of the finger
     */
    public static <K, V> int thanos(Map<K, V> planet, int finger) {
        Iterator<Map.Entry<K, V>> people = planet.entrySet().iterator();
        while (people.hasNext()) {
            people.next();
            if ((++finger & 1) == 1) {
                // kill it
                people.remove();
            }
            // let it go
        }
        return finger;
    }

    @Override
    public Address generateAddress(Meta meta, int network) {
        Address address = meta.generateAddress(network);
        if (address != null) {
            addresses.put(address.toString(), address);
        }
        return address;
    }

    @Override
    public Address parseAddress(String address) {
        Address add = addresses.get(address);
        if (add == null) {
            add = Address.create(address);
            if (add != null) {
                addresses.put(address, add);
            }
        }
        return add;
    }
}
