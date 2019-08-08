
import chat.dim.core.CompletionHandler;
import chat.dim.core.TransceiverDelegate;
import chat.dim.dkd.InstantMessage;
import chat.dim.mkm.entity.ID;

public class Station extends chat.dim.network.Station implements TransceiverDelegate {

    public Station() {
        this(ID.getInstance("gsp-s001@x5Zh9ixt8ECr59XLye1y5WWfaX4fcoaaSC"), "127.0.0.1", 9394);
    }

    public Station(ID identifier, String host, int port) {
        super(identifier, host, port);
    }

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
