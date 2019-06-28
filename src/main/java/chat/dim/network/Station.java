package chat.dim.network;

import chat.dim.mkm.Account;
import chat.dim.mkm.entity.ID;

public class Station extends Account {

    private ServiceProvider serviceProvider;

    public final String host;
    public final int port;

    public StationDelegate delegate;

    public Station(ID identifier) {
        this(identifier, "0.0.0.0", 9394);
    }

    public Station(ID identifier, String host, int port) {
        super(identifier);
        this.host = host;
        this.port = port;
    }

    public ServiceProvider getServiceProvider() {
        return serviceProvider;
    }
}

