package chat.dim.core;

import chat.dim.crypto.PrivateKey;
import chat.dim.crypto.PublicKey;
import chat.dim.mkm.*;
import chat.dim.mkm.entity.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class Barrack implements MetaDataSource, EntityDataSource, UserDataSource, GroupDataSource {

    private static Barrack ourInstance = new Barrack();

    public static Barrack getInstance() {
        return ourInstance;
    }

    private Barrack() {
    }

    // delegates
    public BarrackDelegate  delegate         = null;

    public MetaDataSource   metaDataSource   = null;
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
        String json = new String(data, StandardCharsets.UTF_8);
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
        fos.write(json.getBytes(StandardCharsets.UTF_8));
        fos.close();
        return true;
    }

    public PublicKey getPublicKey(ID identifier) {
        Meta meta = getInstance().getMeta(identifier);
        if (meta == null) {
            return null;
        } else {
            return meta.key;
        }
    }

    /**
     * Call it when receive 'UIApplicationDidReceiveMemoryWarningNotification',
     * this will remove 50% of unused objects from the cache
     */
    public int reduceMemory() {
        int count = 0;
        count += reduceTable(metaMap);
        count += reduceTable(accountMap);
        count += reduceTable(userMap);
        count += reduceTable(groupMap);
        return count;
    }

    private int reduceTable(Map map) {
        Set keys = map.keySet();
        int index = 0;
        int count = 0;
        for (Object key : keys) {
            if (index % 2 > 0) {
                continue;
            }
            map.remove(key);
            ++index;
            ++count;
        }
        return count;
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

    //-------- IMetaDataSource

    @Override
    public Meta getMeta(ID identifier) {
        Meta meta;
        // (a) get from meta cache
        meta = metaMap.get(identifier.address);
        if (meta != null) {
            return meta;
        }
        if (metaDataSource != null) {
            // (b) get from meta data source
            meta = metaDataSource.getMeta(identifier);
            if (meta != null && meta.matches(identifier)) {
                metaMap.put(identifier.address, meta);
                return meta;
            }
        }
        // (c) get from local storage
        try {
            meta = loadMeta(identifier);
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        if (meta != null && meta.matches(identifier)) {
            metaMap.put(identifier.address, meta);
            return meta;
        }
        // THROW: meta not found
        return null;
    }

    //-------- IEntityDataSource

    @Override
    public Meta getMeta(Entity entity) {
        Meta meta;
        ID identifier = entity.identifier;
        // (a) call 'metaForID:' of meta data source
        meta = getMeta(identifier);
        if (meta != null) {
            return meta;
        }
        // (b) check entity data source
        if (entityDataSource == null) {
            // entity data source not set
            return null;
        }
        // (c) get from entity data source
        meta = entityDataSource.getMeta(entity);
        if (meta != null && meta.matches(identifier)) {
            metaMap.put(identifier.address, meta);
            return meta;
        }
        // THROW: meta not found
        return null;
    }

    @Override
    public Profile getProfile(Entity entity) {
        if (entityDataSource == null) {
            return null;
        }
        return entityDataSource.getProfile(entity);
    }

    @Override
    public String getName(Entity entity) {
        String name = null;
        do {
            if (entityDataSource == null) {
                // entity data source not set
                break;
            }
            // (a) get from entity data source
            name = entityDataSource.getName(entity);
            if (name != null && name.length() > 0) {
                break;
            }
            // (b) get from profile
            Profile profile = entityDataSource.getProfile(entity);
            if (profile != null) {
                name = profile.getName();
            }
            break;
        } while (true);
        return name;
    }

    //-------- IUserDataSource

    @Override
    public PrivateKey getPrivateKey(User user) {
        if (userDataSource == null) {
            return null;
        }
        return userDataSource.getPrivateKey(user);
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

    //-------- IGroupDataSource

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
        Meta groupMeta = getMeta(group);
        if (groupMeta == null) {
            throw new NullPointerException("group meta not found:" + group.identifier);
        }
        ID member;
        PublicKey publicKey;
        int count = groupDataSource.getCountOfMembers(group);
        for (int index = 0; index < count; index++) {
            member = groupDataSource.getMemberAtIndex(index, group);
            publicKey = getPublicKey(member);
            if (publicKey == null) {
                continue;
            }
            if (groupMeta.matches(publicKey)) {
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
