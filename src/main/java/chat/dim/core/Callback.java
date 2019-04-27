package chat.dim.core;

import chat.dim.dkd.InstantMessage;
import chat.dim.dkd.ReliableMessage;

public interface Callback {

    public void onFinished(Object result, Error error);
}
