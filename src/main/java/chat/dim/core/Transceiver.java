package chat.dim.core;

import chat.dim.crypto.PrivateKey;
import chat.dim.crypto.PublicKey;
import chat.dim.crypto.SymmetricKey;
import chat.dim.dkd.*;
import chat.dim.mkm.Account;
import chat.dim.mkm.Group;
import chat.dim.mkm.User;
import chat.dim.mkm.entity.ID;
import chat.dim.mkm.entity.Meta;
import chat.dim.protocols.ContentType;
import chat.dim.protocols.ForwardContent;
import chat.dim.protocols.file.FileContent;

import java.io.IOException;
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
     * @param iMsg - instant message
     * @param callback - callback function
     * @param split - if it's a group message, split it before sending out
     * @return NO on data/delegate error
     * @throws NoSuchFieldException when 'group' not found
     * @throws ClassNotFoundException when key algorithm not supported
     */
    public boolean sendMessage(InstantMessage iMsg, Callback callback, boolean split) throws NoSuchFieldException, ClassNotFoundException {
        // transforming
        ID receiver = ID.getInstance(iMsg.envelope.receiver);
        ID groupID = ID.getInstance(iMsg.content.getGroup());
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
        String json = JsON.encode(rMsg);
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
     * @param iMsg - instant message
     * @return ReliableMessage Object
     * @throws NoSuchFieldException when encrypt message content
     */
    public ReliableMessage encryptAndSignMessage(InstantMessage iMsg) throws NoSuchFieldException {
        if (iMsg.delegate == null) {
            iMsg.delegate = this;
        }

        ID receiver = ID.getInstance(iMsg.envelope.receiver);
        ID groupID = ID.getInstance(iMsg.content.getGroup());
        if (groupID != null) {
            // if 'group' exists and the 'receiver' is a group ID,
            // they must be equal
        } else {
            assert receiver != null;
            if (receiver.getType().isGroup()) {
                groupID = receiver;
            }
        }

        // 1. encrypt 'content' to 'data' for receiver
        SecureMessage sMsg;
        if (groupID != null) {
            // group message
            List<Object> members;
            if (receiver.getType().isCommunicator()) {
                // split group message
                members = new ArrayList<>();
                members.add(receiver);
            } else {
                Barrack barrack = Barrack.getInstance();
                Group group = barrack.getGroup(groupID);
                members = barrack.getMembers(group);
            }
            assert members != null;
            sMsg = iMsg.encrypt(getKey(groupID), members);
        } else {
            // personal message
            sMsg = iMsg.encrypt(getKey(receiver));
        }

        // 2. sign 'data' by sender
        sMsg.delegate = this;
        return sMsg.sign();
    }

    private SymmetricKey getKey(ID receiver) {
        KeyStore store = KeyStore.getInstance();
        User user = store.currentUser;
        if (user == null) {
            throw new NullPointerException("current user not set to key store");
        }
        ID sender = user.identifier;
        SymmetricKey key;

        // 1. get from key store
        key = store.getKey(sender, receiver);
        if (key != null) {
            return key;
        }

        // 2. get from delegate
        key = delegate.createCipherKey(sender, receiver);
        if (key == null) {
            // 3. create a new key
            try {
                key = SymmetricKey.create(SymmetricKey.AES);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }

        // 4. save it into the Key Store
        store.setKey(key, sender, receiver);
        return key;
    }

    /**
     *  Extract instant message from a reliable message received
     *
     * @param rMsg - reliable message
     * @param users - my accounts
     * @return InstantMessage object
     * @throws IOException when saving meta
     * @throws ClassNotFoundException when creating meta
     */
    public InstantMessage verifyAndDecryptMessage(ReliableMessage rMsg, List<User> users) throws IOException, ClassNotFoundException {
        ID sender = ID.getInstance(rMsg.envelope.sender);
        ID receiver = ID.getInstance(rMsg.envelope.receiver);

        Barrack barrack = Barrack.getInstance();
        // [Meta Protocol] check meta in first contact message
        Meta meta = barrack.getMeta(sender);
        if (meta == null) {
            // first contact, try meta in message package
            meta = Meta.getInstance(rMsg.getMeta());
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
        if (iMsg.content.type == ContentType.FORWARD.value) {
            // do it again to drop the wrapper,
            // the secret inside the content is the real message
            ForwardContent content = (ForwardContent) iMsg.content;
            rMsg = content.forwardMessage;

            return verifyAndDecryptMessage(rMsg, users);
        }

        // OK
        return iMsg;
    }

    //-------- InstantMessageDelegate

    @Override
    public byte[] encryptContent(InstantMessage iMsg, Content content, Map<String, Object> password) {
        SymmetricKey key;
        try {
            key = SymmetricKey.getInstance(password);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }

        // check attachment for File/Image/Audio/Video message content
        int type = content.type;
        if (type == ContentType.FILE.value ||
                type == ContentType.IMAGE.value ||
                type == ContentType.AUDIO.value ||
                type == ContentType.VIDEO.value) {
            // upload file data onto CDN and save the URL in message content
            FileContent file = new FileContent(content);
            byte[] data = file.getData();
            if (data != null) {
                // encrypt it first
                data = key.encrypt(data);
                // upload encrypted data to CDN
                String url = delegate.uploadFileData(data, iMsg);
                if (url != null) {
                    file.setUrl(url);
                    file.setData(null);
                    content = file;
                }
            }
        }

        String json = JsON.encode(content);
        byte[] data;
        data = json.getBytes(StandardCharsets.UTF_8);
        return key.encrypt(data);
    }

    @Override
    public byte[] encryptKey(InstantMessage iMsg, Map<String, Object> password, Object receiver) {
        String json = JsON.encode(password);
        byte[] data;
        data = json.getBytes(StandardCharsets.UTF_8);
        Barrack barrack = Barrack.getInstance();
        PublicKey publicKey = barrack.getPublicKey(ID.getInstance(receiver));
        if (publicKey == null) {
            throw new NullPointerException("failed to get public key for receiver:" + receiver);
        }
        return publicKey.encrypt(data);
    }

    //-------- SecureMessageDelegate

    @Override
    public Map<String, Object> decryptKey(SecureMessage sMsg, byte[] keyData, Object sender, Object receiver) {
        Barrack barrack = Barrack.getInstance();
        KeyStore store = KeyStore.getInstance();
        ID from = ID.getInstance(sender);
        ID to = ID.getInstance(receiver);
        SymmetricKey key = null;
        if (keyData != null) {
            // decrypt key data with the receiver's private key
            ID identifier = ID.getInstance(sMsg.envelope.receiver);
            User user = store.currentUser;
            if (user == null || !user.identifier.equals(identifier)) {
                if (identifier.getType().isCommunicator()) {
                    user = barrack.getUser(identifier);
                }
                if (user == null) {
                    throw new IllegalArgumentException("receiver error:" + sMsg);
                }
            }
            // FIXME: check sMsg.envelope.receiver == user.identifier
            PrivateKey privateKey = user.getPrivateKey();
            byte[] plaintext = privateKey.decrypt(keyData);
            if (plaintext == null) {
                throw new NullPointerException("failed to decrypt key:" + keyData);
            }
            String json = new String(plaintext, StandardCharsets.UTF_8);
            try {
                // create symmetric key from JsON data
                key = SymmetricKey.getInstance(JsON.decode(json));
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

        String json = new String(plaintext, StandardCharsets.UTF_8);
        Map<String, Object> dictionary = JsON.decode(json);
        Content content;
        try {
            content = Content.getInstance(dictionary);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }

        // check attachment for File/Image/Audio/Video message content
        int type = content.type;
        if (type == ContentType.FILE.value ||
                type == ContentType.IMAGE.value ||
                type == ContentType.AUDIO.value ||
                type == ContentType.VIDEO.value) {
            // download from CDN
            FileContent file = new FileContent(content);
            String url = file.getUrl();
            assert url != null;
            byte[] fileData = delegate.downloadFileData(url, new InstantMessage(content, sMsg.envelope));
            if (fileData != null) {
                // decrypt file data
                file.setData(key.decrypt(fileData));
            } else {
                // save symmetric key for decrypted file data after download from CDN
                file.setPassword(key);
            }
            content = file;
        }

        return content;
    }

    @Override
    public byte[] signData(SecureMessage sMsg, byte[] data, Object sender) {
        Barrack barrack = Barrack.getInstance();
        User user = barrack.getUser(ID.getInstance(sender));
        PrivateKey privateKey = user.getPrivateKey();
        return privateKey.sign(data);
    }

    //-------- ReliableMessageDelegate

    @Override
    public boolean verifyData(ReliableMessage rMsg, byte[] data, byte[] signature, Object sender) {
        Barrack barrack = Barrack.getInstance();
        Account account = barrack.getAccount(ID.getInstance(sender));
        PublicKey publicKey = account.getPublicKey();
        return publicKey.verify(data, signature);
    }
}
