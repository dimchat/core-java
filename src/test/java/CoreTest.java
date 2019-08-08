
import junit.framework.TestCase;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import chat.dim.core.*;
import chat.dim.dkd.Content;
import chat.dim.dkd.InstantMessage;
import chat.dim.dkd.ReliableMessage;
import chat.dim.mkm.Account;
import chat.dim.mkm.Group;
import chat.dim.mkm.User;
import chat.dim.mkm.entity.ID;
import chat.dim.mkm.entity.Meta;
import chat.dim.protocol.TextContent;

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

        Content content = new TextContent("Hello");

        InstantMessage iMsg = new InstantMessage(content, sender, receiver);

        ReliableMessage rMsg = transceiver.encryptAndSignMessage(iMsg);

        InstantMessage iMsg2 = transceiver.verifyAndDecryptMessage(rMsg);

        boolean OK = transceiver.sendMessage(iMsg, callback, true);
        Log.info("send message: " + OK);
    }

    @Test
    public void testBarrack() {
        ID identifier = barrack.getID("moky@4DnqXWdTV8wuZgfqSCX9GjE2kNq7HJrUgQ");

        Meta meta = barrack.getMeta(identifier);

        Account account = barrack.getAccount(identifier);

        User user = barrack.getUser(identifier);

        identifier = ID.getInstance("Group-1280719982@7oMeWadRw4qat2sL4mTdcQSDAqZSo7LH5G");

        Group group = barrack.getGroup(identifier);
    }

    static Barrack barrack;
    static KeyCache keyStore;
    static Transceiver transceiver;

    static {
        barrack = Facebook.getInstance();

        // keystore
        try {
            keyStore = new KeyCache() {
                @Override
                public boolean saveKeys(Map keyMap) {
                    return false;
                }

                @Override
                public Map loadKeys() {
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
        transceiver.barrack = barrack;
        transceiver.keyCache = keyStore;
    }
}
