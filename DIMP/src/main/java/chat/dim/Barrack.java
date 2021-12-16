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
package chat.dim;

import java.util.ArrayList;
import java.util.List;

import chat.dim.crypto.EncryptKey;
import chat.dim.crypto.VerifyKey;
import chat.dim.protocol.Bulletin;
import chat.dim.protocol.Document;
import chat.dim.protocol.ID;
import chat.dim.protocol.Meta;
import chat.dim.protocol.NetworkType;
import chat.dim.protocol.Visa;

/**
 *  Entity Database
 *  ~~~~~~~~~~~~~~~
 *  Manage meta for all entities
 */
public abstract class Barrack implements Entity.Delegate, User.DataSource, Group.DataSource {

    /**
     *  Get all local users (for decrypting received message)
     *
     * @return users with private key
     */
    public abstract List<User> getLocalUsers();

    /**
     *  Get current user (for signing and sending message)
     *
     * @return User
     */
    public User getCurrentUser() {
        List<User> users = getLocalUsers();
        if (users == null || users.size() == 0) {
            return null;
        }
        return users.get(0);
    }

    //-------- Entity Delegate

    @Override
    public User selectLocalUser(ID receiver) {
        List<User> users = getLocalUsers();
        if (users == null || users.size() == 0) {
            throw new NullPointerException("local users should not be empty");
        } else if (receiver.isBroadcast()) {
            // broadcast message can decrypt by anyone, so just return current user
            return users.get(0);
        }
        if (receiver.isGroup()) {
            // group message (recipient not designated)
            List<ID> members = getMembers(receiver);
            if (members == null || members.size() == 0) {
                // TODO: group not ready, waiting for group info
                return null;
            }
            for (User item : users) {
                if (members.contains(item.identifier)) {
                    // DISCUSS: set this item to be current user?
                    return item;
                }
            }
        } else {
            // 1. personal message
            // 2. split group message
            for (User item : users) {
                if (receiver.equals(item.identifier)) {
                    // DISCUSS: set this item to be current user?
                    return item;
                }
            }
        }
        return null;
    }

    //-------- User DataSource

    private EncryptKey getVisaKey(ID user) {
        Document doc = getDocument(user, Document.VISA);
        if (doc instanceof Visa) {
            Visa visa = (Visa) doc;
            if (visa.isValid()) {
                return visa.getKey();
            }
        }
        return null;
    }
    private VerifyKey getMetaKey(ID user) {
        Meta meta = getMeta(user);
        if (meta == null) {
            //throw new NullPointerException("failed to get meta for ID: " + user);
            return null;
        }
        return meta.getKey();
    }

    @Override
    public EncryptKey getPublicKeyForEncryption(ID user) {
        // 1. get key from visa
        EncryptKey visaKey = getVisaKey(user);
        if (visaKey != null) {
            // if visa.key exists, use it for encryption
            return visaKey;
        }
        // 2. get key from meta
        VerifyKey metaKey = getMetaKey(user);
        if (metaKey instanceof EncryptKey) {
            // if visa.key not exists and meta.key is encrypt key,
            // use it for encryption
            return (EncryptKey) metaKey;
        }
        //throw new NullPointerException("failed to get encrypt key for user: " + user);
        return null;
    }

    @Override
    public List<VerifyKey> getPublicKeysForVerification(ID user) {
        List<VerifyKey> keys = new ArrayList<>();
        // 1. get key from visa
        EncryptKey visaKey = getVisaKey(user);
        if (visaKey instanceof VerifyKey) {
            // the sender may use communication key to sign message.data,
            // so try to verify it with visa.key here
            keys.add((VerifyKey) visaKey);
        }
        // 2. get key from meta
        VerifyKey metaKey = getMetaKey(user);
        if (metaKey != null) {
            // the sender may use identity key to sign message.data,
            // try to verify it with meta.key
            keys.add(metaKey);
        }
        assert keys.size() > 0 : "failed to get verify key for user: " + user;
        return keys;
    }

    //-------- Group DataSource

    private String getGroupSeed(ID group) {
        String name = group.getName();
        if (name != null) {
            int len = name.length();
            if (len == 0 || (len == 8 && name.equalsIgnoreCase("everyone"))) {
                name = null;
            }
        }
        return name;
    }

    protected ID getBroadcastFounder(ID group) {
        String name = getGroupSeed(group);
        if (name == null) {
            // Consensus: the founder of group 'everyone@everywhere'
            //            'Albert Moky'
            return ID.FOUNDER;
        } else {
            // DISCUSS: who should be the founder of group 'xxx@everywhere'?
            //          'anyone@anywhere', or 'xxx.founder@anywhere'
            return ID.parse(name + ".founder@anywhere");
        }
    }
    protected ID getBroadcastOwner(ID group) {
        String name = getGroupSeed(group);
        if (name == null) {
            // Consensus: the owner of group 'everyone@everywhere'
            //            'anyone@anywhere'
            return ID.ANYONE;
        } else {
            // DISCUSS: who should be the owner of group 'xxx@everywhere'?
            //          'anyone@anywhere', or 'xxx.owner@anywhere'
            return ID.parse(name + ".owner@anywhere");
        }
    }
    protected List<ID> getBroadcastMembers(ID group) {
        List<ID> members = new ArrayList<>();
        String name = getGroupSeed(group);
        if (name == null) {
            // Consensus: the member of group 'everyone@everywhere'
            //            'anyone@anywhere'
            members.add(ID.ANYONE);
        } else {
            // DISCUSS: who should be the member of group 'xxx@everywhere'?
            //          'anyone@anywhere', or 'xxx.member@anywhere'
            ID owner = ID.parse(name + ".owner@anywhere");
            ID member = ID.parse(name + ".member@anywhere");
            members.add(owner);
            members.add(member);
        }
        return members;
    }

    @Override
    public ID getFounder(ID group) {
        // check broadcast group
        if (group.isBroadcast()) {
            // founder of broadcast group
            return getBroadcastFounder(group);
        }
        // check group meta
        Meta gMeta = getMeta(group);
        if (gMeta == null) {
            // FIXME: when group profile was arrived but the meta still on the way,
            //        here will cause founder not found
            return null;
        }
        // check each member's public key with group meta
        List<ID> members = getMembers(group);
        if (members != null) {
            Meta mMeta;
            for (ID item : members) {
                mMeta = getMeta(item);
                if (mMeta == null) {
                    // failed to get member's meta
                    continue;
                }
                if (gMeta.matches(mMeta.getKey())) {
                    // if the member's public key matches with the group's meta,
                    // it means this meta was generated by the member's private key
                    return item;
                }
            }
        }
        // TODO: load founder from database
        return null;
    }

    @Override
    public ID getOwner(ID group) {
        // check broadcast group
        if (group.isBroadcast()) {
            // owner of broadcast group
            return getBroadcastOwner(group);
        }
        // check group type
        if (NetworkType.POLYLOGUE.equals(group.getType())) {
            // Polylogue's owner is its founder
            return getFounder(group);
        }
        // TODO: load owner from database
        return null;
    }

    @Override
    public List<ID> getMembers(ID group) {
        // check broadcast group
        if (group.isBroadcast()) {
            // members of broadcast group
            return getBroadcastMembers(group);
        }
        // TODO: load members from database
        return null;
    }

    @Override
    public List<ID> getAssistants(ID group) {
        Document doc = getDocument(group, Document.BULLETIN);
        if (doc instanceof Bulletin) {
            if (doc.isValid()) {
                return ((Bulletin) doc).getAssistants();
            }
        }
        // TODO: get group bots from SP configuration
        return null;
    }
}
