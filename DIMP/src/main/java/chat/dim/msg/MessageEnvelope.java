/* license: https://mit-license.org
 *
 *  Dao-Ke-Dao: Universal Message Module
 *
 *                                Written in 2019 by Moky <albert.moky@gmail.com>
 *
 * ==============================================================================
 * The MIT License (MIT)
 *
 * Copyright (c) 2019 Albert Moky
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
package chat.dim.msg;

import java.util.Date;
import java.util.Map;

import chat.dim.protocol.Envelope;
import chat.dim.protocol.ID;
import chat.dim.type.Dictionary;

/**
 *  Envelope for message
 *  ~~~~~~~~~~~~~~~~~~~~
 *  This class is used to create a message envelope
 *  which contains 'sender', 'receiver' and 'time'
 *
 *  data format: {
 *      sender   : "moki@xxx",
 *      receiver : "hulk@yyy",
 *      time     : 123
 *  }
 */
public class MessageEnvelope extends Dictionary implements Envelope {

    private ID sender;
    private ID receiver;
    private Date time;

    public MessageEnvelope(Map<String, Object> env) {
        super(env);
        // lazy load
        sender   = null;
        receiver = null;
        time     = null;
    }

    public MessageEnvelope(ID from, ID to, Date when) {
        super();
        assert from != null : "message sender should not be empty";
        if (to == null) {
            to = ID.ANYONE;
        }
        if (when == null) {
            when = new Date();
        }
        sender   = from;
        receiver = to;
        time     = when;
        setString("sender", from);
        setString("receiver", to);
        setDateTime("time", when);
    }

    @Override
    public ID getSender() {
        if (sender == null) {
            sender = ID.parse(get("sender"));
            assert sender != null : "message sender not found: " + toMap();
        }
        return sender;
    }

    @Override
    public ID getReceiver() {
        if (receiver == null) {
            receiver = ID.parse(get("receiver"));
            if (receiver == null) {
                receiver = ID.ANYONE;
            }
        }
        return receiver;
    }

    @Override
    public Date getTime() {
        if (time == null) {
            time = getDateTime("time", null);
        }
        return time;
    }

    /*
     *  Group ID
     *  ~~~~~~~~
     *  when a group message was split/trimmed to a single message
     *  the 'receiver' will be changed to a member ID, and
     *  the group ID will be saved as 'group'.
     */
    @Override
    public ID getGroup() {
        return ID.parse(get("group"));
    }

    @Override
    public void setGroup(ID group) {
        setString("group", group);
    }

    /*
     *  Message Type
     *  ~~~~~~~~~~~~
     *  because the message content will be encrypted, so
     *  the intermediate nodes(station) cannot recognize what kind of it.
     *  we pick out the content type and set it in envelope
     *  to let the station do its job.
     */
    @Override
    public int getType() {
        return getInt("type", 0);
    }

    @Override
    public void setType(int type) {
        put("type", type);
    }
}
