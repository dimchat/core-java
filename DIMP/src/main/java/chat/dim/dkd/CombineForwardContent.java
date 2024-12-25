/* license: https://mit-license.org
 *
 *  DIMP : Decentralized Instant Messaging Protocol
 *
 *                                Written in 2024 by Moky <albert.moky@gmail.com>
 *
 * ==============================================================================
 * The MIT License (MIT)
 *
 * Copyright (c) 2024 Albert Moky
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

import chat.dim.protocol.CombineContent;
import chat.dim.protocol.ContentType;
import chat.dim.protocol.InstantMessage;

/**
 *  Combine Forward message: {
 *      type : 0xCF,
 *      sn   : 123,
 *
 *      title    : "...",  // chat title
 *      messages : [...]   // chat history
 *  }
 */
public class CombineForwardContent extends BaseContent implements CombineContent {

    private List<InstantMessage> history;

    public CombineForwardContent(Map<String, Object> content) {
        super(content);
        // lazy load
        history = null;
    }

    public CombineForwardContent(String title, List<InstantMessage> messages) {
        super(ContentType.COMBINE_FORWARD.value);
        // chat name
        put("title", title);
        // chat history
        put("messages", CombineContent.revert(messages));
        history = messages;
    }

    @Override
    public String getTitle() {
        return getString("title", "");
    }

    @Override
    public List<InstantMessage> getMessages() {
        List<InstantMessage> messages = history;
        if (messages == null) {
            Object info = get("messages");
            if (info instanceof List) {
                messages = CombineContent.convert((List<?>) info);
            } else {
                assert info == null : "combine message error: " + info;
                messages = new ArrayList<>();
            }
            history = messages;
        }
        return messages;
    }

}
