package chat.dim.protocols;

import chat.dim.dkd.Envelope;
import chat.dim.dkd.Utils;
import chat.dim.dkd.content.CommandContent;

import java.util.Map;

/**
 *  Command message: {
 *      type : 0x88,
 *      sn   : 123,  // the same serial number with the original message
 *
 *      command : "receipt",
 *      message : "...",
 *      // -- extra info
 *      sender    : "...",
 *      receiver  : "...",
 *      time      : 0,
 *      signature : "..." // the same signature with the original message
 *  }
 */
public class ReceiptCommand extends CommandContent {

    public final String message;

    // original message info
    public Envelope envelope;
    public byte[] signature;

    public ReceiptCommand(ReceiptCommand content) {
        super(content);
        this.message   = content.message;
        this.envelope  = content.envelope;
        this.signature = content.signature;
    }

    public ReceiptCommand(Map<String, Object> dictionary) {
        super(dictionary);
        this.message = (String) dictionary.get("message");
        this.envelope = Envelope.getInstance(dictionary.get("envelope"));
        // signature
        Object signature = dictionary.get("signature");
        if (signature == null) {
            this.signature = null;
        } else {
            this.signature = Utils.base64Decode((String) signature);
        }
    }

    public ReceiptCommand(String message) {
        super(RECEIPT);
        this.message   = message;
        this.envelope  = null;
        this.signature = null;
    }

    public void setEnvelope(Envelope envelope) {
        this.envelope = envelope;
        if (envelope == null) {
            this.dictionary.remove("envelope");
        } else {
            this.dictionary.put("envelope", envelope);
        }
    }

    public void setSignature(byte[] signature) {
        this.signature = signature;
        if (signature == null) {
            this.dictionary.remove("signature");
        } else {
            this.dictionary.put("signature", Utils.base64Encode(signature));
        }
    }
}
