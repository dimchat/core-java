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

## Examples

### Extends Command

* _Handshake Command Protocol_
  0. (C-S) handshake start
  1. (S-C) handshake again with new session
  2. (C-S) handshake restart with new session
  3. (S-C) handshake success

```java
public enum HandshakeState {
    START,    // C -> S, without session key(or session expired)
    AGAIN,    // S -> C, with new session key
    RESTART,  // C -> S, with new session key
    SUCCESS;  // S -> C, handshake accepted

    public static HandshakeState checkState(String title, String session) {
        assert title != null : "handshake title should not be empty";
        if (title.equals("DIM!")/* || text.equals("OK!")*/) {
            return SUCCESS;
        } else if (title.equals("DIM?")) {
            return AGAIN;
        } else if (session == null) {
            return START;
        } else {
            return RESTART;
        }
    }

}
```

```java
/**
 *  Handshake command message
 *
 *  <blockquote><pre>
 *  data format: {
 *      type : 0x88,
 *      sn   : 123,
 *
 *      command : "handshake",    // command name
 *      title   : "Hello world!", // "DIM?", "DIM!"
 *      session : "{SESSION_KEY}" // session key
 *  }
 *  </pre></blockquote>
 */
public interface HandshakeCommand extends Command {

    String HANDSHAKE = "handshake";

    String getTitle();
    String getSessionKey();

    HandshakeState getState();

    static HandshakeCommand start() {
        return new BaseHandshakeCommand("Hello world!", null);
    }

    static HandshakeCommand restart(String sessionKey) {
        return new BaseHandshakeCommand("Hello world!", sessionKey);
    }

    static HandshakeCommand again(String sessionKey) {
        return new BaseHandshakeCommand("DIM?", sessionKey);
    }

    static HandshakeCommand success(String sessionKey) {
        return new BaseHandshakeCommand("DIM!", sessionKey);
    }
}
```

```java
public class BaseHandshakeCommand extends BaseCommand implements HandshakeCommand {

    public BaseHandshakeCommand(Map<String, Object> content) {
        super(content);
    }

    public BaseHandshakeCommand(String text, String session) {
        super(HANDSHAKE);
        // text message
        assert text != null : "new handshake command error";
        put("title", text);
        // session key
        if (session != null) {
            put("session", session);
        }
    }

    @Override
    public String getTitle() {
        return getString("title", null);
    }

    @Override
    public String getSessionKey() {
        return getString("session", null);
    }

    @Override
    public HandshakeState getState() {
        return HandshakeState.checkState(getTitle(), getSessionKey());
    }

}
```

### Extends Content

```java
/**
 *  Content for Application 0nly
 *
 *  <blockquote><pre>
 *  data format: {
 *      "type" : i2s(0xA0),
 *      "sn"   : 123,
 *
 *      "app"   : "{APP_ID}",  // application (e.g.: "chat.dim.sechat")
 *      "extra" : info         // others
 *  }
 *  </pre></blockquote>
 */
public interface AppContent extends Content {

    // get App ID
    String getApplication();

}
```

```java
/**
 *  Customized Content
 *
 *  <blockquote><pre>
 *  data format: {
 *      "type" : i2s(0xCC),
 *      "sn"   : 123,
 *
 *      "app"   : "{APP_ID}",  // application (e.g.: "chat.dim.sechat")
 *      "mod"   : "{MODULE}",  // module name (e.g.: "drift_bottle")
 *      "act"   : "{ACTION}",  // action name (3.g.: "throw")
 *      "extra" : info         // action parameters
 *  }
 *  </pre></blockquote>
 */
public interface CustomizedContent extends Content {

    // get Module Name
    String getModule();

    // get Action Name
    String getAction();

    //
    //  Factory methods
    //

    static CustomizedContent create(String app, String mod, String act) {
        return new AppCustomizedContent(app, mod, act);
    }

    static CustomizedContent create(String type, String app, String mod, String act) {
        return new AppCustomizedContent(type, app, mod, act);
    }

}
```

```java
public class AppCustomizedContent extends BaseContent implements AppContent, CustomizedContent {

    public AppCustomizedContent(Map<String, Object> content) {
        super(content);
    }

    public AppCustomizedContent(String type, String app, String mod, String act) {
        super(type);
        put("app", app);
        put("mod", mod);
        put("act", act);
    }

    public AppCustomizedContent(String app, String mod, String act) {
        super(ContentType.CUSTOMIZED);
        put("app", app);
        put("mod", mod);
        put("act", act);
    }

    //-------- getters --------

    @Override
    public String getApplication() {
        return getString("app", "");
    }

    @Override
    public String getModule() {
        return getString("mod", "");
    }

    @Override
    public String getAction() {
        return getString("act", "");
    }

}
```


### Extends ID Address

* Examples in [Plugins](https://mvnrepository.com/artifact/chat.dim/Plugins)

----

Copyright &copy; 2018-2026 Albert Moky
[![Followers](https://img.shields.io/github/followers/moky)](https://github.com/moky?tab=followers)