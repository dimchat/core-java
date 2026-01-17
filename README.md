# Decentralized Instant Messaging Protocol (Java)

[![License](https://img.shields.io/github/license/dimchat/core-java)](https://github.com/dimchat/core-java/blob/master/LICENSE)
[![PRs Welcome](https://img.shields.io/badge/PRs-welcome-brightgreen.svg)](https://github.com/dimchat/core-java/pulls)
[![Platform](https://img.shields.io/badge/Platform-Java%208-brightgreen.svg)](https://github.com/dimchat/core-java/wiki)
[![Issues](https://img.shields.io/github/issues/dimchat/core-java)](https://github.com/dimchat/core-java/issues)
[![Repo Size](https://img.shields.io/github/repo-size/dimchat/core-java)](https://github.com/dimchat/core-java/archive/refs/heads/master.zip)
[![Tags](https://img.shields.io/github/tag/dimchat/core-java)](https://github.com/dimchat/core-java/tags)
[![Version](https://img.shields.io/maven-central/v/chat.dim/DIMP)](https://mvnrepository.com/artifact/chat.dim/DIMP)

[![Watchers](https://img.shields.io/github/watchers/dimchat/core-java)](https://github.com/dimchat/core-java/watchers)
[![Forks](https://img.shields.io/github/forks/dimchat/core-java)](https://github.com/dimchat/core-java/forks)
[![Stars](https://img.shields.io/github/stars/dimchat/core-java)](https://github.com/dimchat/core-java/stargazers)
[![Followers](https://img.shields.io/github/followers/dimchat)](https://github.com/orgs/dimchat/followers)

## Dependencies

* Latest Versions

| Name | Version | Description |
|------|---------|-------------|
| [Cryptography](https://github.com/dimchat/mkm-java) | [![Version](https://img.shields.io/maven-central/v/chat.dim/Crypto)](https://mvnrepository.com/artifact/chat.dim/Crypto) | Crypto Keys |
| [Ming Ke Ming (名可名)](https://github.com/dimchat/mkm-java) | [![Version](https://img.shields.io/maven-central/v/chat.dim/MingKeMing)](https://mvnrepository.com/artifact/chat.dim/MingKeMing) | Decentralized User Identity Authentication |
| [Dao Ke Dao (道可道)](https://github.com/dimchat/dkd-java) | [![Version](https://img.shields.io/maven-central/v/chat.dim/DaoKeDao)](https://mvnrepository.com/artifact/chat.dim/DaoKeDao) | Universal Message Module |

* build.gradle

```javascript
allprojects {
    repositories {
        mavenLocal()
        mavenCentral()
        google()
    }
}

dependencies {

    // https://central.sonatype.com/artifact/chat.dim/DIMP
    implementation group: 'chat.dim', name: 'DIMP', version: '2.0.0'

}
```

* pom.xml

```xml
<dependencies>

    <!-- https://mvnrepository.com/artifact/chat.dim/DIMP -->
    <dependency>
        <groupId>chat.dim</groupId>
        <artifactId>DIMP</artifactId>
        <version>2.0.0</version>
        <type>pom</type>
    </dependency>

</dependencies>
```

## Examples

### Extends Command

* _Handshake Command Protocol_
  0. (C-S) handshake start
  1. (S-C) handshake again with new session
  2. (C-S) handshake restart with new session
  3. (S-C) handshake success

```java
public enum HandshakeState {
    START,    // C -> S, without session key (or session expired)
    AGAIN,    // S -> C, with new session key
    RESTART,  // C -> S, with new session key
    SUCCESS,  // S -> C, handshake accepted
}

/**
 *  Handshake command
 *
 *  <blockquote><pre>
 *  data format: {
 *      "type" : i2s(0x88),
 *      "sn"   : 123,
 *
 *      "command" : "handshake",    // command name
 *      "title"   : "Hello world!", // "DIM?", "DIM!"
 *      "session" : "{SESSION_KEY}" // session key
 *  }
 *  </pre></blockquote>
 */
public class HandshakeCommand extends BaseCommand {

    public final static String HANDSHAKE = "handshake";

    public HandshakeCommand(Map<String, Object> content) {
        super(content);
    }

    public HandshakeCommand(String text, String session) {
        super(HANDSHAKE);
        // text message
        assert text != null : "new handshake command error";
        put("title", text);
        // session key
        if (session != null) {
            put("session", session);
        }
    }

    public String getTitle() {
        return getString("title", null);
    }

    public String getSessionKey() {
        return getString("session", null);
    }

    public HandshakeState getState() {
        return checkState(getTitle(), getSessionKey());
    }

    private static HandshakeState checkState(String text, String session) {
        assert text != null : "handshake title should not be empty";
        if (text.equals("DIM!")/* || text.equals("OK!")*/) {
            return HandshakeState.SUCCESS;
        } else if (text.equals("DIM?")) {
            return HandshakeState.AGAIN;
        } else if (session == null) {
            return HandshakeState.START;
        } else {
            return HandshakeState.RESTART;
        }
    }

    //
    //  Factories
    //

    public static HandshakeCommand start() {
        return new HandshakeCommand("Hello world!", null);
    }

    public static HandshakeCommand restart(String sessionKey) {
        return new HandshakeCommand("Hello world!", sessionKey);
    }

    public static HandshakeCommand again(String sessionKey) {
        return new HandshakeCommand("DIM?", sessionKey);
    }

    public static HandshakeCommand success(String sessionKey) {
        return new HandshakeCommand("DIM!", sessionKey);
    }
}
```

### Extends Content

```java
/**
 *  Content for Application 0nly: {
 *  
 *      "type" : i2s(0xA0),
 *      "sn"   : 123,
 *
 *      "app"   : "{APP_ID}",  // application (e.g.: "chat.dim.sechat")
 *      "extra" : info         // others
 *  }
 */
public class ApplicationContent extends BaseContent implements AppContent {

    public ApplicationContent(Map<String, Object> content) {
        super(content);
    }

    public ApplicationContent(String app) {
        super(ContentType.APPLICATION);
        put("app", app);
    }

    @Override
    public String getApplication() {
        return getString("app", "");
    }

}

```

### Extends ID Address

* Examples in [Plugins](https://mvnrepository.com/artifact/chat.dim/Plugins)

----

Copyright &copy; 2018-2026 Albert Moky
[![Followers](https://img.shields.io/github/followers/moky)](https://github.com/moky?tab=followers)