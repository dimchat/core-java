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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import chat.dim.EntityDelegate;
import chat.dim.Group;
import chat.dim.User;
import chat.dim.crypto.EncryptKey;
import chat.dim.crypto.VerifyKey;
import chat.dim.protocol.Bulletin;
import chat.dim.protocol.Document;
import chat.dim.protocol.ID;
import chat.dim.protocol.Meta;
import chat.dim.protocol.NetworkType;
import chat.dim.protocol.Visa;

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

    //-------- group membership

    public boolean isFounder(ID member, ID group) {
        // check member's public key with group's meta.key
        Meta gMeta = getMeta(group);
        assert gMeta != null : "failed to get meta for group: " + group;
        Meta mMeta = getMeta(member);
        assert mMeta != null : "failed to get meta for member: " + member;
        return gMeta.matches(mMeta.getKey());
    }

    public boolean isOwner(ID member, ID group) {
        if (NetworkType.Polylogue.equals(group.getType())) {
            return isFounder(member, group);
        }
        throw new UnsupportedOperationException("only Polylogue so far");
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
            List<ID> members = getMembers(receiver);
            if (members == null || members.size() == 0) {
                // TODO: group not ready, waiting for group info
                return null;
            }
            for (User item : users) {
                if (members.contains(item.identifier)) {
                    // DISCUSS: set this item to be current user?
                    return item;
                }
            }
        } else {
            // 1. personal message
            // 2. split group message
            for (User item : users) {
                if (receiver.equals(item.identifier)) {
                    // DISCUSS: set this item to be current user?
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

    private EncryptKey getVisaKey(ID user) {
        Document doc = getDocument(user, Document.VISA);
        if (doc instanceof Visa) {
            Visa visa = (Visa) doc;
            if (visa.isValid()) {
                return visa.getKey();
            }
        }
        return null;
    }
    private VerifyKey getMetaKey(ID user) {
        Meta meta = getMeta(user);
        assert meta != null : "failed to get meta for ID: " + user;
        return meta.getKey();
    }

    @Override
    public EncryptKey getPublicKeyForEncryption(ID user) {
        // 1. get key from visa
        EncryptKey visaKey = getVisaKey(user);
        if (visaKey != null) {
            return visaKey;
        }
        // 2. get key from meta
        VerifyKey metaKey = getMetaKey(user);
        if (metaKey instanceof EncryptKey) {
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
        assert keys.size() > 0 : "failed to get verify key for user: " + user;
        return keys;
    }

    //-------- Group DataSource

    @Override
    public ID getFounder(ID group) {
        // check broadcast group
        if (ID.isBroadcast(group)) {
            // founder of broadcast group
            String name = group.getName();
            int len = name == null ? 0 : name.length();
            if (len == 0 || (len == 8 && name.equalsIgnoreCase("everyone"))) {
                // Consensus: the founder of group 'everyone@everywhere'
                //            'Albert Moky'
                return ID.parse("moky@anywhere");
            } else {
                // DISCUSS: who should be the founder of group 'xxx@everywhere'?
                //          'anyone@anywhere', or 'xxx.founder@anywhere'
                return ID.parse(name + ".founder@anywhere");
            }
        }
        // check group meta
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
        // check broadcast group
        if (ID.isBroadcast(group)) {
            // owner of broadcast group
            String name = group.getName();
            int len = name == null ? 0 : name.length();
            if (len == 0 || (len == 8 && name.equalsIgnoreCase("everyone"))) {
                // Consensus: the owner of group 'everyone@everywhere'
                //            'anyone@anywhere'
                return ID.ANYONE;
            } else {
                // DISCUSS: who should be the owner of group 'xxx@everywhere'?
                //          'anyone@anywhere', or 'xxx.owner@anywhere'
                return ID.parse(name + ".owner@anywhere");
            }
        }
        // check group type
        if (NetworkType.Polylogue.equals(group.getType())) {
            // Polylogue's owner is its founder
            return getFounder(group);
        }
        // TODO: load owner from database
        return null;
    }

    @Override
    public List<ID> getMembers(ID group) {
        if (ID.isBroadcast(group)) {
            // members of broadcast group
            ID member;
            ID owner;
            String name = group.getName();
            int len = name == null ? 0 : name.length();
            if (len == 0 || (len == 8 && name.equalsIgnoreCase("everyone"))) {
                // Consensus: the member of group 'everyone@everywhere'
                //            'anyone@anywhere'
                member = ID.ANYONE;
                owner = ID.ANYONE;
            } else {
                // DISCUSS: who should be the member of group 'xxx@everywhere'?
                //          'anyone@anywhere', or 'xxx.member@anywhere'
                member = ID.parse(name + ".member@anywhere");
                owner = ID.parse(name + ".owner@anywhere");
            }
            assert owner != null : "failed to get owner of broadcast group";
            // add owner first
            List<ID> members = new ArrayList<>();
            members.add(owner);
            // check and add member
            if (!owner.equals(member)) {
                members.add(member);
            }
            return members;
        }
        // TODO: load members from database
        return null;
    }

    @Override
    public List<ID> getAssistants(ID group) {
        Document doc = getDocument(group, Document.BULLETIN);
        if (doc instanceof Bulletin) {
            return ((Bulletin) doc).getAssistants();
        }
        // TODO: get group bots from SP configuration
        return null;
    }
}
