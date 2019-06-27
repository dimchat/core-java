import chat.dim.core.CompletionHandler;
import chat.dim.core.TransceiverDelegate;
import chat.dim.dkd.InstantMessage;

public class Station implements TransceiverDelegate {

    @Override
    public boolean sendPackage(byte[] data, CompletionHandler handler) {
        return false;
    }

    @Override
    public String uploadFileData(byte[] data, InstantMessage iMsg) {
        return null;
    }

    @Override
    public byte[] downloadFileData(String url, InstantMessage iMsg) {
        return new byte[0];
    }
}
