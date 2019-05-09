package chat.dim.core;

import chat.dim.dkd.InstantMessage;

public interface TransceiverDelegate {

    /**
     *  Send out a data package onto network
     *
     *  @param data - package`
     *  @param handler - completion handler
     *  @return NO on data/delegate error
     */
    boolean sendPackage(byte[] data, CompletionHandler handler);

    /**
     *  Upload encrypted data to CDN
     *
     *  @param data - encrypted file data
     *  @param iMsg - instant message
     *  @return download URL
     */
    String uploadFileData(byte[] data, InstantMessage iMsg);

    /**
     *  Download encrypted data from CDN, and decrypt it when finished
     *
     *  @param url - download URL
     *  @param iMsg - instant message
     *  @return encrypted file data
     */
    byte[] downloadFileData(String url, InstantMessage iMsg);
}
