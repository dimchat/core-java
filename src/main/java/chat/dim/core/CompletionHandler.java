package chat.dim.core;

public interface CompletionHandler {

    public void onSuccess();

    public void onFailed(Error error);
}
