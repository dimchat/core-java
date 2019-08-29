# Decentralized Instant Messaging Protocol (Java)

[![license](https://img.shields.io/github/license/mashape/apistatus.svg)](https://github.com/dimchat/core-java/blob/master/LICENSE)
[![Version](https://img.shields.io/badge/alpha-0.4.5-red.svg)](https://github.com/dimchat/core-java/archive/master.zip)
[![PRs Welcome](https://img.shields.io/badge/PRs-welcome-brightgreen.svg)](https://github.com/dimchat/core-java/pulls)
[![Platform](https://img.shields.io/badge/Platform-Java%208-brightgreen.svg)](https://github.com/dimchat/core-java/wiki)

## Talk is cheap, show you the codes!

### Dependencies

build.gradle

```javascript
allprojects {
    repositories {
        jcenter()
        mavenCentral()
    }
}

dependencies {

    // https://bintray.com/dimchat/core/dimp
    compile 'chat.dim:DIMP:0.4.5'
//  implementation group: 'chat.dim', name: 'DIMP', version: '0.4.5'

}
```

pom.xml

```xml
<dependencies>

    <!-- https://mvnrepository.com/artifact/chat.dim/DIMP -->
    <dependency>
        <groupId>chat.dim</groupId>
        <artifactId>DIMP</artifactId>
        <version>0.4.5</version>
        <type>pom</type>
    </dependency>

</dependencies>
```

### Common Extensions

Facebook.java

```java
/**
 *  Access database to load/save user's private key, meta and profiles
 */
public class Facebook extends Barrack {
    private static final Facebook ourInstance = new Facebook();
    public static Facebook getInstance() { return ourInstance; }
    
    private Facebook() {
        super();
        //...
    }
    
    public boolean savePrivateKey(PrivateKey privateKey, ID identifier) {
        // TODO: save private key into safety storage
        return false;
    }
    
    public boolean saveMeta(Meta meta, ID identifier) {
        if (!meta.matches(identifier)) {
            return false;
        }
        // TODO: save meta to local/persistent storage
        return false;
    }
    
    public boolean saveProfile(Profile profile) {
        if (!verifyProfile(profile)) {
            return false;
        }
        // TODO: save profile in local storage
        return false;
    }
    
    private boolean verifyProfile(Profile profile) {
        if (profile == null) {
            return false;
        } else if (profile.isValid()) {
            return true;
        }
        ID identifier = profile.identifier;
        assert identifier.isValid();
        NetworkType type = identifier.getType();
        Meta meta = null;
        if (type.isUser()) {
            // verify with user's meta.key
            meta = getMeta(identifier);
        } else if (type.isGroup()) {
            // verify with group owner's meta.key
            Group group = getGroup(identifier);
            if (group != null) {
                meta = getMeta(group.getOwner());
            }
        }
        return meta != null && profile.verify(meta.key);
    }
    
    //-------- SocialNetworkDataSource

    @Override
    public User getUser(ID identifier) {
        User user = super.getUser(identifier);
        if (user != null) {
            return user;
        }
        // check meta and private key
        Meta meta = getMeta(identifier);
        if (meta == null) {
            throw new NullPointerException("meta not found: " + identifier);
        }
        NetworkType type = identifier.getType();
        if (type.isPerson()) {
            PrivateKey key = getPrivateKeyForSignature(identifier);
            if (key == null) {
                user = new User(identifier);
            } else {
                user = new LocalUser(identifier);
            }
        } else if (type.isStation()) {
            // FIXME: prevent station to be erased from memory cache
            user = new Station(identifier);
        } else {
            throw new UnsupportedOperationException("unsupported user type: " + type);
        }
        cacheUser(user);
        return user;
    }

    @Override
    public Group getGroup(ID identifier) {
        Group group = super.getGroup(identifier);
        if (group != null) {
            return group;
        }
        // check meta
        Meta meta = getMeta(identifier);
        if (meta == null) {
            throw new NullPointerException("meta not found: " + identifier);
        }
        // create it with type
        NetworkType type = identifier.getType();
        if (type == NetworkType.Polylogue) {
            group = new Polylogue(identifier);
        } else if (type == NetworkType.Chatroom) {
            group = new Chatroom(identifier);
        } else {
            throw new UnsupportedOperationException("unsupported group type: " + type);
        }
        cacheGroup(group);
        return group;
    }
    
    static {
        // mkm.Base64 (for Android)
        chat.dim.format.Base64.coder = new chat.dim.format.BaseCoder() {
            @Override
            public String encode(byte[] data) {
                return android.util.Base64.encodeToString(data, android.util.Base64.DEFAULT);
            }
            
            @Override
            public byte[] decode(String string) {
                return android.util.Base64.decode(string, android.util.Base64.DEFAULT);
            }
        };
    }
}
```

KeyStore.java

```java
/**
 *  For reuse symmetric key
 */
public class KeyStore extends KeyCache {
    private static final KeyStore ourInstance = new KeyStore();
    public static KeyStore getInstance() { return ourInstance; }
    
    private KeyStore() {
        super();
    }

    @Override
    public boolean saveKeys(Map keyMap) {
        return false;
    }

    @Override
    public Map loadKeys() {
        return null;
    }

    @Override
    public SymmetricKey reuseCipherKey(ID sender, ID receiver, SymmetricKey key) {
        return super.reuseCipherKey(sender, receiver, key);
    }
}
```

Messanger.java

```java
/**
 *  Transform and send message
 */
public class Messanger extends Transceiver implements TransceiverDelegate {
    private static final Messanger ourInstance = new Messanger();
    public static Messanger getInstance() { return ourInstance; }
    
    private Messanger()  {
        super();

        barrack = Facebook.getInstance();
        keyCache = KeyStore.getInstance();
    }
    
    // TransceiverDelegate
    @Override
    public boolean sendPackage(byte[] data, CompletionHandler handler) {
        // TODO: send out data
        return false;
    }
    
    @Override
    public String uploadFileData(byte[] data, InstantMessage iMsg) {
        // TODO: upload onto FTP server
        return null;
    }
    
    @Override
    public byte[] downloadFileData(String url, InstantMessage iMsg) {
        // TODO: download from FTP server
        return new byte[0];
    }
}
```

### User Account

Register.java

```java
    public User register(String username) {
        // 1. generate private key
        PrivateKey sk = PrivateKeyImpl.generate(PrivateKey.RSA);
        // 2. generate meta with username(as seed) and private key
        String seed = username;
        Meta meta = Meta.generate(Meta.VersionDefault, sk, seed);
        // 3. generate ID with network type by meta
        ID identifier = meta.generateID(NetworkType.Main);
        // 4. save private key and meta info
        facebook.savePrivateKey(sk, identifier);
        facebook.saveMeta(meta, identifier);
        // 5. create user with ID
        return facebook.getUser(identifier);
    }
```

### Messaging

Send.java

```java
    public ReliableMessage pack(Content content, ID sender, ID receiver) {
        InstantMessage iMsg = new InstantMessage(content, sender, receiver);
        ReliableMessage rMsg = null;
        try {
            rMsg = messanger.encryptAndSignMessage(iMsg);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
        return rMsg;
    }
    
    public void send(Content content, ID sender, ID receiver) {
        InstantMessage iMsg = new InstantMessage(content, sender, receiver);
        // callback
        Callback callback = new Callback() {
            @Override
            public void onFinished(Object result, Error error) {
                if (error == null) {
                    //iMsg.put("state", "Accepted");
                } else {
                    //iMsg.put("state", "Error");
                    //iMsg.put("error", error);
                }
            }
        };
        // encrypt, sign and send out
        try {
            messanger.sendMessage(iMsg, callback, true);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
    }
    
    public void test() {
        ID moki = facebook.getID("moki@4WDfe3zZ4T7opFSi3iDAKiuTnUHjxmXekk");
        ID hulk = facebook.getID("hulk@4YeVEN3aUnvC1DNUufCq1bs9zoBSJTzVEj");
        
        Content content = new TextContent("Hello world!");
        send(content, moki, hulk);
    }
```

Receive.java

```java
    // StationDelegate
    @Override
    public void didReceivePackage(byte[] data, Station server) {
        String json = new String(data, Charset.forName("UTF-8"));
        Object msg = JSON.decode(json);
        ReliableMessage rMsg = ReliableMessage.getInstance(msg);
        InstantMessage iMsg = null;
        try {
            iMsg = messanger.verifyAndDecryptMessage(rMsg);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        // TODO: process instant message
    }
```


Copyright &copy; 2019 Albert Moky
