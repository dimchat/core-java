
import chat.dim.dkd.SecureMessage;
import chat.dim.mkm.*;
import junit.framework.TestCase;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import chat.dim.core.*;
import chat.dim.dkd.Content;
import chat.dim.dkd.InstantMessage;
import chat.dim.dkd.ReliableMessage;
import chat.dim.protocol.TextContent;

public class CoreTest extends TestCase {

    @Test
    public void testTransceiver() {

        ID sender = ID.getInstance("moki@4WDfe3zZ4T7opFSi3iDAKiuTnUHjxmXekk");
        ID receiver = ID.getInstance("hulk@4YeVEN3aUnvC1DNUufCq1bs9zoBSJTzVEj");

        Content content = new TextContent("Hello");

        InstantMessage iMsg = new InstantMessage(content, sender, receiver);
        SecureMessage sMsg = transceiver.encryptMessage(iMsg);
        ReliableMessage rMsg = transceiver.signMessage(sMsg);

        SecureMessage sMsg2 = transceiver.verifyMessage(rMsg);
        InstantMessage iMsg2 = transceiver.decryptMessage(sMsg2);

        Log.info("send message: " + iMsg2);
    }

    @Test
    public void testBarrack() {
        ID identifier = barrack.getID("moky@4DnqXWdTV8wuZgfqSCX9GjE2kNq7HJrUgQ");

        Meta meta = barrack.getMeta(identifier);

        identifier = barrack.getID("moki@4WDfe3zZ4T7opFSi3iDAKiuTnUHjxmXekk");
        User user = barrack.getUser(identifier);

//        identifier = ID.getInstance("Group-1280719982@7oMeWadRw4qat2sL4mTdcQSDAqZSo7LH5G");
//
//        Group group = barrack.getGroup(identifier);

        Map<ID, Meta> map = new HashMap<>();
        identifier = null;
        meta = map.get(identifier);
        Log.info("meta: " + meta);
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
        transceiver.setSocialNetworkDataSource(barrack);
        transceiver.setCipherKeyDataSource(keyStore);
    }
}
