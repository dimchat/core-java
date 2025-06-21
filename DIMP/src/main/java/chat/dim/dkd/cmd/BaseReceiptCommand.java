/* license: https://mit-license.org
 *
 *  DIMP : Decentralized Instant Messaging Protocol
 *
 *                                Written in 2023 by Moky <albert.moky@gmail.com>
 *
 * ==============================================================================
 * The MIT License (MIT)
 *
 * Copyright (c) 2023 Albert Moky
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
package chat.dim.dkd.cmd;

import java.util.Map;

import chat.dim.protocol.Envelope;
import chat.dim.protocol.ReceiptCommand;
import chat.dim.type.Converter;

/**
 *  Receipt Command Content
 *
 *  <blockquote><pre>
 *  data format: {
 *      'type' : i2s(0x88),
 *      'sn'   : 456,
 *
 *      'command' : "receipt",
 *      'text'    : "...",  // text message
 *      'origin'  : {       // original message envelope
 *          'sender'    : "...",
 *          'receiver'  : "...",
 *          'time'      : 0,
 *
 *          'sn'        : 123,
 *          'signature' : "..."
 *      }
 *  }
 *  </pre></blockquote>
 */
public class BaseReceiptCommand extends BaseCommand implements ReceiptCommand {

    /**
     *  original message envelope
     */
    private Envelope envelope = null;

    public BaseReceiptCommand(Map<String, Object> content) {
        super(content);
    }

    public BaseReceiptCommand(String text, Map<String, Object> origin) {
        super(RECEIPT);
        // text message
        put("text", text);
        // original envelope of message responding to,
        // includes 'sn' and 'signature'
        if (origin != null) {
            assert !(origin.isEmpty() ||
                    origin.containsKey("data") ||
                    origin.containsKey("key") ||
                    origin.containsKey("keys") ||
                    origin.containsKey("meta") ||
                    origin.containsKey("visa")) : "impure envelope: " + origin;
            put("origin", origin);
        }
    }

    @Override
    public String getText() {
        return getString("text", "");
    }

    @SuppressWarnings("unchecked")
    protected Map<String, Object> getOrigin() {
        Object origin = get("origin");
        if (origin instanceof Map) {
            return (Map<String, Object>) origin;
        }
        assert origin == null : "origin error: " + origin;
        return null;
    }

    @Override
    public Envelope getOriginalEnvelope() {
        if (envelope == null) {
            // origin: { sender: "...", receiver: "...", time: 0 }
            envelope = Envelope.parse(getOrigin());
        }
        return envelope;
    }

    @Override
    public long getOriginalSerialNumber() {
        Map<?, ?> origin = getOrigin();
        if (origin == null) {
            // original info not found
            return 0;
        }
        return Converter.getLong(origin.get("sn"), 0);
    }

    @Override
    public String getOriginalSignature() {
        Map<?, ?> origin = getOrigin();
        if (origin == null) {
            // original info not found
            return null;
        }
        return Converter.getString(origin.get("signature"), null);
    }
}
