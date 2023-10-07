/* license: https://mit-license.org
 *
 *  DIMP : Decentralized Instant Messaging Protocol
 *
 *                                Written in 2021 by Moky <albert.moky@gmail.com>
 *
 * ==============================================================================
 * The MIT License (MIT)
 *
 * Copyright (c) 2021 Albert Moky
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
package chat.dim;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import chat.dim.crypto.EncryptKey;
import chat.dim.crypto.VerifyKey;
import chat.dim.mkm.Entity;
import chat.dim.mkm.Group;
import chat.dim.mkm.User;
import chat.dim.protocol.Bulletin;
import chat.dim.protocol.Document;
import chat.dim.protocol.EntityType;
import chat.dim.protocol.ID;
import chat.dim.protocol.Meta;
import chat.dim.protocol.Visa;

/**
 *  Entity Database
 *  ~~~~~~~~~~~~~~~
 *  Entity pool to manage User/Contact/Group/Member instances
 *  Manage meta/document for all entities
 *
 *      1st, get instance here to avoid create same instance,
 *      2nd, if they were updated, we can refresh them immediately here
 */
public abstract class Barrack implements Entity.Delegate, User.DataSource, Group.DataSource {

    // memory caches
    private final Map<ID, User>   userMap = new HashMap<>();
    private final Map<ID, Group> groupMap = new HashMap<>();

    protected void cache(User user) {
        if (user.getDataSource() == null) {
            user.setDataSource(this);
        }
        userMap.put(user.getIdentifier(), user);
    }

    protected void cache(Group group) {
        if (group.getDataSource() == null) {
            group.setDataSource(this);
        }
        groupMap.put(group.getIdentifier(), group);
    }

    /**
     * Call it when received 'UIApplicationDidReceiveMemoryWarningNotification',
     * this will remove 50% of cached objects
     *
     * @return number of survivors
     */
    public int reduceMemory() {
        int finger = 0;
        finger = thanos(userMap, finger);
        finger = thanos(groupMap, finger);
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

    /**
     *  Create user when visa.key exists
     *
     * @param identifier - user ID
     * @return user, null on not ready
     */
    protected abstract User createUser(ID identifier);

    /**
     *  Create group when members exist
     *
     * @param identifier - group ID
     * @return group, null on not ready
     */
    protected abstract Group createGroup(ID identifier);

    protected EncryptKey getVisaKey(ID user) {
        Document doc = getDocument(user, Document.VISA);
        if (doc instanceof Visa/* && doc.isValid()*/) {
            return ((Visa) doc).getPublicKey();
        }
        return null;
    }
    protected VerifyKey getMetaKey(ID user) {
        Meta meta = getMeta(user);
        if (meta == null) {
            //throw new NullPointerException("failed to get meta for ID: " + user);
            return null;
        }
        return meta.getPublicKey();
    }

    //-------- Entity Delegate

    @Override
    public User getUser(ID identifier) {
        // 1. get from user cache
        User user = userMap.get(identifier);
        if (user == null) {
            // 2. create user and cache it
            user = createUser(identifier);
            if (user != null) {
                cache(user);
            }
        }
        return user;
    }

    @Override
    public Group getGroup(ID identifier) {
        // 1. get from group cache
        Group group = groupMap.get(identifier);
        if (group == null) {
            // 2. create group and cache it
            group = createGroup(identifier);
            if (group != null) {
                cache(group);
            }
        }
        return group;
    }

    //-------- User DataSource

    @Override
    public EncryptKey getPublicKeyForEncryption(ID user) {
        // 1. get key from visa
        EncryptKey visaKey = getVisaKey(user);
        if (visaKey != null) {
            // if visa.key exists, use it for encryption
            return visaKey;
        }
        // 2. get key from meta
        VerifyKey metaKey = getMetaKey(user);
        if (metaKey instanceof EncryptKey) {
            // if visa.key not exists and meta.key is encrypt key,
            // use it for encryption
            return (EncryptKey) metaKey;
        }
        //throw new NullPointerException("failed to get encrypt key for user: " + user);
        return null;
    }

    @Override
    public List<VerifyKey> getPublicKeysForVerification(ID user) {
        List<VerifyKey> keys = new ArrayList<>();
        // 1. get key from visa
        EncryptKey visaKey = getVisaKey(user);
        if (visaKey instanceof VerifyKey) {
            // the sender may use communication key to sign message.data,
            // so try to verify it with visa.key here
            keys.add((VerifyKey) visaKey);
        }
        // 2. get key from meta
        VerifyKey metaKey = getMetaKey(user);
        if (metaKey != null) {
            // the sender may use identity key to sign message.data,
            // try to verify it with meta.key
            keys.add(metaKey);
        }
        assert !keys.isEmpty() : "failed to get verify key for user: " + user;
        return keys;
    }

    //-------- Group DataSource

    @Override
    public ID getFounder(ID group) {
        // check broadcast group
        if (group.isBroadcast()) {
            // founder of broadcast group
            return getBroadcastFounder(group);
        }
        // get from document
        Document doc = getDocument(group, "*");
        if (doc instanceof Bulletin/* && doc.isValid()*/) {
            return ((Bulletin) doc).getFounder();
        }
        // TODO: load founder from database
        return null;
    }

    @Override
    public ID getOwner(ID group) {
        // check broadcast group
        if (group.isBroadcast()) {
            // owner of broadcast group
            return getBroadcastOwner(group);
        }
        // check group type
        if (EntityType.GROUP.equals(group.getType())) {
            // Polylogue's owner is its founder
            return getFounder(group);
        }
        // TODO: load owner from database
        return null;
    }

    @Override
    public List<ID> getMembers(ID group) {
        // check broadcast group
        if (group.isBroadcast()) {
            // members of broadcast group
            return getBroadcastMembers(group);
        }
        // TODO: load members from database
        return null;
    }

    @Override
    public List<ID> getAssistants(ID group) {
        Document doc = getDocument(group, Document.BULLETIN);
        if (doc instanceof Bulletin/* && doc.isValid()*/) {
            return ((Bulletin) doc).getAssistants();
        }
        // TODO: get group bots from SP configuration
        return null;
    }

    //-------- Broadcast Group

    private static String getGroupSeed(ID group) {
        String name = group.getName();
        if (name != null) {
            int len = name.length();
            if (len == 0 || name.equalsIgnoreCase("everyone")) {
                name = null;
            }
        }
        return name;
    }

    protected static ID getBroadcastFounder(ID group) {
        String name = getGroupSeed(group);
        if (name == null) {
            // Consensus: the founder of group 'everyone@everywhere'
            //            'Albert Moky'
            return ID.FOUNDER;
        } else {
            // DISCUSS: who should be the founder of group 'xxx@everywhere'?
            //          'anyone@anywhere', or 'xxx.founder@anywhere'
            return ID.parse(name + ".founder@anywhere");
        }
    }
    protected static ID getBroadcastOwner(ID group) {
        String name = getGroupSeed(group);
        if (name == null) {
            // Consensus: the owner of group 'everyone@everywhere'
            //            'anyone@anywhere'
            return ID.ANYONE;
        } else {
            // DISCUSS: who should be the owner of group 'xxx@everywhere'?
            //          'anyone@anywhere', or 'xxx.owner@anywhere'
            return ID.parse(name + ".owner@anywhere");
        }
    }
    protected static List<ID> getBroadcastMembers(ID group) {
        List<ID> members = new ArrayList<>();
        String name = getGroupSeed(group);
        if (name == null) {
            // Consensus: the member of group 'everyone@everywhere'
            //            'anyone@anywhere'
            members.add(ID.ANYONE);
        } else {
            // DISCUSS: who should be the member of group 'xxx@everywhere'?
            //          'anyone@anywhere', or 'xxx.member@anywhere'
            ID owner = ID.parse(name + ".owner@anywhere");
            ID member = ID.parse(name + ".member@anywhere");
            members.add(owner);
            members.add(member);
        }
        return members;
    }

}
