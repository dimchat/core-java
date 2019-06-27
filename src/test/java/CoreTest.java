import chat.dim.core.*;
import chat.dim.crypto.SymmetricKey;
import chat.dim.dkd.Content;
import chat.dim.dkd.InstantMessage;
import chat.dim.dkd.ReliableMessage;
import chat.dim.mkm.Account;
import chat.dim.mkm.Group;
import chat.dim.mkm.User;
import chat.dim.mkm.entity.ID;
import chat.dim.mkm.entity.Meta;
import chat.dim.protocol.TextContent;
import junit.framework.TestCase;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CoreTest extends TestCase {

    @Test
    public void testTransceiver() throws NoSuchFieldException, ClassNotFoundException {

        CompletionHandler handler = new CompletionHandler() {
            @Override
            public void onSuccess() {
                Log.info("success!");
            }

            @Override
            public void onFailed(Error error) {
                Log.info("error: " + error);
            }
        };

        Callback callback = (result, error) -> {
            Log.info("callback: " + result + ", error: " + error);
        };

        ID sender = ID.getInstance("moki@4WDfe3zZ4T7opFSi3iDAKiuTnUHjxmXekk");
        ID receiver = ID.getInstance("hulk@4YeVEN3aUnvC1DNUufCq1bs9zoBSJTzVEj");

        User user = barrack.getUser(receiver);
        List<User> users = new ArrayList<>();
        users.add(user);

        Content content = new TextContent("Hello");

        InstantMessage iMsg = new InstantMessage(content, sender, receiver, null);

        ReliableMessage rMsg = transceiver.encryptAndSignMessage(iMsg);

        InstantMessage iMsg2 = transceiver.verifyAndDecryptMessage(rMsg, users);

        boolean OK = transceiver.sendMessage(iMsg, callback, true);
        Log.info("send message: " + OK);
    }

    @Test
    public void testBarrack() {
        ID identifier = ID.getInstance("moky@4DnqXWdTV8wuZgfqSCX9GjE2kNq7HJrUgQ");

        Meta meta = barrack.getMeta(identifier);
        if (meta != null) {
            barrack.cacheMeta(meta, identifier);
        }

        Account account = barrack.getAccount(identifier);

        User user = barrack.getUser(identifier);

        identifier = ID.getInstance("Group-1280719982@7oMeWadRw4qat2sL4mTdcQSDAqZSo7LH5G");

        Group group = barrack.getGroup(identifier);
    }

    static Barrack barrack;
    static KeyStore keyStore;
    static Transceiver transceiver;

    static {
        Facebook facebook = Facebook.getInstance();

        // barrack
        barrack = new Barrack();
        barrack.entityDataSource = facebook;
        barrack.userDataSource = facebook;
        barrack.groupDataSource = facebook;
        barrack.delegate = facebook;
        int count = barrack.reduceMemory();

        // keystore
        try {
            keyStore = new KeyStore() {
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
                    return null;
                }
            };
            Map keys = new HashMap();
            boolean changed = keyStore.updateKeys(keys);
            keyStore.flush();

        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            keyStore = null;
        }

        // transceiver
        transceiver = new Transceiver();
        transceiver.delegate = new Station();
        transceiver.barrackDelegate = facebook;
        transceiver.entityDataSource = barrack;
        transceiver.cipherKeyDataSource = keyStore;
    }
}
