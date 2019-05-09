package chat.dim.core;

public interface CompletionHandler {

    void onSuccess();

    void onFailed(Error error);
}
