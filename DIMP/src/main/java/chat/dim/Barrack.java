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
import chat.dim.mkm.BroadcastHelper;
import chat.dim.mkm.DocumentHelper;
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
        Visa doc = getVisa(user);
        if (doc != null/* && doc.isValid()*/) {
            return doc.getPublicKey();
        }
        return null;
    }
    protected VerifyKey getMetaKey(ID user) {
        Meta meta = getMeta(user);
        if (meta != null/* && meta.isValid()*/) {
            return meta.getPublicKey();
        }
        //throw new NullPointerException("failed to get meta for ID: " + user);
        return null;
    }

    public Visa getVisa(ID user) {
        // assert user.isUser() : "user ID error: " + user;
        List<Document> documents = getDocuments(user);
        return DocumentHelper.lastVisa(documents);
    }
    public Bulletin getBulletin(ID group) {
        // assert group.isGroup() : "group ID error: " + group;
        List<Document> documents = getDocuments(group);
        return DocumentHelper.lastBulletin(documents);
    }

    //-------- Entity Delegate

    @Override
    public User getUser(ID identifier) {
        assert identifier.isUser() : "user ID error: " + identifier;
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
        assert identifier.isGroup() : "group ID error: " + identifier;
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
        assert user.isUser() : "user ID error: " + user;
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
        // assert user.isUser() : "user ID error: " + user;
        List<VerifyKey> keys = new ArrayList<>();
        // 1. get key from meta
        VerifyKey metaKey = getMetaKey(user);
        if (metaKey != null) {
            // the sender may use identity key to sign message.data,
            // try to verify it with meta.key
            keys.add(metaKey);
        }
        // 2. get key from visa
        EncryptKey visaKey = getVisaKey(user);
        if (visaKey instanceof VerifyKey) {
            // the sender may use communication key to sign message.data,
            // so try to verify it with visa.key here
            keys.add((VerifyKey) visaKey);
        }
        assert !keys.isEmpty() : "failed to get verify key for user: " + user;
        return keys;
    }

    //-------- Group DataSource

    @Override
    public ID getFounder(ID group) {
        assert group.isGroup() : "group ID error: " + group;
        // check broadcast group
        if (group.isBroadcast()) {
            // founder of broadcast group
            return BroadcastHelper.getBroadcastFounder(group);
        }
        // get from document
        Bulletin doc = getBulletin(group);
        if (doc != null/* && doc.isValid()*/) {
            return doc.getFounder();
        }
        // TODO: load founder from database
        return null;
    }

    @Override
    public ID getOwner(ID group) {
        assert group.isGroup() : "group ID error: " + group;
        // check broadcast group
        if (group.isBroadcast()) {
            // owner of broadcast group
            return BroadcastHelper.getBroadcastOwner(group);
        }
        // check group type
        if (EntityType.GROUP.equals(group.getType())) {
            // Polylogue owner is its founder
            return getFounder(group);
        }
        // TODO: load owner from database
        return null;
    }

    @Override
    public List<ID> getMembers(ID group) {
        assert group.isGroup() : "group ID error: " + group;
        // check broadcast group
        if (group.isBroadcast()) {
            // members of broadcast group
            return BroadcastHelper.getBroadcastMembers(group);
        }
        // TODO: load members from database
        return null;
    }

    @Override
    public List<ID> getAssistants(ID group) {
        assert group.isGroup() : "group ID error: " + group;
        // get from document
        Bulletin doc = getBulletin(group);
        if (doc != null/* && doc.isValid()*/) {
            return doc.getAssistants();
        }
        // TODO: get group bots from SP configuration
        return null;
    }

}
