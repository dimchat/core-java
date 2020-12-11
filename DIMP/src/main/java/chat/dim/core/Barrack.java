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

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import chat.dim.EntityDelegate;
import chat.dim.Group;
import chat.dim.User;
import chat.dim.crypto.VerifyKey;
import chat.dim.protocol.ID;
import chat.dim.protocol.Meta;
import chat.dim.protocol.NetworkType;

/**
 *  Entity Database
 *  ~~~~~~~~~~~~~~~
 *  Manage meta for all entities
 */
public abstract class Barrack implements EntityDelegate, User.DataSource, Group.DataSource {

    // memory caches
    private Map<ID, User>  userMap  = new HashMap<>();
    private Map<ID, Group> groupMap = new HashMap<>();

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

    private void cache(User user) {
        if (user.getDataSource() == null) {
            user.setDataSource(this);
        }
        userMap.put(user.identifier, user);
    }

    private void cache(Group group) {
        if (group.getDataSource() == null) {
            group.setDataSource(this);
        }
        groupMap.put(group.identifier, group);
    }

    protected abstract User createUser(ID identifier);

    protected abstract Group createGroup(ID identifier);

    //-------- group

    public boolean isFounder(ID member, ID group) {
        // check member's public key with group's meta.key
        Meta gMeta = getMeta(group);
        if (gMeta == null) {
            throw new NullPointerException("failed to get meta for group: " + group);
        }
        Meta mMeta = getMeta(member);
        if (mMeta == null) {
            throw new NullPointerException("failed to get meta for member: " + member);
        }
        return gMeta.matches(mMeta.getKey());
    }

    public boolean isOwner(ID member, ID group) {
        if (NetworkType.Polylogue.equals(group.getType())) {
            return isFounder(member, group);
        }
        throw new UnsupportedOperationException("only Polylogue so far");
    }

    public boolean containsMember(ID member, ID group) {
        List<ID> members = getMembers(group);
        if (members != null && members.contains(member)) {
            return true;
        }
        ID owner = getOwner(group);
        return owner != null && owner.equals(member);
    }

    /**
     *  Get assistants for this group
     *
     * @param group - group ID
     * @return robot ID list
     */
    public abstract List<ID> getAssistants(ID group);

    public boolean containsAssistant(ID user, ID group) {
        List<ID> assistants = getAssistants(group);
        if (assistants == null) {
            return false;
        }
        return assistants.contains(user);
    }

    //-------- EntityDelegate

    @Override
    public User selectLocalUser(ID receiver) {
        List<User> users = getLocalUsers();
        if (users == null || users.size() == 0) {
            throw new NullPointerException("local users should not be empty");
        } else if (ID.isBroadcast(receiver)) {
            // broadcast message can decrypt by anyone, so just return current user
            return users.get(0);
        }
        if (ID.isGroup(receiver)) {
            // group message (recipient not designated)
            for (User item : users) {
                if (containsMember(item.identifier, receiver)) {
                    // set this item to be current user?
                    return item;
                }
            }
        } else {
            // 1. personal message
            // 2. split group message
            for (User item : users) {
                if (receiver.equals(item.identifier)) {
                    // set this item to be current user?
                    return item;
                }
            }
        }
        return null;
    }

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
    public List<VerifyKey> getPublicKeysForVerification(ID user) {
        // return null to user [visa.key, meta.key]
        return null;
    }

    //-------- Group DataSource

    @Override
    public ID getFounder(ID group) {
        Meta gMeta = getMeta(group);
        if (gMeta == null) {
            // FIXME: when group profile was arrived but the meta still on the way,
            //        here will cause founder not found
            return null;
        }
        // check each member's public key with group meta
        List<ID> members = getMembers(group);
        if (members != null) {
            Meta mMeta;
            for (ID item : members) {
                mMeta = getMeta(item);
                if (mMeta == null) {
                    // failed to get member meta
                    continue;
                }
                if (gMeta.matches(mMeta.getKey())) {
                    // if the member's public key matches with the group's meta,
                    // it means this meta was generated by the member's private key
                    return item;
                }
            }
        }
        // TODO: load founder from database
        return null;
    }

    @Override
    public ID getOwner(ID group) {
        // check group type
        if (NetworkType.Polylogue.equals(group.getType())) {
            // Polylogue's owner is its founder
            return getFounder(group);
        }
        // TODO: load owner from database
        return null;
    }
}
