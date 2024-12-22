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
package chat.dim.protocol;

import java.util.HashMap;
import java.util.Map;

import chat.dim.dkd.BaseQuoteContent;

/**
 *  Quote message: {
 *      type : 0x37,
 *      sn   : 456,
 *
 *      text    : "...",  // text message
 *      origin  : {       // original message envelope
 *          sender    : "...",
 *          receiver  : "...",
 *
 *          type      : 0x01,
 *          sn        : 123,
 *      }
 *  }
 */
public interface QuoteContent extends Content {

    String getText();

    Envelope getOriginalEnvelope();
    long getOriginalSerialNumber();

    //
    //  Factories
    //

    /**
     *  Create base receipt command with text & original message info
     */
    static QuoteContent create(String text, Envelope head, Content body) {
        Map<String, Object> info = purify(head);
        info.put("type", body.getType());
        info.put("sn", body.getSerialNumber());
        return new BaseQuoteContent(text, info);
    }

    static Map<String, Object> purify(Envelope envelope) {
        ID from = envelope.getSender();
        ID to = envelope.getGroup();
        if (to == null) {
            to = envelope.getReceiver();
        }
        Map<String, Object> info = new HashMap<>();
        info.put("sender", from.toString());
        info.put("receiver", to.toString());
        return info;
    }

}
