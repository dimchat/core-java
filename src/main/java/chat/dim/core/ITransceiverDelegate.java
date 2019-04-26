package chat.dim.core;

import chat.dim.dkd.InstantMessage;

public interface ITransceiverDelegate {

    /**
     *  Send out a data package onto network
     *
     *  @param data - package`
     *  @param handler - completion handler
     *  @return NO on data/delegate error
     */
    public boolean sendPackage(byte[] data, ICompletionHandler handler);

    /**
     *  Upload encrypted data to CDN
     *
     *  @param CT - encrypted file data
     *  @param iMsg - instant message
     *  @return download URL
     */
    public String uploadFileData(byte[] data, InstantMessage iMsg);

    /**
     *  Download encrypted data from CDN, and decrypt it when finished
     *
     *  @param url - download URL
     *  @param iMsg - instant message
     *  @return encrypted file data
     */
    public byte[] downloadFileData(String url, InstantMessage iMsg);
}
