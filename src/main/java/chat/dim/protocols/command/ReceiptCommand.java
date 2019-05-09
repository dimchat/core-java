package chat.dim.protocols.command;

import chat.dim.dkd.Envelope;
import chat.dim.dkd.Utils;
import chat.dim.protocols.CommandContent;

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
    private Envelope envelope;
    private byte[] signature;

    public ReceiptCommand(Map<String, Object> dictionary) {
        super(dictionary);
        message = (String) dictionary.get("message");
        envelope = Envelope.getInstance(dictionary.get("envelope"));
        // signature
        String base64 = (String) dictionary.get("signature");
        if (base64 == null) {
            signature = null;
        } else {
            signature = Utils.base64Decode(base64);
        }
    }

    public ReceiptCommand(String text) {
        super(RECEIPT);
        message   = text;
        envelope  = null;
        signature = null;
        dictionary.put("message", text);
    }

    //-------- setters/getters --------

    public void setEnvelope(Envelope env) {
        envelope = env;
        if (env == null) {
            dictionary.remove("envelope");
        } else {
            dictionary.put("envelope", env);
        }
    }

    public Envelope getEnvelope() {
        return envelope;
    }

    public void setSignature(byte[] sig) {
        signature = sig;
        if (sig == null) {
            dictionary.remove("signature");
        } else {
            dictionary.put("signature", Utils.base64Encode(sig));
        }
    }

    public byte[] getSignature() {
        return signature;
    }
}
