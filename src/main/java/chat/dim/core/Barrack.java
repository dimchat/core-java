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

import java.io.*;
import java.nio.charset.Charset;
import java.util.*;

public final class Barrack implements EntityDataSource, UserDataSource, GroupDataSource {

    private static Barrack ourInstance = new Barrack();

    public static Barrack getInstance() {
        return ourInstance;
    }

    private Barrack() {
    }

    // delegates
    public BarrackDelegate  delegate         = null;

    public EntityDataSource entityDataSource = null;
    public UserDataSource   userDataSource   = null;
    public GroupDataSource  groupDataSource  = null;

    // memory caches
    private Map<Address, Meta>    metaMap    = new HashMap<>();
    private Map<Address, Account> accountMap = new HashMap<>();
    private Map<Address, User>    userMap    = new HashMap<>();
    private Map<Address, Group>   groupMap   = new HashMap<>();

    // "/sdcard/chat.dim.sechat/.mkm/"
    public String metaDirectory = null;

    // "/sdcard/chat.dim.sechat/.mkm/{address}.meta"
    private File getMetaFile(ID identifier) throws FileNotFoundException {
        if (metaDirectory == null) {
            throw new FileNotFoundException("meta directory not set");
        }
        return new File(metaDirectory, identifier.address + ".meta");
    }

    // local storage
    private Meta loadMeta(ID identifier) throws IOException, ClassNotFoundException {
        File file = getMetaFile(identifier);
        if (!file.exists()) {
            // meta file not found
            return null;
        }
        // load from JsON file
        FileInputStream fis = new FileInputStream(file);
        byte[] data = new byte[fis.available()];
        fis.read(data);
        fis.close();
        String json = new String(data, Charset.forName("UTF-8"));
        return Meta.getInstance(JsON.decode(json));
    }

    public boolean saveMeta(Meta meta, ID identifier) throws IOException {
        // (a) check meta with ID
        if (!meta.matches(identifier)) {
            throw new IllegalArgumentException("meta not match");
        }
        metaMap.put(identifier.address, meta);

        // (b) check meta delegate
        if (delegate != null && delegate.saveMeta(meta, identifier)) {
            // saved by delegate
            return true;
        }

        // (c) save to local storage
        File file = getMetaFile(identifier);
        if (file.exists()) {
            // meta file exists, ignore it
            return false;
        }

        // save into JsON file
        FileOutputStream fos = new FileOutputStream(file);
        String json = JsON.encode(meta);
        fos.write(json.getBytes(Charset.forName("UTF-8")));
        fos.close();
        return true;
    }

    /**
     * Call it when received 'UIApplicationDidReceiveMemoryWarningNotification',
     * this will remove 50% of cached objects
     *
     * @return reduced object count
     */
    public int reduceMemory() {
        int finger = 0;
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

    private void addAccount(Account account) {
        if (account instanceof User) {
            addUser((User) account);
            return;
        }
        if (account.dataSource == null) {
            account.dataSource = this;
        }
        accountMap.put(account.identifier.address, account);
    }

    private void addUser(User user) {
        if (user.dataSource == null) {
            user.dataSource = this;
        }
        userMap.put(user.identifier.address, user);
    }

    private void addGroup(Group group) {
        if (group.dataSource == null) {
            group.dataSource = this;
        }
        groupMap.put(group.identifier.address, group);
    }

    public Account getAccount(ID identifier) {
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
        if (delegate != null) {
            account = delegate.getAccount(identifier);
            if (account != null) {
                addAccount(account);
                return account;
            }
        }
        // (d) create directly
        account = new Account(identifier);
        addAccount(account);
        return account;
    }

    public User getUser(ID identifier) {
        User user;
        // (a) get from user cache
        user = userMap.get(identifier.address);
        if (user != null) {
            return user;
        }
        // (b) get from delegate
        if (delegate != null) {
            user = delegate.getUser(identifier);
            if (user != null) {
                addUser(user);
                return user;
            }
        }
        // (c) create it directly
        user = new User(identifier);
        addUser(user);
        return user;
    }

    public Group getGroup(ID identifier) {
        Group group;
        // (a) get from group cache
        group = groupMap.get(identifier.address);
        if (group != null) {
            return group;
        }
        // (b) get from delegate
        if (delegate != null) {
            group = delegate.getGroup(identifier);
            if (group != null) {
                addGroup(group);
                return group;
            }
        }
        // (c) create directly
        // TODO: group type - polylogue/chatroom/...
        return null;
    }

    //-------- EntityDataSource

    @Override
    public Meta getMeta(ID identifier) {
        // (a) get from meta cache
        Meta meta = metaMap.get(identifier.address);
        if (meta != null) {
            return meta;
        }
        // (b) get from data source
        if (entityDataSource != null) {
            meta = entityDataSource.getMeta(identifier);
            if (meta!= null) {
                metaMap.put(identifier.address, meta);
                return meta;
            }
        }
        // (c) get from local storage
        try {
            meta = loadMeta(identifier);
            if (meta != null) {
                metaMap.put(identifier.address, meta);
                return meta;
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        // THROW: meta not found
        return null;
    }

    @Override
    public Profile getProfile(ID identifier) {
        if (entityDataSource == null) {
            throw new NullPointerException("entity data source not set");
        }
        return entityDataSource.getProfile(identifier);
    }

    //-------- UserDataSource

    @Override
    public PrivateKey getPrivateKey(int flag, User user) {
        if (userDataSource == null) {
            return null;
        }
        return userDataSource.getPrivateKey(flag, user);
    }

    @Override
    public List<Object> getContacts(User user) {
        if (userDataSource == null) {
            return null;
        }
        return userDataSource.getContacts(user);
    }

    @Override
    public int getCountOfContacts(User user) {
        if (userDataSource == null) {
            return 0;
        }
        return userDataSource.getCountOfContacts(user);
    }

    @Override
    public ID getContactAtIndex(int index, User user) {
        if (userDataSource == null) {
            return null;
        }
        return userDataSource.getContactAtIndex(index, user);
    }

    //-------- GroupDataSource

    @Override
    public ID getFounder(Group group) {
        if (groupDataSource == null) {
            return null;
        }
        // get from data source
        ID founder = groupDataSource.getFounder(group);
        if (founder != null) {
            return founder;
        }
        // check each member's public key with group meta
        Meta groupMeta = getMeta(group.identifier);
        if (groupMeta == null) {
            throw new NullPointerException("group meta not found:" + group.identifier);
        }
        ID member;
        Meta meta;
        int count = groupDataSource.getCountOfMembers(group);
        for (int index = 0; index < count; index++) {
            member = groupDataSource.getMemberAtIndex(index, group);
            meta = getMeta(member);
            if (meta == null) {
                continue;
            }
            if (groupMeta.matches(meta.key)) {
                // if public key matched, means the group is created by this member
                return member;
            }
        }
        return null;
    }

    @Override
    public ID getOwner(Group group) {
        if (groupDataSource == null) {
            return null;
        }
        return groupDataSource.getOwner(group);
    }

    @Override
    public List<Object> getMembers(Group group) {
        if (groupDataSource == null) {
            return null;
        }
        return groupDataSource.getMembers(group);
    }

    @Override
    public int getCountOfMembers(Group group) {
        if (groupDataSource == null) {
            return 0;
        }
        return groupDataSource.getCountOfMembers(group);
    }

    @Override
    public ID getMemberAtIndex(int index, Group group) {
        if (groupDataSource == null) {
            return null;
        }
        return groupDataSource.getMemberAtIndex(index, group);
    }
}
