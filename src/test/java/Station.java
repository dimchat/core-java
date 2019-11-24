
import chat.dim.ID;
import chat.dim.User;

public class Station extends User {

    public final String host;
    public final int port;

    public Station() {
        this(ID.getInstance("gsp-s001@x5Zh9ixt8ECr59XLye1y5WWfaX4fcoaaSC"), "127.0.0.1", 9394);
    }

    public Station(ID identifier, String host, int port) {
        super(identifier);
        this.host = host;
        this.port = port;
    }
}
