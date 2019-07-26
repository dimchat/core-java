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

import chat.dim.crypto.PrivateKey;
import chat.dim.mkm.*;
import chat.dim.mkm.entity.*;

import java.util.*;

public class Barrack implements BarrackDelegate, EntityDataSource, UserDataSource, GroupDataSource {

    // delegates
    public BarrackDelegate  delegate         = null;
    public EntityDataSource entityDataSource = null;
    public UserDataSource   userDataSource   = null;
    public GroupDataSource  groupDataSource  = null;

    // memory caches
    private Map<String, ID>  idMap      = new HashMap<>();
    private Map<ID, Meta>    metaMap    = new HashMap<>();
    private Map<ID, Account> accountMap = new HashMap<>();
    private Map<ID, User>    userMap    = new HashMap<>();
    private Map<ID, Group>   groupMap   = new HashMap<>();

    public Barrack() {
        super();
    }

    /**
     * Call it when received 'UIApplicationDidReceiveMemoryWarningNotification',
     * this will remove 50% of cached objects
     *
     * @return reduced object count
     */
    public int reduceMemory() {
        int finger = 0;
        finger = thanos(idMap, finger);
        finger = thanos(metaMap, finger);
        finger = thanos(accountMap, finger);
        finger = thanos(userMap, finger);
        finger = thanos(groupMap, finger);
        return finger >> 1;
    }

    private int thanos(Map map, int finger) {
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

    private boolean cacheID(ID identifier) {
        assert identifier.isValid();
        idMap.put(identifier.toString(), identifier);
        return true;
    }

    private boolean cacheMeta(Meta meta, ID identifier) {
        if (meta.matches(identifier)) {
            metaMap.put(identifier, meta);
            return true;
        }
        return false;
    }

    private boolean cacheAccount(Account account) {
        if (account instanceof User) {
            return cacheUser((User) account);
        }
        if (account.dataSource == null) {
            account.dataSource = this;
        }
        assert account.identifier.isValid();
        accountMap.put(account.identifier, account);
        return true;
    }

    private boolean cacheUser(User user) {
        accountMap.remove(user.identifier);
        if (user.dataSource == null) {
            user.dataSource = this;
        }
        assert user.identifier.isValid();
        userMap.put(user.identifier, user);
        return true;
    }

    private boolean cacheGroup(Group group) {
        if (group.dataSource == null) {
            group.dataSource = this;
        }
        assert group.identifier.isValid();
        groupMap.put(group.identifier, group);
        return true;
    }

    //-------- BarrackDelegate

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
        // 2. get from delegate
        identifier = delegate.getID(string);
        if (identifier == null) {
            // create it directly
            identifier = ID.getInstance(string);
        }
        // 3. cache it
        if (identifier != null && cacheID(identifier)) {
            return identifier;
        }
        // failed to create ID
        return identifier;
    }

    public Account getAccount(ID identifier) {
        if (identifier == null) {
            return null;
        }
        // 1. get from account cache
        Account account = accountMap.get(identifier);
        if (account != null) {
            return account;
        }
        // 2. get from user cache
        account = userMap.get(identifier);
        if (account != null) {
            return account;
        }
        // 3. get from delegate
        account = delegate.getAccount(identifier);
        if (account != null && cacheAccount(account)) {
            return account;
        }
        // failed to get account
        return null;
    }

    public User getUser(ID identifier) {
        if (identifier == null) {
            return null;
        }
        // 1. get from user cache
        User user = userMap.get(identifier);
        if (user != null) {
            return user;
        }
        // 2. get from delegate
        user = delegate.getUser(identifier);
        if (user != null && cacheUser(user)) {
            return user;
        }
        // failed to get user
        return null;
    }

    public Group getGroup(ID identifier) {
        if (identifier == null) {
            return null;
        }
        // 1. get from group cache
        Group group = groupMap.get(identifier);
        if (group != null) {
            return group;
        }
        // 2. get from delegate
        group = delegate.getGroup(identifier);
        if (group != null && cacheGroup(group)) {
            return group;
        }
        // failed to get group
        return null;
    }

    //-------- EntityDataSource

    @Override
    public Meta getMeta(ID identifier) {
        if (identifier == null) {
            return null;
        }
        // 1. get from meta cache
        Meta meta = metaMap.get(identifier);
        if (meta != null) {
            return meta;
        }
        // 2. get from entity data source
        meta = entityDataSource.getMeta(identifier);
        if (meta != null && cacheMeta(meta, identifier)) {
            return meta;
        }
        // failed to get meta
        assert meta == null;
        return null;
    }

    @Override
    public boolean saveMeta(Meta meta, ID identifier) {
        // 1. check meta with ID
        if (!cacheMeta(meta, identifier)) {
            throw new IllegalArgumentException("meta not match ID: " + identifier + ", " + meta);
        }
        // 2. save by delegate
        return entityDataSource.saveMeta(meta, identifier);
    }

    @Override
    public Profile getProfile(ID identifier) {
        if (identifier == null) {
            return null;
        }
        return entityDataSource.getProfile(identifier);
    }

    //-------- UserDataSource

    @Override
    public PrivateKey getPrivateKeyForSignature(ID user) {
        if (user == null) {
            return null;
        }
        return userDataSource.getPrivateKeyForSignature(user);
    }

    @Override
    public List<PrivateKey> getPrivateKeysForDecryption(ID user) {
        if (user == null) {
            return null;
        }
        return userDataSource.getPrivateKeysForDecryption(user);
    }

    @Override
    public List<ID> getContacts(ID user) {
        if (user == null) {
            return null;
        }
        return userDataSource.getContacts(user);
    }

    //-------- GroupDataSource

    @Override
    public ID getFounder(ID group) {
        if (group == null) {
            return null;
        }
        // get from data source
        ID founder = groupDataSource.getFounder(group);
        if (founder != null) {
            return founder;
        }
        // check each member's public key with group meta
        Meta gMeta = getMeta(group);
        List<ID> members = groupDataSource.getMembers(group);
        if (gMeta == null || members == null) {
            //throw new NullPointerException("failed to get group info: " + gMeta + ", " + members);
            return null;
        }
        for (ID member : members) {
            Meta meta = getMeta(member);
            if (meta == null) {
                // TODO: query meta for this member from DIM network
                continue;
            }
            if (gMeta.matches(meta.key)) {
                // if public key matched, means the group is created by this member
                return member;
            }
        }
        return null;
    }

    @Override
    public ID getOwner(ID group) {
        if (group == null) {
            return null;
        }
        return groupDataSource.getOwner(group);
    }

    @Override
    public List<ID> getMembers(ID group) {
        if (group == null) {
            return null;
        }
        return groupDataSource.getMembers(group);
    }
}
