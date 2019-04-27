package chat.dim.core;

import chat.dim.crypto.PrivateKey;
import chat.dim.crypto.PublicKey;
import chat.dim.crypto.SymmetricKey;
import chat.dim.crypto.Utils;
import chat.dim.dkd.*;
import chat.dim.dkd.content.Content;
import chat.dim.dkd.content.ForwardContent;
import chat.dim.mkm.Account;
import chat.dim.mkm.Group;
import chat.dim.mkm.User;
import chat.dim.mkm.entity.ID;
import chat.dim.mkm.entity.Meta;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class Transceiver implements InstantMessageDelegate, SecureMessageDelegate, ReliableMessageDelegate {

    private static Transceiver ourInstance = new Transceiver();

    public static Transceiver getInstance() {
        return ourInstance;
    }

    private Transceiver() {
    }

    // delegate
    public TransceiverDelegate delegate;

    /**
     *  Send message (secured + certified) to target station
     *
     *  @param iMsg - instant message
     *  @param callback - callback function
     *  @param split - if it's a group message, split it before sending out
     *  @return NO on data/delegate error
     */
    public boolean sendMessage(InstantMessage iMsg, Callback callback, boolean split) throws NoSuchFieldException {
        // transforming
        ID receiver = ID.getInstance(iMsg.envelope.receiver);
        ID groupID = ID.getInstance(iMsg.content.group);
        ReliableMessage rMsg = encryptAndSignMessage(iMsg);
        if (rMsg == null) {
            // TODO: set iMsg.state = error
            throw new NullPointerException("failed to encrypt and sign message:" + iMsg);
        }
        Barrack barrack = Barrack.getInstance();

        // trying to send out
        boolean OK = true;
        if (split && receiver.getType().isGroup()) {
            Group group = barrack.getGroup(groupID);
            int count = barrack.getCountOfMembers(group);
            assert count > 0;
            List<Object> members = new ArrayList<>(count);
            ID item;
            for (int index = 0; index < count; index++) {
                item = barrack.getMemberAtIndex(index, group);
                if (item == null) {
                    continue;
                }
                members.add(item.toString());
            }
            List<SecureMessage> messages = rMsg.split(members);
            for (SecureMessage msg: messages) {
                if (sendMessage((ReliableMessage) msg, callback)) {
                    // group message sent
                } else {
                    OK = false;
                }
            }
        } else {
            OK = sendMessage(rMsg, callback);
        }

        // sending status
        if (OK) {
            // TODO: set iMsg.state = sending
        } else {
            // TODO: set iMsg.state = waiting
        }
        return OK;
    }

    private boolean sendMessage(ReliableMessage rMsg, Callback callback) {
        String json = Utils.jsonEncode(rMsg);
        byte[] data = json.getBytes(StandardCharsets.UTF_8);
        return delegate.sendPackage(data, new CompletionHandler() {
            @Override
            public void onSuccess() {
                callback.onFinished(rMsg, null);
            }

            @Override
            public void onFailed(Error error) {
                callback.onFinished(rMsg, error);
            }
        });
    }

    /**
     *  Pack instant message to reliable message for delivering
     *
     *  @param iMsg - instant message
     *  @return ReliableMessage Object
     */
    private ReliableMessage encryptAndSignMessage(InstantMessage iMsg) throws NoSuchFieldException {
        Barrack barrack = Barrack.getInstance();
        KeyStore store = KeyStore.getInstance();
        User user = store.currentUser;
        if (user == null) {
            throw new NullPointerException("current user not set to key store");
        }

        if (iMsg.delegate == null) {
            iMsg.delegate = this;
        }
        SymmetricKey key = null;
        SecureMessage sMsg = null;

        // 1. encrypt 'content' to 'data' for receiver
        ID receiver = ID.getInstance(iMsg.envelope.receiver);
        ID groupID = ID.getInstance(iMsg.content.group);
        if (groupID != null) {
            // if 'group' exists and the 'receiver' is a group ID,
            // they must be equal
        } else {
            assert receiver != null;
            if (receiver.getType().isGroup()) {
                groupID = receiver;
            }
        }

        if (groupID != null) {
            // group message
            List<Object> members;
            if (receiver.getType().isCommunicator()) {
                // split group message
                members = new ArrayList<>();
                members.add(receiver);
            } else {
                Group group = barrack.getGroup(groupID);
                members = barrack.getMembers(group);
            }
            assert members != null;
            key = store.getKey(user.identifier, groupID);
            sMsg = iMsg.encrypt(key, members);
        } else {
            // personal message
            key = store.getKey(user.identifier, receiver);
            sMsg = iMsg.encrypt(key);
        }

        // 2. sign 'data' by sender
        sMsg.delegate = this;
        return sMsg.sign();
    }

    /**
     *  Extract instant message from a reliable message received
     *
     *  @param rMsg - reliable message
     *  @param users - my accounts
     *  @return InstantMessage object
     */
    private InstantMessage verifyAndDecryptMessage(ReliableMessage rMsg, List<User> users) throws IOException, ClassNotFoundException {
        ID sender = ID.getInstance(rMsg.envelope.sender);
        ID receiver = ID.getInstance(rMsg.envelope.receiver);

        Barrack barrack = Barrack.getInstance();
        // [Meta Protocol] check meta in first contact message
        Meta meta = barrack.getMeta(sender);
        if (meta == null) {
            // first contact, try meta in message package
            meta = Meta.getInstance(rMsg.meta);
            assert meta.matches(sender);
            barrack.saveMeta(meta, sender);
        }

        if (rMsg.delegate == null) {
            rMsg.delegate = this;
        }

        // check recipient
        ID groupID = ID.getInstance(rMsg.getGroup());
        User user = null;
        if (receiver.getType().isGroup()) {
            groupID = receiver;
            // FIXME: maybe other user?
            user = users.get(0);
            receiver = user.identifier;
        } else {
            for (User item : users) {
                if (item.identifier.equals(receiver)) {
                    user = item;
                    // got new message for this user
                    break;
                }
            }
        }
        if (user == null) {
            throw new NullPointerException("wrong recipient:" + receiver);
        }

        // 1. verify 'data' with 'signature'
        SecureMessage sMsg = rMsg.verify();

        // 2. decrypt 'data' to 'content'
        InstantMessage iMsg = null;
        if (groupID != null) {
            // group message
            sMsg = sMsg.trim(user.identifier.toString());
            sMsg.delegate = this;
            iMsg = sMsg.decrypt(receiver.toString());
        } else {
            // personal message
            sMsg.delegate = this;
            iMsg = sMsg.decrypt();
        }

        // 3. check: top-secret message
        if (iMsg.content.type == Content.FORWARD) {
            // do it again to drop the wrapper,
            // the secret inside the content is the real message
            ForwardContent content = (ForwardContent) iMsg.content;
            rMsg = content.forwardMessage;

            return verifyAndDecryptMessage(rMsg, users);
        }

        // OK
        return iMsg;
    }

    //-------- IInstantMessageDelegate

    @Override
    public String uploadFileData(InstantMessage iMsg, byte[] data, String filename, Map<String, Object> password) {
        SymmetricKey key;
        try {
            key = SymmetricKey.getInstance(password);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
        byte[] cipherText = key.encrypt(data);
        return delegate.uploadFileData(cipherText, iMsg);
    }

    @Override
    public byte[] downloadFileData(InstantMessage iMsg, String url, Map<String, Object> password) {
        byte[] cipherText = delegate.downloadFileData(url, iMsg);
        if (cipherText == null) {
            return null;
        }
        SymmetricKey key;
        try {
            key = SymmetricKey.getInstance(password);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
        return key.decrypt(cipherText);
    }

    @Override
    public byte[] encryptContent(InstantMessage iMsg, Content content, Map<String, Object> password) {
        SymmetricKey key;
        try {
            key = SymmetricKey.getInstance(password);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
        String json = Utils.jsonEncode(content);
        byte[] data;
        data = json.getBytes(StandardCharsets.UTF_8);
        return key.encrypt(data);
    }

    @Override
    public byte[] encryptKey(InstantMessage iMsg, Map<String, Object> password, Object receiver) {
        String json = Utils.jsonEncode(password);
        byte[] data;
        data = json.getBytes(StandardCharsets.UTF_8);
        Barrack barrack = Barrack.getInstance();
        PublicKey publicKey = barrack.getPublicKey(ID.getInstance(receiver));
        if (publicKey == null) {
            throw new NullPointerException("failed to get public key for receiver:" + receiver);
        }
        return publicKey.encrypt(data);
    }

    //-------- ISecureMessageDelegate

    @Override
    public Map<String, Object> decryptKey(SecureMessage sMsg, byte[] keyData, Object sender, Object receiver) {
        KeyStore store = KeyStore.getInstance();
        ID from = ID.getInstance(sender);
        ID to = ID.getInstance(receiver);
        SymmetricKey key = null;
        if (keyData != null) {
            // decrypt key data with the receiver's private key
            User user = store.currentUser;
            // FIXME: check sMsg.envelope.receiver == user.identifier
            PrivateKey privateKey = user.privateKey;
            byte[] plaintext = privateKey.decrypt(keyData);
            if (plaintext == null) {
                throw new NullPointerException("failed to decrypt key:" + keyData);
            }
            String json = new String(plaintext, StandardCharsets.UTF_8);
            try {
                // create symmetric key from JsON data
                key = SymmetricKey.getInstance(Utils.jsonDecode(json));
                // set the new key in key store
                store.setKey(key, from, to);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
        if (key == null) {
            // if key data is empty, get it from key store
            key = store.getKey(from, to);
        }
        return key;
    }

    @Override
    public Content decryptContent(SecureMessage sMsg, byte[] data, Map<String, Object> password) {
        SymmetricKey key;
        try {
            key = SymmetricKey.getInstance(password);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
        // decrypt message.data
        byte[] plaintext = key.decrypt(data);
        if (plaintext == null) {
            throw new NullPointerException("failed to decrypt data:" + password);
        }
        try {
            String json = new String(plaintext, StandardCharsets.UTF_8);
            return Content.getInstance(Utils.jsonDecode(json));
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public byte[] signData(SecureMessage sMsg, byte[] data, Object sender) {
        Barrack barrack = Barrack.getInstance();
        User user = barrack.getUser(ID.getInstance(sender));
        PrivateKey privateKey = user.privateKey;
        return privateKey.sign(data);
    }

    //-------- IReliableMessageDelegate

    @Override
    public boolean verifyData(ReliableMessage rMsg, byte[] data, byte[] signature, Object sender) {
        Barrack barrack = Barrack.getInstance();
        Account account = barrack.getAccount(ID.getInstance(sender));
        PublicKey publicKey = account.publicKey;
        return publicKey.verify(data, signature);
    }
}
