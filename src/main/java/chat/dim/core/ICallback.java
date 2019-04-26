package chat.dim.core;

import chat.dim.dkd.InstantMessage;
import chat.dim.dkd.ReliableMessage;

public interface ICallback {

    public void onFinished(Error error);
}
