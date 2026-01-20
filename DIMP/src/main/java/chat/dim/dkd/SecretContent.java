/* license: https://mit-license.org
 *
 *  DIMP : Decentralized Instant Messaging Protocol
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import chat.dim.protocol.ContentType;
import chat.dim.protocol.ForwardContent;
import chat.dim.protocol.ReliableMessage;

/**
 *  Top-Secret Content
 *
 *  <blockquote><pre>
 *  data format: {
 *      "type" : i2s(0xFF),
 *      "sn"   : 456,
 *
 *      "forward" : {...}  // reliable (secure + certified) message
 *      "secrets" : [...]  // reliable (secure + certified) messages
 *  }
 *  </pre></blockquote>
 */
public class SecretContent extends BaseContent implements ForwardContent {

    private ReliableMessage forward;
    private List<ReliableMessage> secrets;

    public SecretContent(Map<String, Object> content) {
        super(content);
        // lazy load
        forward = null;
        secrets = null;
    }

    public SecretContent(ReliableMessage msg) {
        super(ContentType.FORWARD);
        forward = msg;
        secrets = null;
        //put("forward", msg.toMap());
    }

    public SecretContent(List<ReliableMessage> messages) {
        super(ContentType.FORWARD);
        forward = null;
        secrets = messages;
        //put("secrets", ReliableMessage.revert(messages));
    }

    @Override
    public Map<String, Object> toMap() {
        ReliableMessage msg = forward;
        List<ReliableMessage> messages = secrets;
        if (messages != null) {
            // serialize 'secrets'
            if (get("secrets") == null) {
                put("secrets", ReliableMessage.revert(messages));
            }
        } else if (msg != null) {
            // serialize 'forward'
            if (get("forward") == null) {
                put("forward", msg.toMap());
            }
        }
        // OK
        return super.toMap();
    }

    @Override
    public ReliableMessage getForward() {
        if (forward == null) {
            Object info = get("forward");
            forward = ReliableMessage.parse(info);
        }
        return forward;
    }

    @Override
    public List<ReliableMessage> getSecrets() {
        List<ReliableMessage> messages = secrets;
        if (messages == null) {
            Object info = get("secrets");
            if (info instanceof List) {
                // get from 'secrets'
                secrets = messages = ReliableMessage.convert((List<?>) info);
            } else {
                assert info == null : "secret messages error: " + info;
                // get from 'forward'
                messages = new ArrayList<>();
                ReliableMessage msg = getForward();
                if (msg != null) {
                    messages.add(msg);
                }
                secrets = messages;
            }
        }
        return messages;
    }

}
