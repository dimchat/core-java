package chat.dim.core;

import chat.dim.crypto.SymmetricKey;
import chat.dim.dkd.InstantMessage;
import chat.dim.mkm.entity.ID;

public interface TransceiverDelegate {

    /**
     *  Send out a data package onto network
     *
     * @param data - package`
     * @param handler - completion handler
     * @return NO on data/delegate error
     */
    boolean sendPackage(byte[] data, CompletionHandler handler);

    /**
     *  Create cipher key for encrypt message content
     *
     * @param sender - user identifier
     * @param receiver - contact/group identifier
     * @return SymmetricKey
     */
    SymmetricKey createCipherKey(ID sender, ID receiver);

    /**
     *  Upload encrypted data to CDN
     *
     * @param data - encrypted file data
     * @param iMsg - instant message
     * @return download URL
     */
    String uploadFileData(byte[] data, InstantMessage iMsg);

    /**
     *  Download encrypted data from CDN, and decrypt it when finished
     *
     * @param url - download URL
     * @param iMsg - instant message
     * @return encrypted file data
     */
    byte[] downloadFileData(String url, InstantMessage iMsg);
}
