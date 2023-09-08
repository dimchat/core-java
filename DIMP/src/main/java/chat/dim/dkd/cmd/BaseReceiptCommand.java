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

import chat.dim.protocol.ContentType;
import chat.dim.protocol.Envelope;
import chat.dim.protocol.ID;
import chat.dim.protocol.InstantMessage;

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
public class BaseReceiptCommand extends BaseReceipt {

    public BaseReceiptCommand(Map<String, Object> content) {
        super(content);
    }

    public BaseReceiptCommand(int type, String text, Envelope env, long sn, String sig) {
        super(type, text, env, sn, sig);
    }
    public BaseReceiptCommand(ContentType type, String text, Envelope env, long sn, String sig) {
        super(type, text, env, sn, sig);
    }
    public BaseReceiptCommand(String text, Envelope env, long sn, String sig) {
        super(ContentType.COMMAND, text, env, sn, sig);
    }
    public BaseReceiptCommand(String text, Envelope env) {
        super(ContentType.COMMAND, text, env, 0, null);
    }

    @Override
    public boolean match(InstantMessage iMsg) {
        Map<?, ?> origin = getOrigin();
        if (origin == null) {
            // receipt without original message info
            return false;
        }
        // check signature
        String sig1 = getOriginalSignature();
        if (sig1 != null) {
            // if contains signature, check it
            String sig2 = iMsg.getString("signature");
            if (sig2 != null) {
                return checkSignatures(sig1, sig2);
            }
        }
        // check envelope
        Envelope env1 = getOriginalEnvelope();
        if (env1 != null) {
            // if contains envelope, check it
            Envelope env2 = iMsg.getEnvelope();
            if (!checkEnvelopes(env1, env2)) {
                return false;
            }
        }
        // check serial number
        // (only the original message's receiver can know this number)
        return getOriginalSerialNumber() == iMsg.getContent().getSerialNumber();
    }

    private static boolean checkSignatures(String sig1, String sig2) {
        assert sig1 != null && sig2 != null : "signatures should not be empty: " + sig1 + ", " + sig2;
        if (sig1.length() > 8) {
            sig1 = sig1.substring(sig1.length() - 8);
        }
        if (sig2.length() > 8) {
            sig2 = sig2.substring(sig1.length() - 8);
        }
        return sig1.equals(sig2);
    }
    private static boolean checkEnvelopes(Envelope env1, Envelope env2) {
        if (!env1.getSender().equals(env2.getSender())) {
            return false;
        }
        ID to1 = env1.getGroup();
        if (to1 == null) {
            to1 = env1.getReceiver();
        }
        ID to2 = env2.getGroup();
        if (to2 == null) {
            to2 = env2.getReceiver();
        }
        return to1.equals(to2);
    }

}
