# Decentralized Instant Messaging Protocol (Java)

[![license](https://img.shields.io/github/license/mashape/apistatus.svg)](https://github.com/dimchat/core-java/blob/master/LICENSE)
[![Version](https://img.shields.io/badge/alpha-0.1.0-red.svg)](https://github.com/dimchat/core-java/archive/master.zip)
[![PRs Welcome](https://img.shields.io/badge/PRs-welcome-brightgreen.svg)](https://github.com/dimchat/core-java/pulls)
[![Platform](https://img.shields.io/badge/Platform-Java%208-brightgreen.svg)](https://github.com/dimchat/core-objc/wiki)

## Talk is cheap, show you the codes!

### Dependencies

build.gradle

```java
allprojects {
    repositories {
        jcenter()
        mavenCentral()
    }
}

dependencies {

    // https://bintray.com/dimchat/core/dimp
    compile 'chat.dim:DIMP:0.3.6'
//  implementation group: 'chat.dim', name: 'DIMP', version: '0.3.6'

}
```

pom.xml

```xml
<dependencies>

    <!-- https://mvnrepository.com/artifact/chat.dim/DIMP -->
    <dependency>
        <groupId>chat.dim</groupId>
        <artifactId>DIMP</artifactId>
        <version>0.3.6</version>
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
    
    //...
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

Transceiver.java

```java
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
    }
    
    //...
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
        User user = new User(identifier);
        user.dataSource = facebook;
        facebook.cacheUser(user);
        return user;
    }
```

### Messaging

Send.java

```java
    public void send(Content content, ID sender, ID receiver) {
        InstantMessage iMsg = new InstantMessage(content, sender, receiver);
        // callback
        Callback callback = new Callback() {
            @Override
            public void onFinished(Object result, Error error) {
                if (error == null) {
                    //iMsg.state = "Accepted";
                } else {
                    //iMsg.state = "Error";
                    //iMsg.error = error;
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
