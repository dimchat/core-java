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

import chat.dim.protocol.Content;
import chat.dim.protocol.Envelope;
import chat.dim.protocol.ID;
import chat.dim.protocol.InstantMessage;

/**
 *  Instant Message
 *
 *  <blockquote><pre>
 *  data format: {
 *      //-- envelope
 *      'sender'   : "moki@xxx",
 *      'receiver' : "hulk@yyy",
 *      'time'     : 123,
 *
 *      //-- content
 *      'content'  : {...}
 *  }
 *  </pre></blockquote>
 */
public class PlainMessage extends BaseMessage implements InstantMessage {

    /// message body
    private Content content;

    public PlainMessage(Map<String, Object> msg) {
        super(msg);
        // lazy load
        content = null;
    }

    public PlainMessage(Envelope head, Content body) {
        super(head);
        setContent(body);
    }

    @Override
    public Date getTime() {
        Date time = getContent().getTime();
        if (time != null) {
            return time;
        }
        return super.getTime();
    }

    @Override
    public ID getGroup() {
        return getContent().getGroup();
    }

    @Override
    public String getType() {
        return getContent().getType();
    }

    @Override
    public Content getContent() {
        if (content == null) {
            Object info = get("content");
            assert info != null : "message content not found: " + toMap();
            content = Content.parse(info);
            assert content != null : "message content error: " + info;
        }
        return content;
    }

    //@Override
    public void setContent(Content body) {
        setMap("content", body);
        content = body;
    }

}
