/* license: https://mit-license.org
 *
 *  DIMP : Decentralized Instant Messaging Protocol
 *
 *                                Written in 2021 by Moky <albert.moky@gmail.com>
 *
 * ==============================================================================
 * The MIT License (MIT)
 *
 * Copyright (c) 2021 Albert Moky
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 * ==============================================================================
 */
package chat.dim.core;

import chat.dim.crypto.SymmetricKey;
import chat.dim.dkd.AudioFileContent;
import chat.dim.dkd.BaseContent;
import chat.dim.dkd.BaseDocumentCommand;
import chat.dim.dkd.BaseFileContent;
import chat.dim.dkd.BaseMetaCommand;
import chat.dim.dkd.BaseMoneyContent;
import chat.dim.dkd.BaseTextContent;
import chat.dim.dkd.ImageFileContent;
import chat.dim.dkd.ListContent;
import chat.dim.dkd.SecretContent;
import chat.dim.dkd.TransferMoneyContent;
import chat.dim.dkd.VideoFileContent;
import chat.dim.dkd.WebPageContent;
import chat.dim.dkd.group.ExpelGroupCommand;
import chat.dim.dkd.group.InviteGroupCommand;
import chat.dim.dkd.group.JoinGroupCommand;
import chat.dim.dkd.group.QueryGroupCommand;
import chat.dim.dkd.group.QuitGroupCommand;
import chat.dim.dkd.group.ResetGroupCommand;
import chat.dim.format.Base64;
import chat.dim.format.JSON;
import chat.dim.format.UTF8;
import chat.dim.mkm.Entity;
import chat.dim.mkm.User;
import chat.dim.protocol.Command;
import chat.dim.protocol.Content;
import chat.dim.protocol.ContentType;
import chat.dim.protocol.GroupCommand;
import chat.dim.protocol.ID;
import chat.dim.protocol.InstantMessage;
import chat.dim.protocol.Message;
import chat.dim.protocol.ReliableMessage;
import chat.dim.protocol.SecureMessage;

/**
 *  Message Transceiver
 *  ~~~~~~~~~~~~~~~~~~~
 *
 *  Converting message format between PlainMessage and NetworkMessage
 */
public abstract class Transceiver implements InstantMessage.Delegate, ReliableMessage.Delegate {

    protected abstract Entity.Delegate getEntityDelegate();

    protected static boolean isBroadcast(Message msg) {
        ID receiver = msg.getGroup();
        if (receiver == null) {
            receiver = msg.getReceiver();
        }
        return receiver.isBroadcast();
    }

    //-------- InstantMessageDelegate

    @Override
    public byte[] serializeContent(Content content, SymmetricKey password, InstantMessage iMsg) {
        // NOTICE: check attachment for File/Image/Audio/Video message content
        //         before serialize content, this job should be do in subclass
        return UTF8.encode(JSON.encode(content));
    }

    @Override
    public byte[] encryptContent(byte[] data, SymmetricKey password, InstantMessage iMsg) {
        return password.encrypt(data);
    }

    @Override
    public Object encodeData(byte[] data, InstantMessage iMsg) {
        if (isBroadcast(iMsg)) {
            // broadcast message content will not be encrypted (just encoded to JsON),
            // so no need to encode to Base64 here
            return UTF8.decode(data);
        }
        return Base64.encode(data);
    }

    @Override
    public byte[] serializeKey(SymmetricKey password, InstantMessage iMsg) {
        if (isBroadcast(iMsg)) {
            // broadcast message has no key
            return null;
        }
        return UTF8.encode(JSON.encode(password));
    }

    @Override
    public byte[] encryptKey(byte[] data, ID receiver, InstantMessage iMsg) {
        assert !isBroadcast(iMsg) : "broadcast message has no key: " + iMsg;
        Entity.Delegate barrack = getEntityDelegate();
        assert barrack != null : "entity delegate not set yet";
        // NOTICE: make sure the receiver's public key exists
        User user = barrack.getUser(receiver);
        assert user != null : "failed to encrypt for receiver: " + receiver;
        return user.encrypt(data);
    }

    @Override
    public Object encodeKey(byte[] key, InstantMessage iMsg) {
        assert !isBroadcast(iMsg) : "broadcast message has no key: " + iMsg;
        return Base64.encode(key);
    }

    //-------- SecureMessageDelegate

    @Override
    public byte[] decodeKey(Object key, SecureMessage sMsg) {
        assert !isBroadcast(sMsg) : "broadcast message has no key: " + sMsg;
        return Base64.decode((String) key);
    }

    @Override
    public byte[] decryptKey(byte[] key, ID sender, ID receiver, SecureMessage sMsg) {
        // NOTICE: the receiver will be group ID in a group message here
        assert !isBroadcast(sMsg) : "broadcast message has no key: " + sMsg;
        Entity.Delegate barrack = getEntityDelegate();
        assert barrack != null : "entity delegate not set yet";
        // decrypt key data with the receiver/group member's private key
        ID identifier = sMsg.getReceiver();
        User user = barrack.getUser(identifier);
        assert user != null : "failed to create local user: " + identifier;
        return user.decrypt(key);
    }

    @Override
    public SymmetricKey deserializeKey(byte[] key, ID sender, ID receiver, SecureMessage sMsg) {
        // NOTICE: the receiver will be group ID in a group message here
        assert !isBroadcast(sMsg) : "broadcast message has no key: " + sMsg;
        Object dict = JSON.decode(UTF8.decode(key));
        // TODO: translate short keys
        //       'A' -> 'algorithm'
        //       'D' -> 'data'
        //       'V' -> 'iv'
        //       'M' -> 'mode'
        //       'P' -> 'padding'
        return SymmetricKey.parse(dict);
    }

    @Override
    public byte[] decodeData(Object data, SecureMessage sMsg) {
        if (isBroadcast(sMsg)) {
            // broadcast message content will not be encrypted (just encoded to JsON),
            // so return the string data directly
            return UTF8.encode((String) data);
        }
        return Base64.decode((String) data);
    }

    @Override
    public byte[] decryptContent(byte[] data, SymmetricKey password, SecureMessage sMsg) {
        return password.decrypt(data);
    }

    @Override
    public Content deserializeContent(byte[] data, SymmetricKey password, SecureMessage sMsg) {
        assert sMsg.getData() != null : "message data empty";
        Object dict = JSON.decode(UTF8.decode(data));
        // TODO: translate short keys
        //       'T' -> 'type'
        //       'N' -> 'sn'
        //       'G' -> 'group'
        return Content.parse(dict);
    }

    @Override
    public byte[] signData(byte[] data, ID sender, SecureMessage sMsg) {
        Entity.Delegate barrack = getEntityDelegate();
        assert barrack != null : "entity delegate not set yet";
        User user = barrack.getUser(sender);
        assert user != null : "failed to sign with sender: " + sender;
        return user.sign(data);
    }

    @Override
    public Object encodeSignature(byte[] signature, SecureMessage sMsg) {
        return Base64.encode(signature);
    }

    //-------- ReliableMessageDelegate

    @Override
    public byte[] decodeSignature(Object signature, ReliableMessage rMsg) {
        return Base64.decode((String) signature);
    }

    @Override
    public boolean verifyDataSignature(byte[] data, byte[] signature, ID sender, ReliableMessage rMsg) {
        Entity.Delegate barrack = getEntityDelegate();
        assert barrack != null : "entity delegate not set yet";
        User user = barrack.getUser(sender);
        assert user != null : "failed to verify signature for sender: " + sender;
        return user.verify(data, signature);
    }

    /**
     *  Register core content factories
     */
    public static void registerContentFactories() {

        // Text
        Content.setFactory(ContentType.TEXT, BaseTextContent::new);

        // File
        Content.setFactory(ContentType.FILE, BaseFileContent::new);
        // Image
        Content.setFactory(ContentType.IMAGE, ImageFileContent::new);
        // Audio
        Content.setFactory(ContentType.AUDIO, AudioFileContent::new);
        // Video
        Content.setFactory(ContentType.VIDEO, VideoFileContent::new);

        // Web Page
        Content.setFactory(ContentType.PAGE, WebPageContent::new);

        // Money
        Content.setFactory(ContentType.MONEY, BaseMoneyContent::new);
        Content.setFactory(ContentType.TRANSFER, TransferMoneyContent::new);
        // ...

        // Command
        Content.setFactory(ContentType.COMMAND, new GeneralCommandFactory());

        // History Command
        Content.setFactory(ContentType.HISTORY, new HistoryCommandFactory());

        // Content Array
        Content.setFactory(ContentType.ARRAY, ListContent::new);

        // Top-Secret
        Content.setFactory(ContentType.FORWARD, SecretContent::new);

        // unknown content type
        Content.setFactory(0, BaseContent::new);
    }

    /**
     *  Register core command factories
     */
    public static void registerCommandFactories() {

        // Meta Command
        Command.setFactory(Command.META, BaseMetaCommand::new);

        // Document Command
        Command.setFactory(Command.DOCUMENT, BaseDocumentCommand::new);

        // Group Commands
        Command.setFactory("group", new GroupCommandFactory());
        Command.setFactory(GroupCommand.INVITE, InviteGroupCommand::new);
        Command.setFactory(GroupCommand.EXPEL, ExpelGroupCommand::new);
        Command.setFactory(GroupCommand.JOIN, JoinGroupCommand::new);
        Command.setFactory(GroupCommand.QUIT, QuitGroupCommand::new);
        Command.setFactory(GroupCommand.QUERY, QueryGroupCommand::new);
        Command.setFactory(GroupCommand.RESET, ResetGroupCommand::new);
    }
}
