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
package chat.dim.dkd;

import java.util.Date;
import java.util.Map;

import chat.dim.plugins.SharedMessageExtensions;
import chat.dim.protocol.Content;
import chat.dim.protocol.ID;
import chat.dim.protocol.InstantMessage;
import chat.dim.type.Dictionary;

/**
 *  Message Content
 *  <p>
 *      This class is for creating message content
 *  </p>
 *
 *  <blockquote><pre>
 *  data format: {
 *      'type'    : i2s(0x00),   // message type
 *      'sn'      : 0,           // serial number
 *
 *      'time'    : 123,         // message time
 *      'group'   : 'Group ID',  // for group message
 *
 *      //-- message info
 *      'text'    : 'text',         // for text message
 *      'command' : 'Command Name'  // for system command
 *      //...
 *  }
 *  </pre></blockquote>
 */
public class BaseContent extends Dictionary implements Content {

    // message type: text, image, ...
    private String type;

    // serial number: random number to identify message content
    private long sn;

    // message time
    private Date time;

    public BaseContent(Map<String, Object> content) {
        super(content);
        // lazy load
        type = null;
        sn   = -1;
        time = null;
    }

    public BaseContent(String msgType) {
        super();
        Date now = new Date();
        type = msgType;
        sn   = InstantMessage.generateSerialNumber(msgType, now);
        time = now;
        put("type", type);
        put("sn", sn);
        setDateTime("time", now);
    }

    @Override
    public String getType() {
        if (type == null) {
            type = SharedMessageExtensions.helper.getContentType(toMap(), "");
            // type = getInt("type", 0);
            assert type != null: "content type error: " + toMap();
        }
        return type;
    }

    @Override
    public long getSerialNumber() {
        if (sn == -1) {
            sn = getLong("sn", 0);
            assert sn > 0 : "serial number error: " + toMap();
        }
        return sn;
    }

    @Override
    public Date getTime() {
        if (time == null) {
            time = getDateTime("time", null);
        }
        return time;
    }

    // Group ID/string for group message
    //    if field 'group' exists, it means this is a group message
    @Override
    public ID getGroup() {
        return ID.parse(get("group"));
    }

    @Override
    public void setGroup(ID group) {
        setString("group", group);
    }
}
