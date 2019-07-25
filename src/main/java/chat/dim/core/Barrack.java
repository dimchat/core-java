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
    private Map<String, ID>       idMap      = new HashMap<>();
    private Map<Address, Meta>    metaMap    = new HashMap<>();
    private Map<Address, Account> accountMap = new HashMap<>();
    private Map<Address, User>    userMap    = new HashMap<>();
    private Map<Address, Group>   groupMap   = new HashMap<>();

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
        return (finger & 1) + (finger >> 1);
    }

    private int thanos(Map map, int finger) {
        Iterator iterator = map.entrySet().iterator();
        while (iterator.hasNext()) {
            iterator.next();
            if ((++finger & 1) == 0) {
                // let it go
                continue;
            }
            // kill it
            iterator.remove();
        }
        return finger;
    }

    private boolean cacheID(ID identifier) {
        if (identifier.isValid()) {
            idMap.put(identifier.toString(), identifier);
            return true;
        }
        return false;
    }

    private boolean cacheMeta(Meta meta, ID identifier) {
        if (meta.matches(identifier)) {
            metaMap.put(identifier.address, meta);
            return true;
        }
        return false;
    }

    private boolean cacheAccount(Account account) {
        if (account instanceof User) {
            return cacheUser((User) account);
        }
        Address address = account.identifier.address;
        if (address == null) {
            return false;
        }
        if (account.dataSource == null) {
            account.dataSource = this;
        }
        accountMap.put(address, account);
        return true;
    }

    private boolean cacheUser(User user) {
        Address address = user.identifier.address;
        if (address == null) {
            return false;
        }
        accountMap.remove(address);
        if (user.dataSource == null) {
            user.dataSource = this;
        }
        userMap.put(address, user);
        return true;
    }

    private boolean cacheGroup(Group group) {
        Address address = group.identifier.address;
        if (address == null) {
            return false;
        }
        if (group.dataSource == null) {
            group.dataSource = this;
        }
        groupMap.put(group.identifier.address, group);
        return true;
    }

    //-------- BarrackDelegate

    public ID getID(Object identifier) {
        if (identifier == null) {
            return null;
        } else if (identifier instanceof ID) {
            return (ID) identifier;
        }
        assert identifier instanceof String;
        // (a) get from ID cache
        ID id = idMap.get(identifier);
        if (id != null) {
            return id;
        }
        // (b) get from delegate
        id = delegate.getID(identifier);
        if (id != null && cacheID(id)) {
            return id;
        }
        id = ID.getInstance(identifier);
        if (id != null && cacheID(id)) {
            return id;
        }
        // failed to create ID
        return id;
    }

    public Account getAccount(ID identifier) {
        if (identifier == null) {
            return null;
        }
        Account account;
        // (a) get from account cache
        account = accountMap.get(identifier.address);
        if (account != null) {
            return account;
        }
        // (b) get from user cache
        account = userMap.get(identifier.address);
        if (account != null) {
            return account;
        }
        // (c) get from delegate
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
        User user;
        // (a) get from user cache
        user = userMap.get(identifier.address);
        if (user != null) {
            return user;
        }
        // (b) get from delegate
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
        Group group;
        // (a) get from group cache
        group = groupMap.get(identifier.address);
        if (group != null) {
            return group;
        }
        // (b) get from delegate
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
        // (a) get from meta cache
        Meta meta = metaMap.get(identifier.address);
        if (meta != null) {
            return meta;
        }
        // (b) get from entity data source
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
        // (a) check meta with ID
        if (!cacheMeta(meta, identifier)) {
            throw new IllegalArgumentException("meta not match ID: " + identifier + ", " + meta);
        }
        // (b) save by delegate
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
