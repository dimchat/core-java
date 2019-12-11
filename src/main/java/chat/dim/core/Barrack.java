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
package chat.dim.core;

import java.util.*;

import chat.dim.*;
import chat.dim.crypto.EncryptKey;
import chat.dim.crypto.VerifyKey;

/**
 *  Entity Database
 *  ~~~~~~~~~~~~~~~
 *  Manage meta for all entities
 */
public abstract class Barrack implements EntityDelegate, UserDataSource, GroupDataSource {

    // memory caches
    private Map<String, ID> idMap    = new HashMap<>();
    private Map<ID, Meta>   metaMap  = new HashMap<>();
    private Map<ID, User>   userMap  = new HashMap<>();
    private Map<ID, Group>  groupMap = new HashMap<>();

    protected Barrack() {
        super();
    }

    /**
     * Call it when received 'UIApplicationDidReceiveMemoryWarningNotification',
     * this will remove 50% of cached objects
     *
     * @return number of survivors
     */
    public int reduceMemory() {
        int finger = 0;
        finger = thanos(idMap, finger);
        finger = thanos(metaMap, finger);
        finger = thanos(userMap, finger);
        finger = thanos(groupMap, finger);
        return finger >> 1;
    }

    public static int thanos(Map map, int finger) {
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

    protected boolean cache(ID identifier) {
        assert identifier.isValid();
        idMap.put(identifier.toString(), identifier);
        return true;
    }

    protected boolean cache(Meta meta, ID identifier) {
        assert meta.matches(identifier);
        metaMap.put(identifier, meta);
        return true;
    }

    protected boolean cache(User user) {
        if (user.getDataSource() == null) {
            user.setDataSource(this);
        }
        userMap.put(user.identifier, user);
        return true;
    }

    protected boolean cache(Group group) {
        if (group.getDataSource() == null) {
            group.setDataSource(this);
        }
        groupMap.put(group.identifier, group);
        return true;
    }

    protected ID createID(String string) {
        assert string != null;
        return ID.getInstance(string);
    }

    protected User createUser(ID identifier) {
        assert identifier.getType().isUser();
        if (identifier.isBroadcast()) {
            // create user 'anyone@anywhere'
            return new User(identifier);
        }
        assert getMeta(identifier) != null;
        // TODO: check user type
        return new User(identifier);
    }

    protected Group createGroup(ID identifier) {
        assert identifier.getType().isGroup();
        if (identifier.isBroadcast()) {
            // create group 'everyone@everywhere'
            return new Group(identifier);
        }
        assert getMeta(identifier) != null;
        // TODO: check group type
        return new Group(identifier);
    }

    //-------- EntityDelegate

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
        identifier = createID((String) string);
        if (identifier != null && cache(identifier)) {
            return identifier;
        }
        // failed to create ID
        return null;
    }

    @Override
    public User getUser(ID identifier) {
        // 1. get from user cache
        User user = userMap.get(identifier);
        if (user != null) {
            return user;
        }
        // 2. create user and cache it
        user = createUser(identifier);
        if (user != null && cache(user)) {
            return user;
        }
        // failed to create User
        return null;
    }

    @Override
    public Group getGroup(ID identifier) {
        // 1. get from group cache
        Group group = groupMap.get(identifier);
        if (group != null) {
            return group;
        }
        // 2. create group and cache it
        group = createGroup(identifier);
        if (group != null && cache(group)) {
            return group;
        }
        // failed to create Group
        return null;
    }

    //-------- EntityDataSource

    @Override
    public Meta getMeta(ID identifier) {
        assert identifier.isValid();
        return metaMap.get(identifier);
    }

    //-------- UserDataSource

    @Override
    public EncryptKey getPublicKeyForEncryption(ID user) {
        // NOTICE: return nothing to use profile.key or meta.key
        return null;
    }

    @Override
    public List<VerifyKey> getPublicKeysForVerification(ID user) {
        // NOTICE: return nothing to use meta.key
        return null;
    }

    //-------- GroupDataSource

    @Override
    public ID getFounder(ID group) {
        assert group.getType().isGroup();
        // check for broadcast
        if (group.isBroadcast()) {
            String founder;
            String name = group.name;
            int len = name == null ? 0 : name.length();
            if (len == 0 || (len == 8 && name.equalsIgnoreCase("everyone"))) {
                // Consensus: the founder of group 'everyone@everywhere'
                //            'Albert Moky'
                founder = "moky@anywhere";
            } else {
                // DISCUSS: who should be the founder of group 'xxx@everywhere'?
                //          'anyone@anywhere', or 'xxx.founder@anywhere'
                founder = name + ".founder@anywhere";
            }
            return getID(founder);
        }
        return null;
    }

    @Override
    public ID getOwner(ID group) {
        assert group.getType().isGroup();
        // check for broadcast
        if (group.isBroadcast()) {
            String owner;
            String name = group.name;
            int len = name == null ? 0 : name.length();
            if (len == 0 || (len == 8 && name.equalsIgnoreCase("everyone"))) {
                // Consensus: the owner of group 'everyone@everywhere'
                //            'anyone@anywhere'
                owner = "anyone@anywhere";
            } else {
                // DISCUSS: who should be the owner of group 'xxx@everywhere'?
                //          'anyone@anywhere', or 'xxx.owner@anywhere'
                owner = name + ".owner@anywhere";
            }
            return getID(owner);
        }
        return null;
    }

    @Override
    public List<ID> getMembers(ID group) {
        assert group.getType().isGroup();
        // check for broadcast
        if (group.isBroadcast()) {
            String member;
            String name = group.name;
            int len = name == null ? 0 : name.length();
            if (len == 0 || (len == 8 && name.equalsIgnoreCase("everyone"))) {
                // Consensus: the member of group 'everyone@everywhere'
                //            'anyone@anywhere'
                member = "anyone@anywhere";
            } else {
                // DISCUSS: who should be the member of group 'xxx@everywhere'?
                //          'anyone@anywhere', or 'xxx.member@anywhere'
                member = name + ".member@anywhere";
            }
            // add owner first
            ID owner = getOwner(group);
            List<ID> members = new ArrayList<>();
            if (owner != null) {
                members.add(owner);
            }
            // check and add member
            ID identifier = getID(member);
            if (identifier != null && !identifier.equals(owner)) {
                members.add(identifier);
            }
            return members;
        }
        return null;
    }
}
