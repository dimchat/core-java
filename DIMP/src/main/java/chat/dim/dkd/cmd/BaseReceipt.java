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

import java.util.HashMap;
import java.util.Map;

import chat.dim.protocol.Envelope;
import chat.dim.protocol.ReceiptCommand;
import chat.dim.type.Converter;

/**
 *  Command message: {
 *      type : 0x88,
 *      sn   : 456,
 *
 *      command : "receipt",
 *      text    : "...",  // text message
 *      origin  : {       // original message envelope
 *          sender    : "...",
 *          receiver  : "...",
 *          time      : 0,
 *
 *          sn        : 123,
 *          signature : "..."
 *      }
 *  }
 */
public abstract class BaseReceipt extends BaseCommand implements ReceiptCommand {

    /**
     *  original message envelope
     */
    private Envelope envelope;

    public BaseReceipt(Map<String, Object> content) {
        super(content);
        envelope = null;
    }

    public BaseReceipt(String text, Envelope env, long sn, String sig) {
        super(RECEIPT);
        // text message
        put("text", text);
        // original envelope
        envelope = env;
        // envelope of message responding to
        Map<String, Object> origin = env == null ? new HashMap<>() : env.copyMap(false);
        // sn of the message responding to
        if (sn > 0) {
            origin.put("sn", sn);
        }
        // signature of the message responding to
        if (sig != null && sig.length() > 0) {
            origin.put("signature", sig);
        }
        if (!origin.isEmpty()) {
            put("origin", origin);
        }
    }

    @Override
    public String getText() {
        return getString("text", "");
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map<String, Object> getOrigin() {
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
        return origin == null ? 0 : Converter.getLong(origin.get("sn"), 0);
    }

    @Override
    public String getOriginalSignature() {
        Map<?, ?> origin = getOrigin();
        return origin == null ? null : Converter.getString(origin.get("signature"), null);
    }
}
