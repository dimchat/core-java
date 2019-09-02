/* license: https://mit-license.org
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
package chat.dim.core;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import chat.dim.mkm.*;

/**
 *  Entity Database
 *  ~~~~~~~~~~~~~~~
 *  Manage meta for all entities
 */
public abstract class Barrack implements SocialNetworkDataSource, UserDataSource, GroupDataSource {

    // memory caches
    private Map<String, ID> idMap    = new HashMap<>();
    private Map<ID, Meta>   metaMap  = new HashMap<>();
    private Map<ID, User>   userMap  = new HashMap<>();
    private Map<ID, Group>  groupMap = new HashMap<>();

    public Barrack() {
        super();
    }

    /**
     * Call it when received 'UIApplicationDidReceiveMemoryWarningNotification',
     * this will remove 50% of cached objects
     *
     * @return remained object count
     */
    public int reduceMemory() {
        int finger = 0;
        finger = thanos(idMap, finger);
        finger = thanos(metaMap, finger);
        finger = thanos(userMap, finger);
        finger = thanos(groupMap, finger);
        return finger >> 1;
    }

    protected int thanos(Map map, int finger) {
        Iterator iterator = map.entrySet().iterator();
        while (iterator.hasNext()) {
            iterator.next();
            if ((++finger & 1) == 1) {
                // kill it
                iterator.remove();
            }
            // let it go
        }
        return finger;
    }

    protected boolean cacheID(ID identifier) {
        assert identifier.isValid();
        idMap.put(identifier.toString(), identifier);
        return true;
    }

    protected boolean cacheMeta(Meta meta, ID identifier) {
        if (!meta.matches(identifier)) {
            return false;
        }
        metaMap.put(identifier, meta);
        return true;
    }

    protected boolean cacheUser(User user) {
        assert user.identifier.getType().isUser();
        if (user.dataSource == null) {
            user.dataSource = this;
        }
        userMap.put(user.identifier, user);
        return true;
    }

    protected boolean cacheGroup(Group group) {
        assert group.identifier.getType().isGroup();
        if (group.dataSource == null) {
            group.dataSource = this;
        }
        groupMap.put(group.identifier, group);
        return true;
    }

    //-------- SocialNetworkDataSource

    @Override
    public ID getID(Object string) {
        if (string == null) {
            return null;
        } else if (string instanceof ID) {
            return (ID) string;
        }
        assert string instanceof String;
        // 1. get from ID cache
        ID identifier = idMap.get(string);
        if (identifier != null) {
            return identifier;
        }
        // 2. create and cache it
        identifier = ID.getInstance(string);
        if (identifier != null && cacheID(identifier)) {
            return identifier;
        }
        // failed to create ID
        return null;
    }

    @Override
    public User getUser(ID identifier) {
        assert identifier.getType().isUser();
        return userMap.get(identifier);
    }

    @Override
    public Group getGroup(ID identifier) {
        assert identifier.getType().isGroup();
        return groupMap.get(identifier);
    }

    //-------- EntityDataSource

    @Override
    public Meta getMeta(ID identifier) {
        assert identifier.isValid();
        return metaMap.get(identifier);
    }
}
